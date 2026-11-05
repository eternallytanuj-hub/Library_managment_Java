package com.library.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Abstract base class for all authenticated system users.
 */
public abstract class User extends BaseEntity {
    private static final long serialVersionUID = 1L;

    protected String membershipId;
    protected String name;
    protected String email;
    protected String passwordHash;
    protected Role role;
    protected boolean emailNotifications = true;
    protected boolean dueDateAlerts = true;
    protected boolean newArrivalAlerts = true;

    protected User() {
        super();
        this.emailNotifications = true;
        this.dueDateAlerts = true;
        this.newArrivalAlerts = true;
    }

    protected User(Integer id, String membershipId, String name, String email, String passwordHash, Role role) {
        super(id, new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()));
        this.membershipId = membershipId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.emailNotifications = true;
        this.dueDateAlerts = true;
        this.newArrivalAlerts = true;
    }

    protected User(Integer id, String membershipId, String name, String email, String passwordHash,
                    Role role, boolean emailNotifications, boolean dueDateAlerts, boolean newArrivalAlerts,
                    Timestamp createdAt, Timestamp updatedAt) {
        super(id, createdAt, updatedAt);
        this.membershipId = membershipId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.emailNotifications = emailNotifications;
        this.dueDateAlerts = dueDateAlerts;
        this.newArrivalAlerts = newArrivalAlerts;
    }

    protected User(Integer id, String membershipId, String name, String email, String passwordHash,
                    Role role, boolean emailNotifications, boolean dueDateAlerts, boolean newArrivalAlerts,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, createdAt, updatedAt);
        this.membershipId = membershipId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.emailNotifications = emailNotifications;
        this.dueDateAlerts = dueDateAlerts;
        this.newArrivalAlerts = newArrivalAlerts;
    }

    // Abstract polymorphic methods to be implemented by Librarian and Member
    public abstract boolean canBorrow();
    public abstract int getMaxBorrowLimit();

    public int getMaxBooksAllowed() {
        return getMaxBorrowLimit();
    }

    public String getRoleSpecificDescription() {
        return role != null ? role.getDisplayName() : "";
    }

    // Convenience role verification helpers
    public boolean isLibrarian() {
        return Role.LIBRARIAN.equals(this.role);
    }

    public boolean isMember() {
        return Role.MEMBER.equals(this.role);
    }

    // Getters and Setters
    public String getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(String membershipId) {
        this.membershipId = membershipId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public boolean isDueDateAlerts() {
        return dueDateAlerts;
    }

    public void setDueDateAlerts(boolean dueDateAlerts) {
        this.dueDateAlerts = dueDateAlerts;
    }

    public boolean isNewArrivalAlerts() {
        return newArrivalAlerts;
    }

    public void setNewArrivalAlerts(boolean newArrivalAlerts) {
        this.newArrivalAlerts = newArrivalAlerts;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", membershipId='" + membershipId + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", passwordHash='[PROTECTED]'" +
                ", role=" + role +
                ", emailNotifications=" + emailNotifications +
                ", dueDateAlerts=" + dueDateAlerts +
                ", newArrivalAlerts=" + newArrivalAlerts +
                ", createdAt=" + createdAt +
                '}';
    }
}
