package com.library.dao.impl;

import com.library.config.DBConnectionFactory;
import com.library.exception.DatabaseOperationException;
import com.library.exception.DuplicateEntityException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Base abstract class for JDBC DAO implementations providing common connection retrieval,
 * parameter binding, exception translation, and transaction management.
 */
public abstract class BaseJdbcDao {

    protected Connection getConnection() throws SQLException {
        return DBConnectionFactory.getConnection();
    }

    protected void setParameters(PreparedStatement ps, Object... params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
        }
    }

    protected void close(AutoCloseable... resources) {
        for (AutoCloseable res : resources) {
            if (res != null) {
                try {
                    res.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Translates standard SQL exceptions into domain exceptions.
     * Handles SQLSTATE 23505 (H2 unique constraint) and 1062 (MySQL duplicate entry).
     */
    protected DatabaseOperationException translateException(String operation, SQLException e) throws DuplicateEntityException {
        String sqlState = e.getSQLState();
        int errorCode = e.getErrorCode();

        // 23505 is standard ANSI/H2 unique violation; 1062 is MySQL ER_DUP_ENTRY
        if ("23505".equals(sqlState) || errorCode == 1062) {
            throw new DuplicateEntityException("Unique constraint violation during " + operation + ": " + e.getMessage());
        }
        return new DatabaseOperationException("Failed to " + operation + ": " + e.getMessage(), e);
    }

    @FunctionalInterface
    public interface TransactionAction<R> {
        R execute(Connection conn) throws Exception;
    }

    public <R> R executeInTransaction(TransactionAction<R> action) throws Exception {
        Connection conn = null;
        boolean originalAutoCommit = true;
        try {
            conn = getConnection();
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            R result = action.execute(conn);

            conn.commit();
            return result;
        } catch (Exception ex) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    ex.addSuppressed(rollbackEx);
                }
            }
            throw ex;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (SQLException ignored) {}
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }
}
