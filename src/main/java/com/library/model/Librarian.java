package com.library.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Concrete representation of an administrative Librarian user.
 */
public class Librarian extends User {
    private static final long serialVersionUID = 1L;

    public Librarian() {
        super();
        this.role = Role.LIBRARIAN;
    }

    public Librarian(String membershipId, String name, String email, String passwordHash) {
        this(null, membershipId, name, email, passwordHash);
    }

    public Librarian(Integer id, String membershipId, String name, String email, String passwordHash) {
        super(id, membershipId, name, email, passwordHash, Role.LIBRARIAN);
    }

    public Librarian(Integer id, String membershipId, String name, String email, String passwordHash,
                     boolean emailNotifications, boolean dueDateAlerts, boolean newArrivalAlerts,
                     Timestamp createdAt, Timestamp updatedAt) {
        super(id, membershipId, name, email, passwordHash, Role.LIBRARIAN,
              emailNotifications, dueDateAlerts, newArrivalAlerts, createdAt, updatedAt);
    }

    public Librarian(Integer id, String membershipId, String name, String email, String passwordHash,
                     boolean emailNotifications, boolean dueDateAlerts, boolean newArrivalAlerts,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, membershipId, name, email, passwordHash, Role.LIBRARIAN,
              emailNotifications, dueDateAlerts, newArrivalAlerts, createdAt, updatedAt);
    }

    @Override
    public boolean canBorrow() {
        // Administrative accounts do not borrow books; borrowing is reserved for Member accounts
        return false;
    }

    @Override
    public int getMaxBorrowLimit() {
        return 0;
    }

    @Override
    public void setRole(Role role) {
        if (role != null && role != Role.LIBRARIAN) {
            throw new IllegalArgumentException("Cannot change role of Librarian entity to " + role);
        }
        super.setRole(Role.LIBRARIAN);
    }

    @Override
    public String getRoleSpecificDescription() {
        return "Staff Librarian with full catalog, member, and transaction management privileges.";
    }
}
