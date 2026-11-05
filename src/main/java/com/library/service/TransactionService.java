package com.library.service;

import com.library.exception.BookNotFoundException;
import com.library.exception.BookUnavailableException;
import com.library.exception.DatabaseOperationException;
import com.library.exception.DuplicateEntityException;
import com.library.exception.LibraryException;
import com.library.exception.MemberNotFoundException;
import com.library.model.Transaction;

import java.util.List;
import java.util.Optional;

/**
 * Service contract for book loan transactions, ACID-compliant borrow/return operations,
 * thread-safe inventory synchronization, and overdue sweep processing.
 */
public interface TransactionService {

    Transaction borrowBook(int userId, int bookId)
            throws BookUnavailableException, DuplicateEntityException, MemberNotFoundException,
                   BookNotFoundException, DatabaseOperationException, LibraryException;

    Transaction returnBook(int transactionId, int userId, boolean isLibrarian)
            throws LibraryException, DatabaseOperationException;

    List<Transaction> getActiveLoansByUserId(int userId) throws DatabaseOperationException;

    List<Transaction> getBorrowHistoryByUserId(int userId) throws DatabaseOperationException;

    List<Transaction> getAllTransactions(String statusFilter) throws DatabaseOperationException;

    int processOverdueTransactions() throws DatabaseOperationException;

    Optional<Transaction> getTransactionById(int transactionId) throws DatabaseOperationException;
}
