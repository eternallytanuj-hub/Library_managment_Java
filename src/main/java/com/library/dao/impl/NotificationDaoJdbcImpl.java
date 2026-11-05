package com.library.dao.impl;

import com.library.dao.NotificationDao;
import com.library.exception.DatabaseOperationException;
import com.library.model.Notification;
import com.library.model.NotificationType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link NotificationDao} managing user alert notifications.
 */
public class NotificationDaoJdbcImpl extends BaseJdbcDao implements NotificationDao {

    @Override
    public Optional<Notification> findById(Integer id) throws DatabaseOperationException {
        if (id == null) return Optional.empty();
        String sql = "SELECT * FROM notifications WHERE notification_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToNotification(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find notification by ID: " + id, e);
        }
    }

    @Override
    public List<Notification> findAll() throws DatabaseOperationException {
        String sql = "SELECT * FROM notifications ORDER BY created_at DESC";
        List<Notification> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToNotification(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to list all notifications", e);
        }
    }

    @Override
    public Notification save(Notification n) throws DatabaseOperationException {
        String sql = "INSERT INTO notifications (user_id, title, message, type, is_read, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Timestamp now = n.getCreatedAt() != null ? n.getCreatedAt() : new Timestamp(System.currentTimeMillis());
            ps.setInt(1, n.getUserId());
            ps.setString(2, n.getTitle());
            ps.setString(3, n.getMessage());
            ps.setString(4, n.getType() != null ? n.getType().name() : NotificationType.GENERAL.name());
            ps.setBoolean(5, n.isRead());
            ps.setTimestamp(6, now);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DatabaseOperationException("Creating notification failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    n.setId(generatedKeys.getInt(1));
                } else {
                    throw new DatabaseOperationException("Creating notification failed, no ID obtained.");
                }
            }
            n.setCreatedAt(now);
            n.setUpdatedAt(now);
            return n;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to save notification", e);
        }
    }

    @Override
    public boolean update(Notification n) throws DatabaseOperationException {
        String sql = "UPDATE notifications SET user_id = ?, title = ?, message = ?, type = ?, is_read = ? " +
                     "WHERE notification_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, n.getUserId());
            ps.setString(2, n.getTitle());
            ps.setString(3, n.getMessage());
            ps.setString(4, n.getType().name());
            ps.setBoolean(5, n.isRead());
            ps.setInt(6, n.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to update notification: " + n.getId(), e);
        }
    }

    @Override
    public boolean deleteById(Integer id) throws DatabaseOperationException {
        if (id == null) return false;
        String sql = "DELETE FROM notifications WHERE notification_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to delete notification with ID: " + id, e);
        }
    }

    @Override
    public List<Notification> findByUserId(int userId) throws DatabaseOperationException {
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
        List<Notification> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToNotification(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find notifications for user: " + userId, e);
        }
    }

    @Override
    public List<Notification> findUnreadByUserId(int userId) throws DatabaseOperationException {
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND is_read = FALSE ORDER BY created_at DESC";
        List<Notification> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToNotification(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find unread notifications for user: " + userId, e);
        }
    }

    @Override
    public int countUnreadByUserId(int userId) throws DatabaseOperationException {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to count unread notifications for user: " + userId, e);
        }
    }

    @Override
    public boolean markAsRead(int notificationId, int userId) throws DatabaseOperationException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE notification_id = ? AND user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to mark notification as read: " + notificationId, e);
        }
    }

    @Override
    public boolean markAllAsRead(int userId) throws DatabaseOperationException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to mark all notifications as read for user: " + userId, e);
        }
    }

    @Override
    public int insertBatch(List<Notification> notifications) throws DatabaseOperationException {
        if (notifications == null || notifications.isEmpty()) return 0;
        String sql = "INSERT INTO notifications (user_id, title, message, type, is_read, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Notification n : notifications) {
                Timestamp now = n.getCreatedAt() != null ? n.getCreatedAt() : new Timestamp(System.currentTimeMillis());
                ps.setInt(1, n.getUserId());
                ps.setString(2, n.getTitle());
                ps.setString(3, n.getMessage());
                ps.setString(4, n.getType() != null ? n.getType().name() : NotificationType.GENERAL.name());
                ps.setBoolean(5, n.isRead());
                ps.setTimestamp(6, now);
                ps.addBatch();
            }
            int[] results = ps.executeBatch();
            int inserted = 0;
            for (int r : results) {
                if (r >= 0 || r == Statement.SUCCESS_NO_INFO) {
                    inserted++;
                }
            }
            return inserted;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to batch insert notifications", e);
        }
    }

    protected Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("notification_id"));
        n.setUserId(rs.getInt("user_id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setType(NotificationType.fromString(rs.getString("type")));
        n.setRead(rs.getBoolean("is_read"));
        n.setCreatedAt(rs.getTimestamp("created_at"));
        n.setUpdatedAt(rs.getTimestamp("created_at"));
        return n;
    }
}
