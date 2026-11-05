package com.library.exception;

/**
 * Checked exception thrown when business validation fails.
 */
public class ValidationException extends LibraryException {
    private static final long serialVersionUID = 1L;

    private String fieldName;

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String fieldName, String message) {
        super("Validation failure on field '" + fieldName + "': " + message);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
