package com.library.service.impl;

import com.library.dao.ReportDao;
import com.library.dao.dto.ReportSummaryDTO;
import com.library.dao.impl.ReportDaoJdbcImpl;
import com.library.exception.DatabaseOperationException;
import com.library.service.ReportService;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Concrete implementation of {@link ReportService} aggregating inventory,
 * borrowing trends, genre breakdowns, and executive KPI summaries.
 */
public class ReportServiceImpl implements ReportService {

    private final ReportDao reportDao;

    public ReportServiceImpl() {
        this(new ReportDaoJdbcImpl());
    }

    public ReportServiceImpl(ReportDao reportDao) {
        this.reportDao = Objects.requireNonNull(reportDao, "ReportDao must not be null");
    }

    @Override
    public Map<String, Integer> getSummaryMetrics() throws DatabaseOperationException {
        return reportDao.getSummaryMetrics();
    }

    @Override
    public ReportSummaryDTO getReportSummary() throws DatabaseOperationException {
        Map<String, Integer> metrics = reportDao.getSummaryMetrics();
        return new ReportSummaryDTO(
                metrics.getOrDefault("totalBooks", 0),
                metrics.getOrDefault("totalCopies", 0),
                metrics.getOrDefault("availableCopies", 0),
                metrics.getOrDefault("borrowedCopies", 0),
                metrics.getOrDefault("overdueLoans", 0),
                metrics.getOrDefault("totalMembers", 0)
        );
    }

    @Override
    public Map<String, Integer> getMonthlyBorrowTrends() throws DatabaseOperationException {
        return reportDao.getMonthlyBorrowTrends();
    }

    @Override
    public Map<String, Integer> getGenreDistribution() throws DatabaseOperationException {
        return reportDao.getGenreDistribution();
    }

    @Override
    public Map<String, Integer> getAvailabilityRatio() throws DatabaseOperationException {
        return reportDao.getAvailabilityRatio();
    }

    @Override
    public List<Map<String, Object>> getTopBorrowedBooks(int limit) throws DatabaseOperationException {
        int safeLimit = limit > 0 ? limit : 5;
        return reportDao.getTopBorrowedBooks(safeLimit);
    }
}
