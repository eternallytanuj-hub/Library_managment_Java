package com.library.exception;

/**
 * Checked exception thrown when a requested book cannot be located in the catalog.
 */
public class BookNotFoundException extends EntityNotFoundException {
    private static final long serialVersionUID = 1L;

    private int bookId;
    private String isbn;

    public BookNotFoundException() {
        super("Book not found");
    }

    public BookNotFoundException(String message) {
        super(message);
    }

    public BookNotFoundException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }

    public BookNotFoundException(int bookId) {
        super("Book not found with ID: " + bookId);
        this.bookId = bookId;
    }

    public BookNotFoundException(String isbn, boolean isIsbn) {
        super("Book not found with ISBN: " + isbn);
        this.isbn = isbn;
    }

    public int getBookId() {
        return bookId;
    }

    public String getIsbn() {
        return isbn;
    }
}
