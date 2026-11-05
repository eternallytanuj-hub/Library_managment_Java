package com.library.dao.impl;

import com.library.dao.ReportDao;
import com.library.exception.DatabaseOperationException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC implementation of {@link ReportDao} powering librarian analytics and KPI dashboards.
 */
public class ReportDaoJdbcImpl extends BaseJdbcDao implements ReportDao {

    @Override
    public Map<String, Integer> getSummaryMetrics() throws DatabaseOperationException {
        Map<String, Integer> metrics = new HashMap<>();
        String sql = "SELECT " +
                     "  (SELECT COUNT(*) FROM books) AS total_books, " +
                     "  (SELECT COALESCE(SUM(total_copies), 0) FROM books) AS total_copies, " +
                     "  (SELECT COALESCE(SUM(available_copies), 0) FROM books) AS available_copies, " +
                     "  (SELECT COUNT(*) FROM transactions WHERE status IN ('BORROWED', 'OVERDUE')) AS active_loans, " +
                     "  (SELECT COUNT(*) FROM transactions WHERE status = 'OVERDUE' OR (status = 'BORROWED' AND due_date < CURRENT_TIMESTAMP)) AS overdue_loans, " +
                     "  (SELECT COUNT(*) FROM users WHERE role = 'MEMBER') AS total_members";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int totalBooks = rs.getInt("total_books");
                int totalCopies = rs.getInt("total_copies");
                int availableCopies = rs.getInt("available_copies");
                int activeLoans = rs.getInt("active_loans");
                int overdueLoans = rs.getInt("overdue_loans");
                int totalMembers = rs.getInt("total_members");

                metrics.put("totalBooks", totalBooks);
                metrics.put("totalCopies", totalCopies);
                metrics.put("availableCopies", availableCopies);
                metrics.put("borrowedCopies", Math.max(0, totalCopies - availableCopies));
                metrics.put("activeLoans", activeLoans);
                metrics.put("overdueLoans", overdueLoans);
                metrics.put("totalMembers", totalMembers);
            }
            return metrics;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to calculate summary metrics", e);
        }
    }

    @Override
    public Map<String, Integer> getMonthlyBorrowTrends() throws DatabaseOperationException {
        Map<String, Integer> trends = new LinkedHashMap<>();
        String sql = "SELECT SUBSTRING(CAST(borrow_date AS CHAR), 1, 7) AS month_str, COUNT(*) AS loan_count " +
                     "FROM transactions " +
                     "GROUP BY month_str " +
                     "ORDER BY month_str ASC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                trends.put(rs.getString("month_str"), rs.getInt("loan_count"));
            }
            return trends;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to generate monthly borrow trends", e);
        }
    }

    @Override
    public Map<String, Integer> getGenreDistribution() throws DatabaseOperationException {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        String sql = "SELECT genre, COUNT(*) AS book_count FROM books GROUP BY genre ORDER BY book_count DESC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                distribution.put(rs.getString("genre"), rs.getInt("book_count"));
            }
            return distribution;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to compute genre distribution", e);
        }
    }

    @Override
    public Map<String, Integer> getAvailabilityRatio() throws DatabaseOperationException {
        Map<String, Integer> ratio = new LinkedHashMap<>();
        String sql = "SELECT COALESCE(SUM(available_copies), 0) AS total_avail, " +
                     "COALESCE(SUM(total_copies - available_copies), 0) AS total_borrowed FROM books";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                ratio.put("Available Copies", rs.getInt("total_avail"));
                ratio.put("Borrowed Copies", rs.getInt("total_borrowed"));
            } else {
                ratio.put("Available Copies", 0);
                ratio.put("Borrowed Copies", 0);
            }
            return ratio;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to calculate availability ratio", e);
        }
    }

    @Override
    public List<Map<String, Object>> getTopBorrowedBooks(int limit) throws DatabaseOperationException {
        List<Map<String, Object>> topBooks = new ArrayList<>();
        String sql = "SELECT b.book_id, b.title, b.author, b.genre, COUNT(t.transaction_id) AS borrow_count " +
                     "FROM transactions t " +
                     "JOIN books b ON t.book_id = b.book_id " +
                     "GROUP BY b.book_id, b.title, b.author, b.genre " +
                     "ORDER BY borrow_count DESC, b.title ASC " +
                     "LIMIT ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 5);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("bookId", rs.getInt("book_id"));
                    map.put("title", rs.getString("title"));
                    map.put("author", rs.getString("author"));
                    map.put("genre", rs.getString("genre"));
                    map.put("borrowCount", rs.getInt("borrow_count"));
                    topBooks.add(map);
                }
            }
            return topBooks;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to fetch top borrowed books", e);
        }
    }
}
