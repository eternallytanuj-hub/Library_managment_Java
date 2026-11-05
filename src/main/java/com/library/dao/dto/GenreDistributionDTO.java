package com.library.dao.dto;

import java.io.Serializable;

/**
 * Genre distribution breakdown for Chart.js doughnut chart.
 */
public class GenreDistributionDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String genre;
    private int bookCount;
    private int totalCopies;
    private double percentage;

    public GenreDistributionDTO() {}

    public GenreDistributionDTO(String genre, int bookCount, int totalCopies, double percentage) {
        this.genre = genre;
        this.bookCount = bookCount;
        this.totalCopies = totalCopies;
        this.percentage = percentage;
    }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public int getBookCount() { return bookCount; }
    public void setBookCount(int bookCount) { this.bookCount = bookCount; }

    public int getTotalCopies() { return totalCopies; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }

    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }
}
