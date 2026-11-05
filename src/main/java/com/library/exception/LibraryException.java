package com.library.exception;

/**
 * Root checked exception for all domain and operational failures in the Library Management System.
 */
public class LibraryException extends Exception {
    private static final long serialVersionUID = 1L;

    public LibraryException() {
        super("Library application exception");
    }

    public LibraryException(String message) {
        super(message);
    }

    public LibraryException(String message, Throwable cause) {
        super(message, cause);
    }

    public LibraryException(Throwable cause) {
        super(cause != null ? cause.getMessage() : null, cause);
    }
}
