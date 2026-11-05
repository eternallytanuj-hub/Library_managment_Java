package com.library.dao.dto;

import java.io.Serializable;

/**
 * Monthly loan trend data point for Chart.js line and bar graphs.
 */
public class MonthlyTrendDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String monthLabel; // e.g. "Jun 2026", "2026-06"
    private int borrowCount;
    private int returnCount;

    public MonthlyTrendDTO() {}

    public MonthlyTrendDTO(String monthLabel, int borrowCount, int returnCount) {
        this.monthLabel = monthLabel;
        this.borrowCount = borrowCount;
        this.returnCount = returnCount;
    }

    public String getMonthLabel() { return monthLabel; }
    public void setMonthLabel(String monthLabel) { this.monthLabel = monthLabel; }

    public int getBorrowCount() { return borrowCount; }
    public void setBorrowCount(int borrowCount) { this.borrowCount = borrowCount; }

    public int getReturnCount() { return returnCount; }
    public void setReturnCount(int returnCount) { this.returnCount = returnCount; }
}
