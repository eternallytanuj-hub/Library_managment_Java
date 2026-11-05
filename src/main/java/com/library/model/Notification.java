package com.library.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Concrete domain model for system alerts and announcements dispatched to members.
 */
public class Notification extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Integer userId;
    private String title;
    private String message;
    private NotificationType type;
    private boolean isRead;

    public Notification() {
        super();
        this.type = NotificationType.GENERAL;
        this.isRead = false;
    }

    public Notification(Integer userId, String title, String message, NotificationType type) {
        super();
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = (type != null) ? type : NotificationType.GENERAL;
        this.isRead = false;
    }

    public Notification(Integer id, Integer userId, String title, String message, 
                        NotificationType type, boolean isRead, Timestamp createdAt) {
        super(id, createdAt, createdAt);
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = (type != null) ? type : NotificationType.GENERAL;
        this.isRead = isRead;
    }

    public Notification(Integer id, Integer userId, String title, String message, 
                        NotificationType type, boolean isRead, LocalDateTime createdAt) {
        super(id, createdAt, createdAt);
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = (type != null) ? type : NotificationType.GENERAL;
        this.isRead = isRead;
    }

    // Business Methods
    public void markAsRead() {
        this.isRead = true;
    }

    public void markAsUnread() {
        this.isRead = false;
    }

    // Getters and Setters
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        this.isRead = read;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", type=" + type +
                ", isRead=" + isRead +
                ", createdAt=" + createdAt +
                '}';
    }
}
