package com.library.dao;

import com.library.exception.DatabaseOperationException;

import java.util.List;
import java.util.Map;

/**
 * Data Access Object interface for aggregate analytics and inventory reporting.
 */
public interface ReportDao {

    Map<String, Integer> getSummaryMetrics() throws DatabaseOperationException;

    Map<String, Integer> getMonthlyBorrowTrends() throws DatabaseOperationException;

    Map<String, Integer> getGenreDistribution() throws DatabaseOperationException;

    Map<String, Integer> getAvailabilityRatio() throws DatabaseOperationException;

    List<Map<String, Object>> getTopBorrowedBooks(int limit) throws DatabaseOperationException;
}
