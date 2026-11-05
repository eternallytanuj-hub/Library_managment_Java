package com.library.service;

import com.library.dao.BaseDaoTest;
import com.library.dao.UserDao;
import com.library.dao.impl.UserDaoJdbcImpl;
import com.library.exception.AuthenticationException;
import com.library.exception.DuplicateEntityException;
import com.library.exception.MemberNotFoundException;
import com.library.exception.ValidationException;
import com.library.model.Librarian;
import com.library.model.Member;
import com.library.model.Role;
import com.library.model.User;
import com.library.service.impl.AuthServiceImpl;
import com.library.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AuthService Integration Tests")
class AuthServiceTest extends BaseDaoTest {

    private UserDao userDao;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userDao = new UserDaoJdbcImpl();
        authService = new AuthServiceImpl(userDao);
    }

    @Test
    @DisplayName("Should successfully login member with valid email and password")
    void testLogin_MemberSuccess() throws Exception {
        Member member = new Member("MEM-1001", "Bob Member", "bob@example.com", PasswordUtil.hashPassword("secret123"));
        userDao.save(member);

        User loggedIn = authService.login("bob@example.com", "secret123");
        assertNotNull(loggedIn);
        assertEquals("bob@example.com", loggedIn.getEmail());
        assertEquals(Role.MEMBER, loggedIn.getRole());
        assertInstanceOf(Member.class, loggedIn);
    }

    @Test
    @DisplayName("Should successfully login librarian with valid credentials")
    void testLogin_LibrarianSuccess() throws Exception {
        Librarian lib = new Librarian("LIB-001", "Admin Lib", "admin@library.com", PasswordUtil.hashPassword("adminPass1"));
        userDao.save(lib);

        User loggedIn = authService.login("admin@library.com", "adminPass1");
        assertNotNull(loggedIn);
        assertEquals(Role.LIBRARIAN, loggedIn.getRole());
        assertInstanceOf(Librarian.class, loggedIn);
    }

    @Test
    @DisplayName("Should throw AuthenticationException on incorrect password")
    void testLogin_WrongPassword() throws Exception {
        Member member = new Member("MEM-1002", "Charlie", "charlie@test.com", PasswordUtil.hashPassword("realPass"));
        userDao.save(member);

        assertThrows(AuthenticationException.class, () -> authService.login("charlie@test.com", "wrongPass"));
    }

    @Test
    @DisplayName("Should throw AuthenticationException on non-existent email")
    void testLogin_UserNotFound() {
        assertThrows(AuthenticationException.class, () -> authService.login("nonexistent@domain.com", "anyPass"));
    }

    @Test
    @DisplayName("Should throw ValidationException on empty email or password")
    void testLogin_EmptyInputs() {
        assertThrows(ValidationException.class, () -> authService.login("", "password"));
        assertThrows(ValidationException.class, () -> authService.login("user@test.com", ""));
        assertThrows(ValidationException.class, () -> authService.login(null, "password"));
        assertThrows(ValidationException.class, () -> authService.login("user@test.com", null));
    }

    @Test
    @DisplayName("Should register new member, auto-assign membership ID, and enable login")
    void testRegister_Success() throws Exception {
        Member registered = authService.register("Diana Prince", "diana@test.com", "wonderwoman123");

        assertNotNull(registered.getId());
        assertNotNull(registered.getMembershipId());
        assertTrue(registered.getMembershipId().startsWith("MEM-"));
        assertEquals("Diana Prince", registered.getName());
        assertEquals("diana@test.com", registered.getEmail());

        // Verify password was hashed and user can log in
        User loggedIn = authService.login("diana@test.com", "wonderwoman123");
        assertEquals(registered.getId(), loggedIn.getId());
    }

    @Test
    @DisplayName("Should throw DuplicateEntityException when registering duplicate email")
    void testRegister_DuplicateEmail() throws Exception {
        authService.register("User One", "shared@test.com", "password123");

        assertThrows(DuplicateEntityException.class, () ->
                authService.register("User Two", "shared@test.com", "password456"));
    }

    @Test
    @DisplayName("Should validate name, email, and password during registration")
    void testRegister_ValidationFailures() {
        assertThrows(ValidationException.class, () -> authService.register("", "valid@test.com", "password123"));
        assertThrows(ValidationException.class, () -> authService.register("Valid Name", "invalid-email-format", "password123"));
        assertThrows(ValidationException.class, () -> authService.register("Valid Name", "valid@test.com", "123"));
    }

    @Test
    @DisplayName("Should change password with valid current password")
    void testChangePassword_Success() throws Exception {
        Member member = authService.register("Frank Castle", "frank@test.com", "punisher123");

        boolean changed = authService.changePassword(member.getId(), "punisher123", "newPassword789");
        assertTrue(changed);

        // Old password fails, new password succeeds
        assertThrows(AuthenticationException.class, () -> authService.login("frank@test.com", "punisher123"));
        User user = authService.login("frank@test.com", "newPassword789");
        assertEquals(member.getId(), user.getId());
    }

    @Test
    @DisplayName("Should reject changePassword when current password does not match")
    void testChangePassword_CurrentPasswordMismatch() throws Exception {
        Member member = authService.register("Bruce Wayne", "bruce@test.com", "batman123");

        assertThrows(AuthenticationException.class, () ->
                authService.changePassword(member.getId(), "wrongOldPassword", "newPassword456"));
    }

    @Test
    @DisplayName("Should update user profile and optionally change password")
    void testUpdateProfile_Success() throws Exception {
        Member member = authService.register("Clark Kent", "clark@test.com", "superman123");

        User updated = authService.updateProfile(member.getId(), "Kal-El", "kalel@krypton.com",
                "superman123", "kryptonite99");

        assertEquals("Kal-El", updated.getName());
        assertEquals("kalel@krypton.com", updated.getEmail());

        // Verify login with new email and new password
        User loggedIn = authService.login("kalel@krypton.com", "kryptonite99");
        assertEquals("Kal-El", loggedIn.getName());
    }

    @Test
    @DisplayName("Should reject profile update when new email belongs to another user")
    void testUpdateProfile_DuplicateEmailCollision() throws Exception {
        Member m1 = authService.register("User 1", "u1@test.com", "pass123");
        Member m2 = authService.register("User 2", "u2@test.com", "pass123");

        assertThrows(DuplicateEntityException.class, () ->
                authService.updateProfile(m2.getId(), "User 2 Renamed", "u1@test.com", null, null));
    }

    @Test
    @DisplayName("Should throw MemberNotFoundException on updateProfile for non-existent user")
    void testUpdateProfile_UserNotFound() {
        assertThrows(MemberNotFoundException.class, () ->
                authService.updateProfile(99999, "Non Existent", "ghost@test.com", null, null));
    }
}
