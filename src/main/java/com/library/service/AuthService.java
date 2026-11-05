package com.library.service;

import com.library.exception.AuthenticationException;
import com.library.exception.DatabaseOperationException;
import com.library.exception.DuplicateEntityException;
import com.library.exception.LibraryException;
import com.library.exception.ValidationException;
import com.library.model.Member;
import com.library.model.User;

import java.util.Optional;

/**
 * Service contract for user authentication, registration, and credentials management.
 */
public interface AuthService {

    /**
     * Authenticates a user with email and plain-text password using BCrypt.
     *
     * @param email User's email address
     * @param password Plain-text password
     * @return Authenticated User entity (Member or Librarian)
     * @throws AuthenticationException if credentials are invalid
     * @throws ValidationException if input parameters are invalid
     * @throws DatabaseOperationException if persistence error occurs
     */
    User login(String email, String password) throws AuthenticationException, ValidationException, DatabaseOperationException;

    /**
     * Registers a new Member account with auto-generated membership ID and hashed password.
     *
     * @param name Full name of the user
     * @param email Email address
     * @param password Plain-text initial password
     * @return Newly created Member entity
     * @throws DuplicateEntityException if email is already taken
     * @throws ValidationException if input parameters fail validation
     * @throws DatabaseOperationException if persistence error occurs
     */
    Member register(String name, String email, String password) throws DuplicateEntityException, ValidationException, DatabaseOperationException;

    /**
     * Changes user password after verifying the current password.
     *
     * @param userId Target user ID
     * @param currentPassword Current plain-text password
     * @param newPassword New plain-text password
     * @return true if password successfully updated
     * @throws AuthenticationException if current password does not match
     * @throws ValidationException if new password does not meet criteria
     * @throws DatabaseOperationException if persistence error occurs
     */
    boolean changePassword(int userId, String currentPassword, String newPassword) throws AuthenticationException, ValidationException, DatabaseOperationException;

    /**
     * Updates user profile (name, email) and optionally updates password if provided.
     *
     * @param userId Target user ID
     * @param name New name
     * @param email New email
     * @param currentPassword Current password (required if updating password)
     * @param newPassword New password (optional, null/blank if not changing password)
     * @return Updated User entity
     * @throws LibraryException if user not found, password mismatch, or validation fails
     * @throws DatabaseOperationException if persistence error occurs
     */
    User updateProfile(int userId, String name, String email, String currentPassword, String newPassword) throws LibraryException, DatabaseOperationException;

    /**
     * Finds user by primary key ID.
     */
    Optional<User> getUserById(int userId) throws DatabaseOperationException;

    /**
     * Finds user by unique email.
     */
    Optional<User> getUserByEmail(String email) throws DatabaseOperationException;
}
