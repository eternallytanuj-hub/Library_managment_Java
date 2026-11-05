package com.library.dao;

import com.library.model.Member;
import com.library.model.User;
import com.library.exception.DatabaseOperationException;

import java.util.List;
import java.util.Optional;

/**
 * DAO contract for Member accounts, user credential authentication, and preference updates.
 */
public interface MemberDao extends GenericDao<Member, Integer> {

    /**
     * Finds a member by unique email address.
     * @param email User email string
     * @return Optional containing Member if found
     */
    Optional<Member> findByEmail(String email) throws DatabaseOperationException;

    /**
     * Finds a member by unique library membership ID (e.g. "LIB-M-00101").
     * @param membershipId Alphanumeric membership identifier
     * @return Optional containing Member if found
     */
    Optional<Member> findByMembershipId(String membershipId) throws DatabaseOperationException;

    /**
     * Finds any User (Librarian or Member) by email for authentication verification.
     * @param email User email string
     * @return Optional containing generic User entity
     */
    Optional<User> findUserByEmail(String email) throws DatabaseOperationException;

    /**
     * Checks if an email address is already registered to another user account.
     * @param email Email to check
     * @param excludeUserId User ID to exclude (or null for new registrations)
     * @return true if email collision exists
     */
    boolean existsByEmail(String email, Integer excludeUserId) throws DatabaseOperationException;

    /**
     * Updates user password hash.
     * @param userId Target user ID
     * @param passwordHash Hashed password string (BCrypt / SHA-256)
     * @return true if password updated
     */
    boolean updatePassword(int userId, String passwordHash) throws DatabaseOperationException;

    /**
     * Updates notification alert preferences for a user.
     * @param userId Target user ID
     * @param emailNotifications Email notifications toggle
     * @param dueDateAlerts Due date alerts toggle
     * @param newArrivalAlerts New book arrival alerts toggle
     * @return true if preferences updated successfully
     */
    boolean updateNotificationPreferences(int userId, boolean emailNotifications, boolean dueDateAlerts, boolean newArrivalAlerts) throws DatabaseOperationException;

    /**
     * Counts active borrowed books currently held by a member.
     * @param userId Target user ID
     * @return Count of active loans (status 'BORROWED' or 'OVERDUE')
     */
    int getActiveLoanCount(int userId) throws DatabaseOperationException;

    /**
     * Checks if a member has active loans preventing account deletion or deactivation.
     * @param userId Target user ID
     * @return true if member holds unreturned books
     */
    boolean hasActiveLoans(int userId) throws DatabaseOperationException;

    /**
     * Retrieves all members with their active borrow count populated.
     * @return List of members with active loan statistics
     */
    List<Member> findAllWithActiveLoanCounts() throws DatabaseOperationException;
}
