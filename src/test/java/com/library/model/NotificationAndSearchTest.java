package com.library.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Notification and SearchHistory Domain Model Unit Tests")
class NotificationAndSearchTest {

    @Test
    @DisplayName("Verify Notification creation and default read status")
    void testNotificationCreationAndDefaults() {
        Notification notif = new Notification(10, "Due Date Reminder", "Please return your book", NotificationType.DUE_DATE);

        assertEquals(10, notif.getUserId());
        assertEquals("Due Date Reminder", notif.getTitle());
        assertEquals("Please return your book", notif.getMessage());
        assertEquals(NotificationType.DUE_DATE, notif.getType());
        assertFalse(notif.isRead(), "New notifications must be unread by default");
        assertNotNull(notif.getCreatedAt());
    }

    @Test
    @DisplayName("Verify Notification markAsRead and markAsUnread toggling")
    void testNotificationMarkAsReadAndUnread() {
        Notification notif = new Notification(10, "Title", "Message", NotificationType.GENERAL);
        assertFalse(notif.isRead());

        notif.markAsRead();
        assertTrue(notif.isRead());

        notif.markAsUnread();
        assertFalse(notif.isRead());

        notif.setRead(true);
        assertTrue(notif.isRead());
    }

    @Test
    @DisplayName("Verify NotificationType enum fromString and display names")
    void testNotificationTypeEnum() {
        assertEquals("General Announcement", NotificationType.GENERAL.getDisplayName());
        assertEquals("Due Date Reminder", NotificationType.DUE_DATE.getDisplayName());
        assertEquals("New Book Arrival", NotificationType.NEW_ARRIVAL.getDisplayName());

        assertEquals(NotificationType.DUE_DATE, NotificationType.fromString("DUE_DATE"));
        assertEquals(NotificationType.DUE_DATE, NotificationType.fromString("due_date"));
        assertEquals(NotificationType.NEW_ARRIVAL, NotificationType.fromString("NEW_ARRIVAL"));
        assertEquals(NotificationType.GENERAL, NotificationType.fromString("UNKNOWN"));
        assertEquals(NotificationType.GENERAL, NotificationType.fromString(null));
        assertEquals(NotificationType.GENERAL, NotificationType.fromString("   "));
    }

    @Test
    @DisplayName("Verify SearchHistory creation, trimming, and uppercase normalization")
    void testSearchHistoryNormalization() {
        SearchHistory sh1 = new SearchHistory(5, "  java design patterns  ", "title");
        assertEquals(5, sh1.getUserId());
        assertEquals("java design patterns", sh1.getQueryText());
        assertEquals("TITLE", sh1.getSearchType());
        assertNotNull(sh1.getSearchedAt());

        // Default to ALL when searchType is null or empty
        SearchHistory sh2 = new SearchHistory(5, "clean code", null);
        assertEquals("ALL", sh2.getSearchType());

        SearchHistory sh3 = new SearchHistory(5, "refactoring", "   ");
        assertEquals("ALL", sh3.getSearchType());

        // Null query text safely handled
        SearchHistory sh4 = new SearchHistory(5, null, "genre");
        assertEquals("", sh4.getQueryText());
        assertEquals("GENRE", sh4.getSearchType());
    }

    @Test
    @DisplayName("Verify SearchHistory setters and toString")
    void testSearchHistorySettersAndToString() {
        SearchHistory sh = new SearchHistory();
        sh.setId(1);
        sh.setUserId(20);
        sh.setQueryText("Algorithms");
        sh.setSearchType("author");
        LocalDateTime now = LocalDateTime.now();
        sh.setSearchedAt(now);

        assertEquals(1, sh.getId());
        assertEquals(20, sh.getUserId());
        assertEquals("Algorithms", sh.getQueryText());
        assertEquals("AUTHOR", sh.getSearchType());
        assertEquals(now, sh.getSearchedAt());

        String str = sh.toString();
        assertTrue(str.contains("Algorithms"));
        assertTrue(str.contains("AUTHOR"));
    }
}
