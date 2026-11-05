package com.library.dao.dto;

import java.io.Serializable;

/**
 * Summary KPI DTO for Librarian executive overview dashboard cards.
 */
public class ReportSummaryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int totalBooks;
    private int totalCopies;
    private int availableCopies;
    private int borrowedCopies;
    private int overdueLoans;
    private int totalMembers;

    public ReportSummaryDTO() {}

    public ReportSummaryDTO(int totalBooks, int totalCopies, int availableCopies, int borrowedCopies, int overdueLoans, int totalMembers) {
        this.totalBooks = totalBooks;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
        this.borrowedCopies = borrowedCopies;
        this.overdueLoans = overdueLoans;
        this.totalMembers = totalMembers;
    }

    public int getTotalBooks() { return totalBooks; }
    public void setTotalBooks(int totalBooks) { this.totalBooks = totalBooks; }

    public int getTotalCopies() { return totalCopies; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }

    public int getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }

    public int getBorrowedCopies() { return borrowedCopies; }
    public void setBorrowedCopies(int borrowedCopies) { this.borrowedCopies = borrowedCopies; }

    public int getOverdueLoans() { return overdueLoans; }
    public void setOverdueLoans(int overdueLoans) { this.overdueLoans = overdueLoans; }

    public int getTotalMembers() { return totalMembers; }
    public void setTotalMembers(int totalMembers) { this.totalMembers = totalMembers; }

    /**
     * Helper computing inventory availability percentage.
     */
    public double getAvailabilityPercentage() {
        if (totalCopies <= 0) return 0.0;
        return ((double) availableCopies / totalCopies) * 100.0;
    }
}
