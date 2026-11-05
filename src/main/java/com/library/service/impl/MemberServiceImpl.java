package com.library.service.impl;

import com.library.dao.UserDao;
import com.library.dao.impl.UserDaoJdbcImpl;
import com.library.exception.DatabaseOperationException;
import com.library.exception.DuplicateEntityException;
import com.library.exception.LibraryException;
import com.library.exception.MemberNotFoundException;
import com.library.exception.UnauthorizedAccessException;
import com.library.exception.ValidationException;
import com.library.model.Member;
import com.library.model.User;
import com.library.service.AuthService;
import com.library.service.MemberService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Concrete implementation of {@link MemberService} for member management,
 * preferences configuration, and account deletion guards.
 */
public class MemberServiceImpl implements MemberService {

    private final UserDao userDao;
    private final AuthService authService;

    public MemberServiceImpl() {
        this.userDao = new UserDaoJdbcImpl();
        this.authService = new AuthServiceImpl(this.userDao);
    }

    public MemberServiceImpl(UserDao userDao) {
        this.userDao = Objects.requireNonNull(userDao, "UserDao must not be null");
        this.authService = new AuthServiceImpl(this.userDao);
    }

    public MemberServiceImpl(UserDao userDao, AuthService authService) {
        this.userDao = Objects.requireNonNull(userDao, "UserDao must not be null");
        this.authService = Objects.requireNonNull(authService, "AuthService must not be null");
    }

    @Override
    public User authenticate(String email, String password) throws UnauthorizedAccessException, DatabaseOperationException {
        try {
            return authService.login(email, password);
        } catch (ValidationException e) {
            throw new UnauthorizedAccessException("Authentication failed: " + e.getMessage(), "USER");
        }
    }

    @Override
    public Member registerMember(String name, String email, String password)
            throws DuplicateEntityException, ValidationException, DatabaseOperationException {
        return authService.register(name, email, password);
    }

    @Override
    public Member updateProfile(int userId, String name, String email, String currentPassword, String newPassword)
            throws LibraryException, DatabaseOperationException {
        User updated = authService.updateProfile(userId, name, email, currentPassword, newPassword);
        if (updated instanceof Member) {
            return (Member) updated;
        }
        Optional<Member> memberOpt = getMemberById(userId);
        if (memberOpt.isPresent()) {
            return memberOpt.get();
        }
        throw new MemberNotFoundException(userId);
    }

    @Override
    public void updateNotificationPreferences(int userId, boolean dueDateAlerts, boolean newArrivalAlerts)
            throws DatabaseOperationException {
        updateNotificationPreferences(userId, true, dueDateAlerts, newArrivalAlerts);
    }

    @Override
    public void updateNotificationPreferences(int userId, boolean emailNotifications, boolean dueDateAlerts, boolean newArrivalAlerts)
            throws DatabaseOperationException {
        userDao.updateAlertPreferences(userId, emailNotifications, dueDateAlerts, newArrivalAlerts);
    }

    @Override
    public List<Member> getAllMembers() throws DatabaseOperationException {
        return userDao.findAllMembersWithActiveLoanCount();
    }

    @Override
    public Optional<Member> getMemberById(int userId) throws DatabaseOperationException {
        Optional<User> userOpt = userDao.findById(userId);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User u = userOpt.get();
        if (u instanceof Member) {
            Member m = (Member) u;
            m.setActiveLoansCount(userDao.countActiveLoans(userId));
            return Optional.of(m);
        }
        return Optional.empty();
    }

    @Override
    public boolean deleteMember(int userId) throws LibraryException, DatabaseOperationException {
        Optional<User> userOpt = userDao.findById(userId);
        if (userOpt.isEmpty()) {
            throw new MemberNotFoundException(userId);
        }

        int activeLoans = userDao.countActiveLoans(userId);
        if (activeLoans > 0) {
            throw new LibraryException("Cannot delete member account: member currently holds " + activeLoans + " unreturned books.");
        }

        return userDao.deleteById(userId);
    }
}
