package com.library.service;

import com.library.exception.DatabaseOperationException;
import com.library.exception.ValidationException;
import com.library.model.Notification;
import com.library.model.NotificationType;

import java.util.List;

/**
 * Service contract for notification authoring, broadcast delivery,
 * alert read state management, and asynchronous dispatch queue.
 */
public interface NotificationService {

    Notification sendNotification(int userId, String title, String message, NotificationType type)
            throws ValidationException, DatabaseOperationException;

    int broadcastNotification(String title, String message, NotificationType type)
            throws ValidationException, DatabaseOperationException;

    List<Notification> getNotificationsForUser(int userId) throws DatabaseOperationException;

    List<Notification> getUnreadNotificationsForUser(int userId) throws DatabaseOperationException;

    int getUnreadNotificationCount(int userId) throws DatabaseOperationException;

    boolean markAsRead(int notificationId, int userId) throws DatabaseOperationException;

    boolean markAllAsRead(int userId) throws DatabaseOperationException;

    void enqueueNotification(Notification notification);
}
