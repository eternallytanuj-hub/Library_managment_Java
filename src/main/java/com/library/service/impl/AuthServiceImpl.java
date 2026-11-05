package com.library.service.impl;

import com.library.dao.UserDao;
import com.library.dao.impl.UserDaoJdbcImpl;
import com.library.exception.AuthenticationException;
import com.library.exception.DatabaseOperationException;
import com.library.exception.DuplicateEntityException;
import com.library.exception.LibraryException;
import com.library.exception.MemberNotFoundException;
import com.library.exception.ValidationException;
import com.library.model.Member;
import com.library.model.User;
import com.library.service.AuthService;
import com.library.util.PasswordUtil;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * Concrete implementation of {@link AuthService} handling credential authentication,
 * registration with BCrypt password hashing, and profile maintenance.
 */
public class AuthServiceImpl implements AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private final UserDao userDao;

    public AuthServiceImpl() {
        this(new UserDaoJdbcImpl());
    }

    public AuthServiceImpl(UserDao userDao) {
        this.userDao = Objects.requireNonNull(userDao, "UserDao must not be null");
    }

    @Override
    public User login(String email, String password) throws AuthenticationException, ValidationException, DatabaseOperationException {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("email", "Email address cannot be empty");
        }
        if (password == null || password.isEmpty()) {
            throw new ValidationException("password", "Password cannot be empty");
        }

        Optional<User> userOpt = userDao.findByEmail(email.trim());
        if (userOpt.isEmpty()) {
            throw new AuthenticationException("Invalid email or password");
        }

        User user = userOpt.get();
        if (!PasswordUtil.checkPassword(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password");
        }

        return user;
    }

    @Override
    public Member register(String name, String email, String password) throws DuplicateEntityException, ValidationException, DatabaseOperationException {
        validateRegistrationFields(name, email, password);

        String trimmedEmail = email.trim().toLowerCase();
        if (userDao.findByEmail(trimmedEmail).isPresent()) {
            throw new DuplicateEntityException("User", "email", trimmedEmail);
        }

        String membershipId = generateMembershipId();
        String hashedPassword = PasswordUtil.hashPassword(password);

        Member member = new Member(membershipId, name.trim(), trimmedEmail, hashedPassword);
        member.setEmailNotifications(true);
        member.setDueDateAlerts(true);
        member.setNewArrivalAlerts(true);

        User savedUser = userDao.save(member);
        if (savedUser instanceof Member) {
            return (Member) savedUser;
        }
        // Should not happen, but safe cast fallback
        member.setId(savedUser.getId());
        return member;
    }

    @Override
    public boolean changePassword(int userId, String currentPassword, String newPassword)
            throws AuthenticationException, ValidationException, DatabaseOperationException {
        if (currentPassword == null || currentPassword.isEmpty()) {
            throw new ValidationException("currentPassword", "Current password cannot be empty");
        }
        if (newPassword == null || !PasswordUtil.isValidPassword(newPassword)) {
            throw new ValidationException("newPassword", "New password must be at least " + PasswordUtil.MIN_PASSWORD_LENGTH + " characters");
        }

        Optional<User> userOpt = userDao.findById(userId);
        if (userOpt.isEmpty()) {
            throw new AuthenticationException("User not found");
        }

        User user = userOpt.get();
        if (!PasswordUtil.checkPassword(currentPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Current password does not match");
        }

        String newPasswordHash = PasswordUtil.hashPassword(newPassword);
        return userDao.updatePassword(userId, newPasswordHash);
    }

    @Override
    public User updateProfile(int userId, String name, String email, String currentPassword, String newPassword)
            throws LibraryException, DatabaseOperationException {
        if (name == null || name.trim().length() < 2) {
            throw new ValidationException("name", "Name must be at least 2 characters");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ValidationException("email", "Invalid email address format");
        }

        Optional<User> userOpt = userDao.findById(userId);
        if (userOpt.isEmpty()) {
            throw new MemberNotFoundException(userId);
        }

        User existingUser = userOpt.get();
        String trimmedEmail = email.trim().toLowerCase();

        // Check if email changed and is taken by another user
        if (!existingUser.getEmail().equalsIgnoreCase(trimmedEmail)) {
            Optional<User> otherUser = userDao.findByEmail(trimmedEmail);
            if (otherUser.isPresent() && !otherUser.get().getId().equals(userId)) {
                throw new DuplicateEntityException("User", "email", trimmedEmail);
            }
        }

        // If user wants to update password
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (currentPassword == null || currentPassword.isEmpty()) {
                throw new ValidationException("currentPassword", "Current password is required to set a new password");
            }
            if (!PasswordUtil.checkPassword(currentPassword, existingUser.getPasswordHash())) {
                throw new AuthenticationException("Current password does not match");
            }
            if (!PasswordUtil.isValidPassword(newPassword)) {
                throw new ValidationException("newPassword", "New password must be at least " + PasswordUtil.MIN_PASSWORD_LENGTH + " characters");
            }
            String newHash = PasswordUtil.hashPassword(newPassword);
            userDao.updatePassword(userId, newHash);
            existingUser.setPasswordHash(newHash);
        }

        userDao.updateProfile(userId, name.trim(), trimmedEmail);
        existingUser.setName(name.trim());
        existingUser.setEmail(trimmedEmail);
        return existingUser;
    }

    @Override
    public Optional<User> getUserById(int userId) throws DatabaseOperationException {
        return userDao.findById(userId);
    }

    @Override
    public Optional<User> getUserByEmail(String email) throws DatabaseOperationException {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }
        return userDao.findByEmail(email.trim().toLowerCase());
    }

    private void validateRegistrationFields(String name, String email, String password) throws ValidationException {
        if (name == null || name.trim().length() < 2) {
            throw new ValidationException("name", "Name must be at least 2 characters");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ValidationException("email", "Invalid email address format");
        }
        if (password == null || !PasswordUtil.isValidPassword(password)) {
            throw new ValidationException("password", "Password must be at least " + PasswordUtil.MIN_PASSWORD_LENGTH + " characters");
        }
    }

    private String generateMembershipId() throws DatabaseOperationException {
        for (int attempts = 0; attempts < 100; attempts++) {
            int randomNum = 1000 + ThreadLocalRandom.current().nextInt(9000);
            String candidate = "MEM-" + randomNum;
            if (userDao.findByMembershipId(candidate).isEmpty()) {
                return candidate;
            }
        }
        return "MEM-" + (System.currentTimeMillis() % 100000);
    }
}
