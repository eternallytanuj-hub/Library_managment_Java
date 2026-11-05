package com.library.dao;

import com.library.model.Notification;
import com.library.exception.DatabaseOperationException;

import java.util.List;

/**
 * Data Access Object interface for {@link Notification} alert entities.
 */
public interface NotificationDao extends GenericDao<Notification, Integer> {

    List<Notification> findByUserId(int userId) throws DatabaseOperationException;

    List<Notification> findUnreadByUserId(int userId) throws DatabaseOperationException;

    int countUnreadByUserId(int userId) throws DatabaseOperationException;

    boolean markAsRead(int notificationId, int userId) throws DatabaseOperationException;

    boolean markAllAsRead(int userId) throws DatabaseOperationException;

    int insertBatch(List<Notification> notifications) throws DatabaseOperationException;
}
