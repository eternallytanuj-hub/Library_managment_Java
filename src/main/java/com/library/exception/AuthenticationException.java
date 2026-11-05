package com.library.exception;

/**
 * Checked exception thrown when user authentication fails.
 */
public class AuthenticationException extends UnauthorizedAccessException {
    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }
}
