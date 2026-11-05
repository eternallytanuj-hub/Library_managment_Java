package com.library.exception;

/**
 * Base checked exception for entity-not-found situations.
 */
public class EntityNotFoundException extends LibraryException {
    private static final long serialVersionUID = 1L;

    public EntityNotFoundException(String message) {
        super(message);
    }
}
