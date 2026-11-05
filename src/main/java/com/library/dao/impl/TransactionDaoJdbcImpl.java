package com.library.dao.impl;

import com.library.dao.TransactionDao;
import com.library.exception.DatabaseOperationException;
import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Transaction;
import com.library.model.TransactionStatus;
import com.library.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link TransactionDao} with parameterized queries and ACID transaction support.
 */
public class TransactionDaoJdbcImpl extends BaseJdbcDao implements TransactionDao {

    @Override
    public Optional<Transaction> findById(Integer id) throws DatabaseOperationException {
        if (id == null) return Optional.empty();
        String sql = "SELECT t.*, b.title, b.author, b.isbn, u.name, u.email, u.membership_id " +
                     "FROM transactions t " +
                     "JOIN books b ON t.book_id = b.book_id " +
                     "JOIN users u ON t.user_id = u.user_id " +
                     "WHERE t.transaction_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTransaction(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find transaction by ID: " + id, e);
        }
    }

    @Override
    public List<Transaction> findAll() throws DatabaseOperationException {
        return findAllWithFilter(null);
    }

    @Override
    public Transaction save(Transaction entity) throws DatabaseOperationException {
        try (Connection conn = getConnection()) {
            return createTransaction(conn, entity);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to save transaction", e);
        }
    }

    @Override
    public boolean update(Transaction tx) throws DatabaseOperationException {
        String sql = "UPDATE transactions SET book_id = ?, user_id = ?, borrow_date = ?, " +
                     "due_date = ?, return_date = ?, status = ?, fine_amount = ? " +
                     "WHERE transaction_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tx.getBookId());
            ps.setInt(2, tx.getUserId());
            ps.setTimestamp(3, tx.getBorrowDate());
            ps.setTimestamp(4, tx.getDueDate());
            ps.setTimestamp(5, tx.getReturnDate());
            ps.setString(6, tx.getStatus().name());
            ps.setBigDecimal(7, tx.getFineAmount());
            ps.setInt(8, tx.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to update transaction: " + tx.getId(), e);
        }
    }

    @Override
    public boolean deleteById(Integer id) throws DatabaseOperationException {
        if (id == null) return false;
        String sql = "DELETE FROM transactions WHERE transaction_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to delete transaction with ID: " + id, e);
        }
    }

    @Override
    public List<Transaction> findByUserId(int userId) throws DatabaseOperationException {
        String sql = "SELECT t.*, b.title, b.author, b.isbn, u.name, u.email, u.membership_id " +
                     "FROM transactions t " +
                     "JOIN books b ON t.book_id = b.book_id " +
                     "JOIN users u ON t.user_id = u.user_id " +
                     "WHERE t.user_id = ? " +
                     "ORDER BY t.borrow_date DESC";
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTransaction(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find transactions for user: " + userId, e);
        }
    }

    @Override
    public List<Transaction> findActiveLoansByUserId(int userId) throws DatabaseOperationException {
        String sql = "SELECT t.*, b.title, b.author, b.isbn, u.name, u.email, u.membership_id " +
                     "FROM transactions t " +
                     "JOIN books b ON t.book_id = b.book_id " +
                     "JOIN users u ON t.user_id = u.user_id " +
                     "WHERE t.user_id = ? AND t.status IN ('BORROWED', 'OVERDUE') " +
                     "ORDER BY t.due_date ASC";
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTransaction(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find active loans for user: " + userId, e);
        }
    }

    @Override
    public List<Transaction> findOverdueTransactions(Timestamp asOfDate) throws DatabaseOperationException {
        String sql = "SELECT t.*, b.title, b.author, b.isbn, u.name, u.email, u.membership_id " +
                     "FROM transactions t " +
                     "JOIN books b ON t.book_id = b.book_id " +
                     "JOIN users u ON t.user_id = u.user_id " +
                     "WHERE t.status != 'RETURNED' AND t.due_date < ? " +
                     "ORDER BY t.due_date ASC";
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, asOfDate != null ? asOfDate : new Timestamp(System.currentTimeMillis()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTransaction(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find overdue transactions", e);
        }
    }

    @Override
    public List<Transaction> findAllWithFilter(String statusFilter) throws DatabaseOperationException {
        StringBuilder sb = new StringBuilder(
                "SELECT t.*, b.title, b.author, b.isbn, u.name, u.email, u.membership_id " +
                "FROM transactions t " +
                "JOIN books b ON t.book_id = b.book_id " +
                "JOIN users u ON t.user_id = u.user_id ");

        boolean hasFilter = statusFilter != null && !statusFilter.trim().isEmpty() && !statusFilter.equalsIgnoreCase("ALL");
        if (hasFilter) {
            sb.append("WHERE t.status = ? ");
        }
        sb.append("ORDER BY t.borrow_date DESC");

        List<Transaction> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sb.toString())) {
            if (hasFilter) {
                ps.setString(1, statusFilter.trim().toUpperCase());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTransaction(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to fetch filtered transactions", e);
        }
    }

    @Override
    public int updateStatusBatch(List<Integer> transactionIds, TransactionStatus newStatus) throws DatabaseOperationException {
        if (transactionIds == null || transactionIds.isEmpty()) return 0;
        String sql = "UPDATE transactions SET status = ? WHERE transaction_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Integer id : transactionIds) {
                ps.setString(1, newStatus.name());
                ps.setInt(2, id);
                ps.addBatch();
            }
            int[] results = ps.executeBatch();
            int count = 0;
            for (int r : results) {
                if (r >= 0 || r == Statement.SUCCESS_NO_INFO) {
                    count++;
                }
            }
            return count;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to batch update transaction statuses", e);
        }
    }

    @Override
    public int countActiveLoans() throws DatabaseOperationException {
        String sql = "SELECT COUNT(*) FROM transactions WHERE status IN ('BORROWED', 'OVERDUE')";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
            return 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to count active loans", e);
        }
    }

    @Override
    public int countOverdueLoans() throws DatabaseOperationException {
        String sql = "SELECT COUNT(*) FROM transactions WHERE status = 'OVERDUE' OR (status = 'BORROWED' AND due_date < CURRENT_TIMESTAMP)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
            return 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to count overdue loans", e);
        }
    }

    @Override
    public Transaction createTransaction(Connection conn, Transaction tx) throws SQLException {
        String sql = "INSERT INTO transactions (book_id, user_id, borrow_date, due_date, return_date, status, fine_amount) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Timestamp now = tx.getBorrowDate() != null ? tx.getBorrowDate() : new Timestamp(System.currentTimeMillis());
            ps.setInt(1, tx.getBookId());
            ps.setInt(2, tx.getUserId());
            ps.setTimestamp(3, now);
            ps.setTimestamp(4, tx.getDueDate());
            ps.setTimestamp(5, tx.getReturnDate());
            ps.setString(6, tx.getStatus() != null ? tx.getStatus().name() : TransactionStatus.BORROWED.name());
            ps.setBigDecimal(7, tx.getFineAmount());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Creating transaction failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    tx.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating transaction failed, no ID obtained.");
                }
            }
            tx.setBorrowDate(now);
            tx.setCreatedAt(now);
            tx.setUpdatedAt(now);
            return tx;
        }
    }

    @Override
    public boolean recordReturn(Connection conn, int transactionId, Timestamp returnDate, double fineAmount) throws SQLException {
        String sql = "UPDATE transactions SET return_date = ?, status = 'RETURNED', fine_amount = ? " +
                     "WHERE transaction_id = ? AND status != 'RETURNED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, returnDate != null ? returnDate : new Timestamp(System.currentTimeMillis()));
            ps.setDouble(2, fineAmount);
            ps.setInt(3, transactionId);
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public Optional<Transaction> findByIdForUpdate(Connection conn, int transactionId) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE transaction_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Transaction tx = new Transaction();
                    tx.setId(rs.getInt("transaction_id"));
                    tx.setBookId(rs.getInt("book_id"));
                    tx.setUserId(rs.getInt("user_id"));
                    tx.setBorrowDate(rs.getTimestamp("borrow_date"));
                    tx.setDueDate(rs.getTimestamp("due_date"));
                    tx.setReturnDate(rs.getTimestamp("return_date"));
                    tx.setStatus(TransactionStatus.fromString(rs.getString("status")));
                    tx.setFineAmount(rs.getDouble("fine_amount"));
                    return Optional.of(tx);
                }
            }
            return Optional.empty();
        }
    }

    protected Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        Transaction tx = new Transaction();
        tx.setId(rs.getInt("transaction_id"));
        tx.setBookId(rs.getInt("book_id"));
        tx.setUserId(rs.getInt("user_id"));
        tx.setBorrowDate(rs.getTimestamp("borrow_date"));
        tx.setDueDate(rs.getTimestamp("due_date"));
        tx.setReturnDate(rs.getTimestamp("return_date"));
        tx.setStatus(TransactionStatus.fromString(rs.getString("status")));
        tx.setFineAmount(rs.getDouble("fine_amount"));
        tx.setCreatedAt(rs.getTimestamp("borrow_date"));
        tx.setUpdatedAt(rs.getTimestamp("borrow_date"));

        // Map joined book info if available
        try {
            String title = rs.getString("title");
            if (title != null) {
                Book b = new Book();
                b.setId(tx.getBookId());
                b.setTitle(title);
                b.setAuthor(rs.getString("author"));
                b.setIsbn(rs.getString("isbn"));
                tx.setBook(b);
            }
        } catch (SQLException ignored) {}

        // Map joined user info if available
        try {
            String name = rs.getString("name");
            if (name != null) {
                User u = new Member();
                u.setId(tx.getUserId());
                u.setName(name);
                u.setEmail(rs.getString("email"));
                u.setMembershipId(rs.getString("membership_id"));
                tx.setUser(u);
            }
        } catch (SQLException ignored) {}

        return tx;
    }
}
