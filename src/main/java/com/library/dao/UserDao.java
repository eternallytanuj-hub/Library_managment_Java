package com.library.dao;

import com.library.model.Member;
import com.library.model.Role;
import com.library.model.User;
import com.library.exception.DatabaseOperationException;
import com.library.exception.DuplicateEntityException;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for {@link User}, {@link Member}, and {@link com.library.model.Librarian} entities.
 */
public interface UserDao extends GenericDao<User, Integer> {

    Optional<User> findByEmail(String email) throws DatabaseOperationException;

    Optional<User> findByMembershipId(String membershipId) throws DatabaseOperationException;

    List<User> findByRole(Role role) throws DatabaseOperationException;

    List<Member> findAllMembersWithActiveLoanCount() throws DatabaseOperationException;

    boolean updateProfile(int userId, String name, String email) throws DatabaseOperationException, DuplicateEntityException;

    boolean updatePassword(int userId, String newPasswordHash) throws DatabaseOperationException;

    boolean updateAlertPreferences(int userId, boolean emailNotif, boolean dueDateAlerts, boolean newArrivalAlerts) throws DatabaseOperationException;

    int countActiveLoans(int userId) throws DatabaseOperationException;
}
