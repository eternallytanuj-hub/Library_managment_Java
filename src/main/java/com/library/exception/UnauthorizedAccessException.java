package com.library.exception;

/**
 * Checked exception thrown when an unauthorized user attempts to access a protected resource.
 */
public class UnauthorizedAccessException extends LibraryException {
    private static final long serialVersionUID = 1L;

    private String attemptedResource;
    private String requiredRole;

    public UnauthorizedAccessException(String message) {
        super(message);
    }

    public UnauthorizedAccessException(String attemptedResource, String requiredRole) {
        super(String.format("Unauthorized access to '%s'. Required role: '%s'", attemptedResource, requiredRole));
        this.attemptedResource = attemptedResource;
        this.requiredRole = requiredRole;
    }

    public String getAttemptedResource() {
        return attemptedResource;
    }

    public String getRequiredRole() {
        return requiredRole;
    }
}
