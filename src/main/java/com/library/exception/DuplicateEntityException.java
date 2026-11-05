package com.library.exception;

/**
 * Checked exception thrown when a unique constraint violation occurs (e.g. duplicate email or ISBN).
 */
public class DuplicateEntityException extends LibraryException {
    private static final long serialVersionUID = 1L;

    private String entityName;
    private String fieldName;
    private Object conflictingValue;

    public DuplicateEntityException(String message) {
        super(message);
    }

    public DuplicateEntityException(String entityName, String fieldName, Object conflictingValue) {
        super(String.format("%s with %s '%s' already exists", entityName, fieldName, conflictingValue));
        this.entityName = entityName;
        this.fieldName = fieldName;
        this.conflictingValue = conflictingValue;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getConflictingValue() {
        return conflictingValue;
    }

    public Object getFieldValue() {
        return conflictingValue;
    }
}
