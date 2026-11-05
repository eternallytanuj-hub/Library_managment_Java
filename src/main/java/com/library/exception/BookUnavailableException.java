package com.library.exception;

/**
 * Checked exception thrown when a member attempts to borrow a book with 0 available copies.
 */
public class BookUnavailableException extends LibraryException {
    private static final long serialVersionUID = 1L;

    private int bookId;
    private String title;

    public BookUnavailableException() {
        super("Book is unavailable");
    }

    public BookUnavailableException(String message) {
        super(message);
    }

    public BookUnavailableException(int bookId) {
        super("Book with ID " + bookId + " has 0 available copies and cannot be borrowed.");
        this.bookId = bookId;
    }

    public BookUnavailableException(int bookId, String title) {
        super("Book '" + title + "' (ID: " + bookId + ") has 0 available copies and cannot be borrowed.");
        this.bookId = bookId;
        this.title = title;
    }

    public int getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }
}
