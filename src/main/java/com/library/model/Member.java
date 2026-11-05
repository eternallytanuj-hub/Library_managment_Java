package com.library.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Concrete representation of a regular library Member with borrowing privileges.
 */
public class Member extends User {
    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_MAX_BORROW_LIMIT = 5;
    public static final int DEFAULT_MAX_BOOKS_ALLOWED = DEFAULT_MAX_BORROW_LIMIT;

    private int activeBorrowCount;
    private int maxBorrowLimit;

    public Member() {
        super();
        this.role = Role.MEMBER;
        this.activeBorrowCount = 0;
        this.maxBorrowLimit = DEFAULT_MAX_BORROW_LIMIT;
    }

    public Member(String membershipId, String name, String email, String passwordHash) {
        this(null, membershipId, name, email, passwordHash);
    }

    public Member(Integer id, String membershipId, String name, String email, String passwordHash) {
        super(id, membershipId, name, email, passwordHash, Role.MEMBER);
        this.activeBorrowCount = 0;
        this.maxBorrowLimit = DEFAULT_MAX_BORROW_LIMIT;
    }

    public Member(Integer id, String membershipId, String name, String email, String passwordHash,
                  boolean emailNotifications, boolean dueDateAlerts, boolean newArrivalAlerts,
                  Timestamp createdAt, Timestamp updatedAt) {
        super(id, membershipId, name, email, passwordHash, Role.MEMBER,
              emailNotifications, dueDateAlerts, newArrivalAlerts, createdAt, updatedAt);
        this.activeBorrowCount = 0;
        this.maxBorrowLimit = DEFAULT_MAX_BORROW_LIMIT;
    }

    public Member(Integer id, String membershipId, String name, String email, String passwordHash,
                  boolean emailNotifications, boolean dueDateAlerts, boolean newArrivalAlerts,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, membershipId, name, email, passwordHash, Role.MEMBER,
              emailNotifications, dueDateAlerts, newArrivalAlerts, createdAt, updatedAt);
        this.activeBorrowCount = 0;
        this.maxBorrowLimit = DEFAULT_MAX_BORROW_LIMIT;
    }

    @Override
    public boolean canBorrow() {
        return activeBorrowCount < maxBorrowLimit;
    }

    @Override
    public int getMaxBorrowLimit() {
        return maxBorrowLimit;
    }

    public void setMaxBorrowLimit(int maxBorrowLimit) {
        if (maxBorrowLimit < 0) {
            throw new IllegalArgumentException("Max borrow limit cannot be negative: " + maxBorrowLimit);
        }
        this.maxBorrowLimit = maxBorrowLimit;
    }

    public int getMaxBooksAllowed() {
        return getMaxBorrowLimit();
    }

    public void setMaxBooksAllowed(int maxBooksAllowed) {
        setMaxBorrowLimit(maxBooksAllowed);
    }

    public int getActiveBorrowCount() {
        return activeBorrowCount;
    }

    public void setActiveBorrowCount(int activeBorrowCount) {
        if (activeBorrowCount < 0) {
            throw new IllegalArgumentException("Active borrow count cannot be negative: " + activeBorrowCount);
        }
        this.activeBorrowCount = activeBorrowCount;
    }

    public int getActiveLoansCount() {
        return getActiveBorrowCount();
    }

    public void setActiveLoansCount(int activeLoansCount) {
        setActiveBorrowCount(activeLoansCount);
    }

    public void incrementActiveBorrowCount() {
        if (this.activeBorrowCount >= this.maxBorrowLimit) {
            throw new IllegalStateException("Member has reached maximum borrow limit of " + maxBorrowLimit);
        }
        this.activeBorrowCount++;
    }

    public void decrementActiveBorrowCount() {
        if (this.activeBorrowCount > 0) {
            this.activeBorrowCount--;
        }
    }

    @Override
    public void setRole(Role role) {
        if (role != null && role != Role.MEMBER) {
            throw new IllegalArgumentException("Cannot change role of Member entity to " + role);
        }
        super.setRole(Role.MEMBER);
    }

    @Override
    public String getRoleSpecificDescription() {
        return "Library Member with catalog search and book borrowing privileges.";
    }
}
