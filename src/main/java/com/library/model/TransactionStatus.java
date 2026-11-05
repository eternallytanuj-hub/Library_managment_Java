package com.library.model;

/**
 * Enumeration representing borrowing transaction lifecycle states.
 */
public enum TransactionStatus {
    BORROWED("Borrowed"),
    RETURNED("Returned"),
    OVERDUE("Overdue");

    private final String displayName;

    TransactionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this == BORROWED || this == OVERDUE;
    }

    public static TransactionStatus fromString(String statusStr) {
        if (statusStr == null) return null;
        for (TransactionStatus s : TransactionStatus.values()) {
            if (s.name().equalsIgnoreCase(statusStr.trim())) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown transaction status: " + statusStr);
    }
}
