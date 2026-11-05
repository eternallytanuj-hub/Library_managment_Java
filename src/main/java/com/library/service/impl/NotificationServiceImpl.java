package com.library.service.impl;

import com.library.dao.NotificationDao;
import com.library.dao.UserDao;
import com.library.dao.impl.NotificationDaoJdbcImpl;
import com.library.dao.impl.UserDaoJdbcImpl;
import com.library.daemon.AsyncNotificationDispatcher;
import com.library.exception.DatabaseOperationException;
import com.library.exception.ValidationException;
import com.library.model.Notification;
import com.library.model.NotificationType;
import com.library.model.Role;
import com.library.model.User;
import com.library.service.NotificationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Concrete implementation of {@link NotificationService} handling notification delivery,
 * broadcast dispatches, and asynchronous alert scheduling.
 */
public class NotificationServiceImpl implements NotificationService {

    private final NotificationDao notificationDao;
    private final UserDao userDao;
    private final AsyncNotificationDispatcher dispatcher;

    public NotificationServiceImpl() {
        this(new NotificationDaoJdbcImpl(), new UserDaoJdbcImpl(), AsyncNotificationDispatcher.getInstance());
    }

    public NotificationServiceImpl(NotificationDao notificationDao, UserDao userDao) {
        this(notificationDao, userDao, AsyncNotificationDispatcher.getInstance());
    }

    public NotificationServiceImpl(NotificationDao notificationDao, UserDao userDao, AsyncNotificationDispatcher dispatcher) {
        this.notificationDao = Objects.requireNonNull(notificationDao, "NotificationDao must not be null");
        this.userDao = Objects.requireNonNull(userDao, "UserDao must not be null");
        this.dispatcher = dispatcher;
    }

    @Override
    public Notification sendNotification(int userId, String title, String message, NotificationType type)
            throws ValidationException, DatabaseOperationException {
        validateNotificationContent(title, message);
        Notification notification = new Notification(userId, title.trim(), message.trim(),
                type != null ? type : NotificationType.GENERAL);
        try {
            return notificationDao.save(notification);
        } catch (com.library.exception.DuplicateEntityException e) {
            throw new DatabaseOperationException("Duplicate notification: " + e.getMessage(), e);
        }
    }

    @Override
    public int broadcastNotification(String title, String message, NotificationType type)
            throws ValidationException, DatabaseOperationException {
        validateNotificationContent(title, message);

        List<User> members = userDao.findByRole(Role.MEMBER);
        if (members == null || members.isEmpty()) {
            return 0;
        }

        NotificationType finalType = type != null ? type : NotificationType.GENERAL;
        List<Notification> batch = new ArrayList<>();

        for (User member : members) {
            // Check preference filter
            boolean send = true;
            if (finalType == NotificationType.NEW_ARRIVAL && !member.isNewArrivalAlerts()) {
                send = false;
            } else if (finalType == NotificationType.DUE_DATE && !member.isDueDateAlerts()) {
                send = false;
            }

            if (send && member.getId() != null) {
                batch.add(new Notification(member.getId(), title.trim(), message.trim(), finalType));
            }
        }

        if (batch.isEmpty()) {
            return 0;
        }

        return notificationDao.insertBatch(batch);
    }

    @Override
    public List<Notification> getNotificationsForUser(int userId) throws DatabaseOperationException {
        return notificationDao.findByUserId(userId);
    }

    @Override
    public List<Notification> getUnreadNotificationsForUser(int userId) throws DatabaseOperationException {
        return notificationDao.findUnreadByUserId(userId);
    }

    @Override
    public int getUnreadNotificationCount(int userId) throws DatabaseOperationException {
        return notificationDao.countUnreadByUserId(userId);
    }

    @Override
    public boolean markAsRead(int notificationId, int userId) throws DatabaseOperationException {
        return notificationDao.markAsRead(notificationId, userId);
    }

    @Override
    public boolean markAllAsRead(int userId) throws DatabaseOperationException {
        return notificationDao.markAllAsRead(userId);
    }

    @Override
    public void enqueueNotification(Notification notification) {
        if (notification == null) return;
        if (dispatcher != null) {
            dispatcher.enqueue(notification);
        } else {
            try {
                notificationDao.save(notification);
            } catch (Exception ignored) {}
        }
    }

    private void validateNotificationContent(String title, String message) throws ValidationException {
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("title", "Notification title cannot be empty");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new ValidationException("message", "Notification message cannot be empty");
        }
    }
}
