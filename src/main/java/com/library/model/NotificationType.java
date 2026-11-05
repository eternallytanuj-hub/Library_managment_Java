package com.library.model;

/**
 * Enumeration representing alert notification channels and classifications.
 */
public enum NotificationType {
    DUE_DATE("Due Date Reminder"),
    NEW_ARRIVAL("New Book Arrival"),
    SYSTEM("System Notice"),
    GENERAL("General Announcement");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLabel() {
        return displayName;
    }

    public static NotificationType fromString(String typeStr) {
        if (typeStr == null || typeStr.trim().isEmpty()) {
            return GENERAL;
        }
        for (NotificationType t : NotificationType.values()) {
            if (t.name().equalsIgnoreCase(typeStr.trim()) || t.displayName.equalsIgnoreCase(typeStr.trim())) {
                return t;
            }
        }
        return GENERAL;
    }
}
