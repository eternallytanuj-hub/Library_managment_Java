package com.library.model.dto;

/**
 * Monthly loan trend data point for Chart.js line and bar graphs.
 */
public class MonthlyTrendDTO extends com.library.dao.dto.MonthlyTrendDTO {
    private static final long serialVersionUID = 1L;

    public MonthlyTrendDTO() {
        super();
    }

    public MonthlyTrendDTO(String monthLabel, int borrowCount, int returnCount) {
        super(monthLabel, borrowCount, returnCount);
    }
}
