package com.library.exception;

/**
 * Thrown when a user/member account cannot be found by ID, email, or membership ID.
 */
public class MemberNotFoundException extends LibraryException {
    private static final long serialVersionUID = 1L;
    private Integer memberId;
    private String identifier;

    public MemberNotFoundException(int memberId) {
        super("Member with ID " + memberId + " was not found.");
        this.memberId = memberId;
    }

    public MemberNotFoundException(String identifier) {
        super("Member with identifier '" + identifier + "' was not found.");
        this.identifier = identifier;
    }

    public MemberNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public Integer getMemberId() {
        return memberId;
    }

    public String getIdentifier() {
        return identifier;
    }
}
