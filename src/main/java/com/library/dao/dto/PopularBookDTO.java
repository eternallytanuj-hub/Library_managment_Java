package com.library.dao.dto;

import java.io.Serializable;

/**
 * Popular book leaderboard ranking DTO.
 */
public class PopularBookDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int bookId;
    private String title;
    private String author;
    private String genre;
    private int borrowCount;

    public PopularBookDTO() {}

    public PopularBookDTO(int bookId, String title, String author, String genre, int borrowCount) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.borrowCount = borrowCount;
    }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public int getBorrowCount() { return borrowCount; }
    public void setBorrowCount(int borrowCount) { this.borrowCount = borrowCount; }
}
