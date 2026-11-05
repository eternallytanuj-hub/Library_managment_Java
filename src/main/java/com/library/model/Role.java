package com.library.model;

/**
 * Enumeration representing user security and access roles.
 */
public enum Role {
    LIBRARIAN("Librarian", "/librarian/dashboard"),
    MEMBER("Member", "/member/dashboard");

    private final String displayName;
    private final String defaultDashboardUrl;

    Role(String displayName, String defaultDashboardUrl) {
        this.displayName = displayName;
        this.defaultDashboardUrl = defaultDashboardUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultDashboardUrl() {
        return defaultDashboardUrl;
    }

    public static Role fromString(String roleStr) {
        if (roleStr == null || roleStr.trim().isEmpty()) {
            return null;
        }
        for (Role r : Role.values()) {
            if (r.name().equalsIgnoreCase(roleStr.trim())) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + roleStr);
    }
}
