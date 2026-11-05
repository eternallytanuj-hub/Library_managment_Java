package com.library.service;

import com.library.dao.dto.ReportSummaryDTO;
import com.library.exception.DatabaseOperationException;

import java.util.List;
import java.util.Map;

/**
 * Service contract for library analytics, KPI metric summaries,
 * and trend reporting data for visual dashboards.
 */
public interface ReportService {

    Map<String, Integer> getSummaryMetrics() throws DatabaseOperationException;

    ReportSummaryDTO getReportSummary() throws DatabaseOperationException;

    Map<String, Integer> getMonthlyBorrowTrends() throws DatabaseOperationException;

    Map<String, Integer> getGenreDistribution() throws DatabaseOperationException;

    Map<String, Integer> getAvailabilityRatio() throws DatabaseOperationException;

    List<Map<String, Object>> getTopBorrowedBooks(int limit) throws DatabaseOperationException;
}
