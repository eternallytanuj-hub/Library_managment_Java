package com.library.dao;

import com.library.dao.impl.NotificationDaoJdbcImpl;
import com.library.dao.impl.UserDaoJdbcImpl;
import com.library.model.Member;
import com.library.model.Notification;
import com.library.model.NotificationType;
import com.library.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("NotificationDaoJdbcImpl Integration Tests")
class NotificationDaoJdbcImplTest extends BaseDaoTest {

    private NotificationDao notificationDao;
    private UserDao userDao;
    private User sampleUser;

    @BeforeEach
    void setUp() throws Exception {
        notificationDao = new NotificationDaoJdbcImpl();
        userDao = new UserDaoJdbcImpl();

        Member m = new Member();
        m.setMembershipId("NOTIF-USER");
        m.setName("Alert Recipient");
        m.setEmail("alerts@test.com");
        m.setPasswordHash("pass");
        sampleUser = userDao.save(m);
    }

    @Test
    @DisplayName("Should save notification and find unread notifications by user ID")
    void testSaveAndFindUnread() throws Exception {
        Notification n = new Notification(sampleUser.getId(), "Due Soon", "Your book is due in 2 days", NotificationType.DUE_DATE);
        Notification saved = notificationDao.save(n);

        assertNotNull(saved.getId());
        assertTrue(saved.getId() > 0);

        List<Notification> unread = notificationDao.findUnreadByUserId(sampleUser.getId());
        assertEquals(1, unread.size());
        assertEquals("Due Soon", unread.get(0).getTitle());
        assertEquals(NotificationType.DUE_DATE, unread.get(0).getType());
        assertFalse(unread.get(0).isRead());
    }

    @Test
    @DisplayName("Should mark individual notification as read")
    void testMarkAsRead() throws Exception {
        Notification n = notificationDao.save(new Notification(sampleUser.getId(), "Title", "Message", NotificationType.SYSTEM));
        assertEquals(1, notificationDao.countUnreadByUserId(sampleUser.getId()));

        boolean marked = notificationDao.markAsRead(n.getId(), sampleUser.getId());
        assertTrue(marked);

        assertEquals(0, notificationDao.countUnreadByUserId(sampleUser.getId()));
        Optional<Notification> opt = notificationDao.findById(n.getId());
        assertTrue(opt.isPresent());
        assertTrue(opt.get().isRead());
    }

    @Test
    @DisplayName("Should mark all notifications as read for a user")
    void testMarkAllAsRead() throws Exception {
        notificationDao.save(new Notification(sampleUser.getId(), "Notice 1", "Body 1", NotificationType.NEW_ARRIVAL));
        notificationDao.save(new Notification(sampleUser.getId(), "Notice 2", "Body 2", NotificationType.GENERAL));
        assertEquals(2, notificationDao.countUnreadByUserId(sampleUser.getId()));

        boolean markedAll = notificationDao.markAllAsRead(sampleUser.getId());
        assertTrue(markedAll);
        assertEquals(0, notificationDao.countUnreadByUserId(sampleUser.getId()));
    }

    @Test
    @DisplayName("Should batch insert notifications efficiently")
    void testInsertBatch() throws Exception {
        List<Notification> batch = List.of(
                new Notification(sampleUser.getId(), "Batch 1", "Msg 1", NotificationType.SYSTEM),
                new Notification(sampleUser.getId(), "Batch 2", "Msg 2", NotificationType.NEW_ARRIVAL),
                new Notification(sampleUser.getId(), "Batch 3", "Msg 3", NotificationType.DUE_DATE)
        );

        int inserted = notificationDao.insertBatch(batch);
        assertEquals(3, inserted);
        assertEquals(3, notificationDao.countUnreadByUserId(sampleUser.getId()));
    }
}
