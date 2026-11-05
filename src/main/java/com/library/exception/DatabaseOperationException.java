package com.library.exception;

import java.sql.SQLException;

/**
 * Checked exception wrapping underlying JDBC/SQL and persistence failures.
 */
public class DatabaseOperationException extends LibraryException {
    private static final long serialVersionUID = 1L;

    public DatabaseOperationException(String message) {
        super(message);
    }

    public DatabaseOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseOperationException(Throwable cause) {
        super(cause != null ? cause.getMessage() : null, cause);
    }

    public Integer getErrorCode() {
        Throwable cause = getCause();
        if (cause instanceof SQLException) {
            return ((SQLException) cause).getErrorCode();
        }
        return null;
    }

    public String getSqlState() {
        Throwable cause = getCause();
        if (cause instanceof SQLException) {
            return ((SQLException) cause).getSQLState();
        }
        return null;
    }
}
