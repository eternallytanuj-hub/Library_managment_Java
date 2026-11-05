package com.library.concurrency;

import com.library.dao.BaseDaoTest;
import com.library.dao.BookDao;
import com.library.dao.NotificationDao;
import com.library.dao.TransactionDao;
import com.library.dao.UserDao;
import com.library.dao.impl.BookDaoJdbcImpl;
import com.library.dao.impl.NotificationDaoJdbcImpl;
import com.library.dao.impl.TransactionDaoJdbcImpl;
import com.library.dao.impl.UserDaoJdbcImpl;
import com.library.daemon.AsyncNotificationDispatcher;
import com.library.daemon.DueDateMonitorDaemon;
import com.library.exception.DatabaseOperationException;
import com.library.listener.DueDateDaemonListener;
import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Notification;
import com.library.model.NotificationType;
import com.library.model.Transaction;
import com.library.model.TransactionStatus;
import com.library.service.TransactionService;
import com.library.service.impl.TransactionServiceImpl;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("DueDate Daemon & Async Dispatcher Concurrency Tests")
class DueDateDaemonTest extends BaseDaoTest {

    private UserDao userDao;
    private BookDao bookDao;
    private TransactionDao transactionDao;
    private NotificationDao notificationDao;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        userDao = new UserDaoJdbcImpl();
        bookDao = new BookDaoJdbcImpl();
        transactionDao = new TransactionDaoJdbcImpl();
        notificationDao = new NotificationDaoJdbcImpl();
        transactionService = new TransactionServiceImpl(transactionDao, bookDao, userDao, notificationDao);
    }

    @Test
    @DisplayName("Should start and stop DueDateMonitorDaemon cleanly")
    void testDaemonLifecycle() {
        DueDateMonitorDaemon daemon = new DueDateMonitorDaemon(transactionService, 1, 10);
        assertFalse(daemon.isRunning(), "Daemon should not be running initially");

        daemon.start();
        assertTrue(daemon.isRunning(), "Daemon should be running after start()");

        daemon.stop();
        assertFalse(daemon.isRunning(), "Daemon should be stopped after stop()");
    }

    @Test
    @DisplayName("Should detect overdue loans during scheduled sweep, update status, and generate notifications")
    void testRunOverdueSweep_TransitionsStatusAndAlerts() throws Exception {
        Member member = new Member("MEM-3001", "Late Reader", "late@test.com", "hash");
        userDao.save(member);
        Book book = bookDao.save(new Book("978-0301", "Overdue Tome", "Author", "Fiction", 2, 1));

        // Create loan that became overdue 3 days ago
        long now = System.currentTimeMillis();
        Timestamp borrowDate = new Timestamp(now - 17L * 24 * 60 * 60 * 1000L);
        Timestamp dueDate = new Timestamp(now - 3L * 24 * 60 * 60 * 1000L);
        Transaction tx = transactionDao.save(new Transaction(book.getId(), member.getId(), borrowDate, dueDate));

        DueDateMonitorDaemon daemon = new DueDateMonitorDaemon(transactionService, 1, 10);
        int processed = daemon.runOverdueSweep();

        assertEquals(1, processed, "Daemon sweep should have processed exactly 1 overdue transaction");

        Transaction updatedTx = transactionDao.findById(tx.getId()).get();
        assertEquals(TransactionStatus.OVERDUE, updatedTx.getStatus(), "Transaction status must be OVERDUE");

        List<Notification> alerts = notificationDao.findByUserId(member.getId());
        assertEquals(1, alerts.size(), "Member must have received overdue notification alert");
        assertEquals(NotificationType.DUE_DATE, alerts.get(0).getType());
    }

    @Test
    @DisplayName("Should handle database exceptions during sweep without terminating daemon")
    void testRunOverdueSweep_ExceptionHandling() throws Exception {
        TransactionService failingService = mock(TransactionService.class);
        when(failingService.processOverdueTransactions())
                .thenThrow(new DatabaseOperationException("Simulated connection timeout"));

        DueDateMonitorDaemon daemon = new DueDateMonitorDaemon(failingService, 1, 10);
        daemon.start();

        // Direct sweep execution catches exception safely
        int processed = daemon.runOverdueSweep();
        assertEquals(0, processed, "Processed count should be 0 on error");
        assertTrue(daemon.isRunning(), "Daemon must continue running after caught exception");

        daemon.stop();
        assertFalse(daemon.isRunning());
    }

    @Test
    @DisplayName("Should dispatch notifications asynchronously via AsyncNotificationDispatcher worker queue")
    void testAsyncNotificationDispatcher_EnqueueAndBackgroundDrain() throws Exception {
        Member member = new Member("MEM-3002", "Async Reader", "async@test.com", "hash");
        userDao.save(member);

        AsyncNotificationDispatcher dispatcher = new AsyncNotificationDispatcher(notificationDao, 100);
        assertFalse(dispatcher.isRunning());

        dispatcher.start();
        assertTrue(dispatcher.isRunning());

        Notification n1 = new Notification(member.getId(), "Notice 1", "Message 1", NotificationType.GENERAL);
        Notification n2 = new Notification(member.getId(), "Notice 2", "Message 2", NotificationType.NEW_ARRIVAL);
        Notification n3 = new Notification(member.getId(), "Notice 3", "Message 3", NotificationType.SYSTEM);

        assertTrue(dispatcher.enqueue(n1));
        assertTrue(dispatcher.enqueue(n2));
        assertTrue(dispatcher.enqueue(n3));
        assertFalse(dispatcher.enqueue(null), "Null notification should be rejected");

        // Allow background thread to drain queue
        int attempts = 0;
        while (dispatcher.getQueuedCount() > 0 && attempts++ < 20) {
            TimeUnit.MILLISECONDS.sleep(100);
        }

        // Wait brief moment for worker to commit
        TimeUnit.MILLISECONDS.sleep(200);

        List<Notification> persisted = notificationDao.findByUserId(member.getId());
        assertEquals(3, persisted.size(), "All 3 notifications should be persisted by background worker");

        dispatcher.stop();
        assertFalse(dispatcher.isRunning());
    }

    @Test
    @DisplayName("Should initialize and destroy daemons via DueDateDaemonListener")
    void testDueDateDaemonListener_Lifecycle() {
        DueDateDaemonListener listener = new DueDateDaemonListener();

        ServletContext servletContext = mock(ServletContext.class);
        ServletContextEvent event = new ServletContextEvent(servletContext);

        Map<String, Object> attributes = new HashMap<>();
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            attributes.put(key, value);
            return null;
        }).when(servletContext).setAttribute(anyString(), any());

        when(servletContext.getAttribute(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return attributes.get(key);
        });

        // Initialize context
        listener.contextInitialized(event);

        assertTrue(attributes.containsKey(DueDateDaemonListener.DUE_DATE_DAEMON_ATTR));
        assertTrue(attributes.containsKey(DueDateDaemonListener.NOTIFICATION_DISPATCHER_ATTR));

        DueDateMonitorDaemon daemon = (DueDateMonitorDaemon) attributes.get(DueDateDaemonListener.DUE_DATE_DAEMON_ATTR);
        assertNotNull(daemon);
        assertTrue(daemon.isRunning());

        AsyncNotificationDispatcher dispatcher = (AsyncNotificationDispatcher) attributes.get(DueDateDaemonListener.NOTIFICATION_DISPATCHER_ATTR);
        assertNotNull(dispatcher);
        assertTrue(dispatcher.isRunning());

        // Destroy context
        listener.contextDestroyed(event);

        assertFalse(daemon.isRunning(), "Daemon should be stopped on context destruction");
        assertFalse(dispatcher.isRunning(), "Dispatcher should be stopped on context destruction");
    }
}
