package com.library.dao;

import com.library.model.Transaction;
import com.library.model.TransactionStatus;
import com.library.exception.DatabaseOperationException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for {@link Transaction} loan ledger entities.
 */
public interface TransactionDao extends GenericDao<Transaction, Integer> {

    List<Transaction> findByUserId(int userId) throws DatabaseOperationException;

    List<Transaction> findActiveLoansByUserId(int userId) throws DatabaseOperationException;

    List<Transaction> findOverdueTransactions(Timestamp asOfDate) throws DatabaseOperationException;

    List<Transaction> findAllWithFilter(String statusFilter) throws DatabaseOperationException;

    int updateStatusBatch(List<Integer> transactionIds, TransactionStatus newStatus) throws DatabaseOperationException;

    int countActiveLoans() throws DatabaseOperationException;

    int countOverdueLoans() throws DatabaseOperationException;

    Transaction createTransaction(Connection conn, Transaction tx) throws SQLException;

    boolean recordReturn(Connection conn, int transactionId, Timestamp returnDate, double fineAmount) throws SQLException;

    Optional<Transaction> findByIdForUpdate(Connection conn, int transactionId) throws SQLException;
}
