package com.library.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Concrete domain model representing a book in the library catalog.
 */
public class Book extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private String isbn;
    private String title;
    private String author;
    private String genre;
    private int totalCopies;
    private int availableCopies;

    public Book() {
        super();
    }

    public Book(String isbn, String title, String author, String genre, int totalCopies) {
        this(null, isbn, title, author, genre, totalCopies, totalCopies);
    }

    public Book(String isbn, String title, String author, String genre, int totalCopies, int availableCopies) {
        this(null, isbn, title, author, genre, totalCopies, availableCopies);
    }

    public Book(Integer id, String isbn, String title, String author, String genre, int totalCopies, int availableCopies) {
        super(id, new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()));
        validateCopyInvariants(totalCopies, availableCopies);
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    public Book(Integer id, String isbn, String title, String author, String genre, int totalCopies, int availableCopies,
                Timestamp createdAt, Timestamp updatedAt) {
        super(id, createdAt, updatedAt);
        validateCopyInvariants(totalCopies, availableCopies);
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    public Book(Integer id, String isbn, String title, String author, String genre, int totalCopies, int availableCopies,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, createdAt, updatedAt);
        validateCopyInvariants(totalCopies, availableCopies);
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    /**
     * Enforces domain invariant: 0 <= availableCopies <= totalCopies.
     */
    public static void validateCopyInvariants(int total, int available) {
        if (total < 0) {
            throw new IllegalArgumentException("Total copies cannot be negative: " + total);
        }
        if (available < 0) {
            throw new IllegalArgumentException("Available copies cannot be negative: " + available);
        }
        if (available > total) {
            throw new IllegalArgumentException("Available copies (" + available + 
                    ") cannot exceed total copies (" + total + ")");
        }
    }

    // Business Methods
    public boolean isAvailable() {
        return this.availableCopies > 0;
    }

    public boolean hasCopies(int n) {
        return n > 0 && this.availableCopies >= n;
    }

    public int getBorrowedCopies() {
        return totalCopies - availableCopies;
    }

    public void borrowCopy() {
        if (!isAvailable()) {
            throw new IllegalStateException("Cannot borrow book: no copies available for " + title);
        }
        this.availableCopies--;
    }

    public void returnCopy() {
        if (this.availableCopies >= this.totalCopies) {
            throw new IllegalStateException("Cannot return copy: available copies already equal total copies (" + totalCopies + ")");
        }
        this.availableCopies++;
    }

    // Getters and Setters with Validation
    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(int totalCopies) {
        if (totalCopies < 0) {
            throw new IllegalArgumentException("Total copies cannot be negative: " + totalCopies);
        }
        int borrowed = getBorrowedCopies();
        if (totalCopies < borrowed) {
            throw new IllegalArgumentException("Total copies (" + totalCopies + 
                    ") cannot be less than currently borrowed copies (" + borrowed + ")");
        }
        if (totalCopies < this.availableCopies) {
            throw new IllegalArgumentException("Total copies (" + totalCopies + 
                    ") cannot be less than available copies (" + this.availableCopies + ")");
        }
        this.totalCopies = totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(int availableCopies) {
        validateCopyInvariants(this.totalCopies, availableCopies);
        this.availableCopies = availableCopies;
    }

    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", isbn='" + isbn + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", genre='" + genre + '\'' +
                ", totalCopies=" + totalCopies +
                ", availableCopies=" + availableCopies +
                '}';
    }
}
