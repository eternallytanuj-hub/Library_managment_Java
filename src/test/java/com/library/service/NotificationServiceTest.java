package com.library.service;

import com.library.dao.BaseDaoTest;
import com.library.dao.NotificationDao;
import com.library.dao.UserDao;
import com.library.dao.impl.NotificationDaoJdbcImpl;
import com.library.dao.impl.UserDaoJdbcImpl;
import com.library.exception.ValidationException;
import com.library.model.Member;
import com.library.model.Notification;
import com.library.model.NotificationType;
import com.library.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("NotificationService Integration Tests")
class NotificationServiceTest extends BaseDaoTest {

    private NotificationDao notificationDao;
    private UserDao userDao;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationDao = new NotificationDaoJdbcImpl();
        userDao = new UserDaoJdbcImpl();
        notificationService = new NotificationServiceImpl(notificationDao, userDao);
    }

    @Test
    @DisplayName("Should author and send targeted notification successfully")
    void testSendNotification_Success() throws Exception {
        Member member = new Member("MEM-1001", "Alert User", "alert@test.com", "hash");
        userDao.save(member);

        Notification notif = notificationService.sendNotification(member.getId(), "Welcome", "Welcome aboard!", NotificationType.SYSTEM);

        assertNotNull(notif.getId());
        assertEquals("Welcome", notif.getTitle());
        assertEquals("Welcome aboard!", notif.getMessage());
        assertEquals(NotificationType.SYSTEM, notif.getType());
        assertFalse(notif.isRead());
    }

    @Test
    @DisplayName("Should reject empty notification title or message")
    void testSendNotification_ValidationErrors() {
        assertThrows(ValidationException.class, () ->
                notificationService.sendNotification(1, "", "Message", NotificationType.GENERAL));
        assertThrows(ValidationException.class, () ->
                notificationService.sendNotification(1, "Title", "", NotificationType.GENERAL));
        assertThrows(ValidationException.class, () ->
                notificationService.sendNotification(1, null, "Message", NotificationType.GENERAL));
        assertThrows(ValidationException.class, () ->
                notificationService.sendNotification(1, "Title", null, NotificationType.GENERAL));
    }

    @Test
    @DisplayName("Should broadcast notifications honoring member preferences")
    void testBroadcastNotification_HonoringPreferences() throws Exception {
        // Member 1: wants new arrival alerts
        Member m1 = new Member("MEM-1002", "Reader 1", "r1@test.com", "hash");
        m1.setNewArrivalAlerts(true);
        userDao.save(m1);

        // Member 2: opted OUT of new arrival alerts
        Member m2 = new Member("MEM-1003", "Reader 2", "r2@test.com", "hash");
        m2.setNewArrivalAlerts(false);
        userDao.save(m2);

        int count = notificationService.broadcastNotification("New Books In Catalog", "Check out new titles!", NotificationType.NEW_ARRIVAL);
        assertEquals(1, count, "Only Member 1 should receive new arrival broadcast");

        List<Notification> m1Alerts = notificationService.getNotificationsForUser(m1.getId());
        assertEquals(1, m1Alerts.size());

        List<Notification> m2Alerts = notificationService.getNotificationsForUser(m2.getId());
        assertEquals(0, m2Alerts.size());
    }

    @Test
    @DisplayName("Should manage read states and unread counters")
    void testReadStatesAndCounters() throws Exception {
        Member member = new Member("MEM-1004", "Reader 3", "r3@test.com", "hash");
        userDao.save(member);

        Notification n1 = notificationService.sendNotification(member.getId(), "Notice 1", "Msg 1", NotificationType.GENERAL);
        Notification n2 = notificationService.sendNotification(member.getId(), "Notice 2", "Msg 2", NotificationType.GENERAL);

        assertEquals(2, notificationService.getUnreadNotificationCount(member.getId()));
        assertEquals(2, notificationService.getUnreadNotificationsForUser(member.getId()).size());

        // Mark n1 as read
        boolean marked = notificationService.markAsRead(n1.getId(), member.getId());
        assertTrue(marked);

        assertEquals(1, notificationService.getUnreadNotificationCount(member.getId()));

        // Mark all as read
        notificationService.markAllAsRead(member.getId());
        assertEquals(0, notificationService.getUnreadNotificationCount(member.getId()));
    }
}
