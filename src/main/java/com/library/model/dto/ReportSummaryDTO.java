package com.library.model.dto;

/**
 * Summary KPI DTO for Librarian executive overview dashboard cards.
 */
public class ReportSummaryDTO extends com.library.dao.dto.ReportSummaryDTO {
    private static final long serialVersionUID = 1L;

    public ReportSummaryDTO() {
        super();
    }

    public ReportSummaryDTO(int totalBooks, int totalCopies, int availableCopies, int borrowedCopies, int overdueLoans, int totalMembers) {
        super(totalBooks, totalCopies, availableCopies, borrowedCopies, overdueLoans, totalMembers);
    }
}
