package com.library.model.dto;

/**
 * Genre distribution breakdown for Chart.js doughnut chart.
 */
public class GenreDistributionDTO extends com.library.dao.dto.GenreDistributionDTO {
    private static final long serialVersionUID = 1L;

    public GenreDistributionDTO() {
        super();
    }

    public GenreDistributionDTO(String genre, int bookCount, int totalCopies, double percentage) {
        super(genre, bookCount, totalCopies, percentage);
    }
}
