package com.library.dao.impl;

import com.library.dao.UserDao;
import com.library.exception.DatabaseOperationException;
import com.library.exception.DuplicateEntityException;
import com.library.model.Librarian;
import com.library.model.Member;
import com.library.model.Role;
import com.library.model.User;

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
 * JDBC implementation of {@link UserDao} with parameterized queries and polymorphic mapping.
 */
public class UserDaoJdbcImpl extends BaseJdbcDao implements UserDao {

    @Override
    public Optional<User> findById(Integer id) throws DatabaseOperationException {
        if (id == null) return Optional.empty();
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find user by ID: " + id, e);
        }
    }

    @Override
    public List<User> findAll() throws DatabaseOperationException {
        String sql = "SELECT * FROM users ORDER BY name ASC";
        List<User> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to list all users", e);
        }
    }

    @Override
    public User save(User user) throws DatabaseOperationException, DuplicateEntityException {
        String sql = "INSERT INTO users (membership_id, name, email, password_hash, role, " +
                     "email_notifications, due_date_alerts, new_arrival_alerts, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Timestamp now = user.getCreatedAt() != null ? user.getCreatedAt() : new Timestamp(System.currentTimeMillis());
            ps.setString(1, user.getMembershipId());
            ps.setString(2, user.getName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getRole().name());
            ps.setBoolean(6, user.isEmailNotifications());
            ps.setBoolean(7, user.isDueDateAlerts());
            ps.setBoolean(8, user.isNewArrivalAlerts());
            ps.setTimestamp(9, now);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DatabaseOperationException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                } else {
                    throw new DatabaseOperationException("Creating user failed, no ID obtained.");
                }
            }
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            return user;
        } catch (SQLException e) {
            throw translateException("save user", e);
        }
    }

    @Override
    public boolean update(User user) throws DatabaseOperationException, DuplicateEntityException {
        String sql = "UPDATE users SET membership_id = ?, name = ?, email = ?, password_hash = ?, " +
                     "role = ?, email_notifications = ?, due_date_alerts = ?, new_arrival_alerts = ? " +
                     "WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getMembershipId());
            ps.setString(2, user.getName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getRole().name());
            ps.setBoolean(6, user.isEmailNotifications());
            ps.setBoolean(7, user.isDueDateAlerts());
            ps.setBoolean(8, user.isNewArrivalAlerts());
            ps.setInt(9, user.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw translateException("update user", e);
        }
    }

    @Override
    public boolean deleteById(Integer id) throws DatabaseOperationException {
        if (id == null) return false;
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to delete user with ID: " + id, e);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) throws DatabaseOperationException {
        if (email == null) return Optional.empty();
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find user by email: " + email, e);
        }
    }

    @Override
    public Optional<User> findByMembershipId(String membershipId) throws DatabaseOperationException {
        if (membershipId == null) return Optional.empty();
        String sql = "SELECT * FROM users WHERE membership_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, membershipId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find user by membershipId: " + membershipId, e);
        }
    }

    @Override
    public List<User> findByRole(Role role) throws DatabaseOperationException {
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY name ASC";
        List<User> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role != null ? role.name() : Role.MEMBER.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToUser(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find users by role: " + role, e);
        }
    }

    @Override
    public List<Member> findAllMembersWithActiveLoanCount() throws DatabaseOperationException {
        String sql = "SELECT u.*, COALESCE(t.active_count, 0) AS active_loans_count " +
                     "FROM users u " +
                     "LEFT JOIN ( " +
                     "    SELECT user_id, COUNT(*) AS active_count " +
                     "    FROM transactions " +
                     "    WHERE status IN ('BORROWED', 'OVERDUE') " +
                     "    GROUP BY user_id " +
                     ") t ON u.user_id = t.user_id " +
                     "WHERE u.role = 'MEMBER' " +
                     "ORDER BY u.name ASC";
        List<Member> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User u = mapResultSetToUser(rs);
                if (u instanceof Member) {
                    Member m = (Member) u;
                    m.setActiveLoansCount(rs.getInt("active_loans_count"));
                    list.add(m);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to retrieve members with active loan counts", e);
        }
    }

    @Override
    public boolean updateProfile(int userId, String name, String email) throws DatabaseOperationException, DuplicateEntityException {
        String sql = "UPDATE users SET name = ?, email = ? WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw translateException("update user profile", e);
        }
    }

    @Override
    public boolean updatePassword(int userId, String newPasswordHash) throws DatabaseOperationException {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to update password for user " + userId, e);
        }
    }

    @Override
    public boolean updateAlertPreferences(int userId, boolean emailNotif, boolean dueDateAlerts, boolean newArrivalAlerts) throws DatabaseOperationException {
        String sql = "UPDATE users SET email_notifications = ?, due_date_alerts = ?, new_arrival_alerts = ? WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, emailNotif);
            ps.setBoolean(2, dueDateAlerts);
            ps.setBoolean(3, newArrivalAlerts);
            ps.setInt(4, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to update alert preferences for user " + userId, e);
        }
    }

    @Override
    public int countActiveLoans(int userId) throws DatabaseOperationException {
        String sql = "SELECT COUNT(*) FROM transactions WHERE user_id = ? AND status IN ('BORROWED', 'OVERDUE')";
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
            throw new DatabaseOperationException("Failed to count active loans for user " + userId, e);
        }
    }

    protected User mapResultSetToUser(ResultSet rs) throws SQLException {
        Role role = Role.fromString(rs.getString("role"));
        User user = (role == Role.LIBRARIAN) ? new Librarian() : new Member();
        user.setId(rs.getInt("user_id"));
        user.setMembershipId(rs.getString("membership_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(role);
        user.setEmailNotifications(rs.getBoolean("email_notifications"));
        user.setDueDateAlerts(rs.getBoolean("due_date_alerts"));
        user.setNewArrivalAlerts(rs.getBoolean("new_arrival_alerts"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setUpdatedAt(rs.getTimestamp("created_at"));
        return user;
    }
}
