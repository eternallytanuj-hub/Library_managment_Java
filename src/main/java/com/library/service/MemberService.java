package com.library.service;

import com.library.exception.DatabaseOperationException;
import com.library.exception.DuplicateEntityException;
import com.library.exception.LibraryException;
import com.library.exception.UnauthorizedAccessException;
import com.library.exception.ValidationException;
import com.library.model.Member;
import com.library.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Service contract for member profile lifecycle, account administrative operations,
 * and notification preferences.
 */
public interface MemberService {

    User authenticate(String email, String password) throws UnauthorizedAccessException, DatabaseOperationException;

    Member registerMember(String name, String email, String password) throws DuplicateEntityException, ValidationException, DatabaseOperationException;

    Member updateProfile(int userId, String name, String email, String currentPassword, String newPassword) throws LibraryException, DatabaseOperationException;

    void updateNotificationPreferences(int userId, boolean dueDateAlerts, boolean newArrivalAlerts) throws DatabaseOperationException;

    void updateNotificationPreferences(int userId, boolean emailNotifications, boolean dueDateAlerts, boolean newArrivalAlerts) throws DatabaseOperationException;

    List<Member> getAllMembers() throws DatabaseOperationException;

    Optional<Member> getMemberById(int userId) throws DatabaseOperationException;

    boolean deleteMember(int userId) throws LibraryException, DatabaseOperationException;
}
