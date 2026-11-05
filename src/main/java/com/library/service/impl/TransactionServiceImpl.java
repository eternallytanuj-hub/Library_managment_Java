package com.library.service.impl;

import com.library.config.DBConnectionFactory;
import com.library.dao.BookDao;
import com.library.dao.NotificationDao;
import com.library.dao.TransactionDao;
import com.library.dao.UserDao;
import com.library.dao.impl.BookDaoJdbcImpl;
import com.library.dao.impl.NotificationDaoJdbcImpl;
import com.library.dao.impl.TransactionDaoJdbcImpl;
import com.library.dao.impl.UserDaoJdbcImpl;
import com.library.exception.BookNotFoundException;
import com.library.exception.BookUnavailableException;
import com.library.exception.DatabaseOperationException;
import com.library.exception.DuplicateEntityException;
import com.library.exception.LibraryException;
import com.library.exception.MemberNotFoundException;
import com.library.exception.UnauthorizedAccessException;
import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Notification;
import com.library.model.NotificationType;
import com.library.model.Transaction;
import com.library.model.TransactionStatus;
import com.library.model.User;
import com.library.service.TransactionService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Concrete implementation of {@link TransactionService} with ACID transaction boundaries,
 * in-memory fine-grained ReentrantLock per book, and background overdue sweep processing.
 */
public class TransactionServiceImpl implements TransactionService {

    private static final Logger LOGGER = Logger.getLogger(TransactionServiceImpl.class.getName());
    private static volatile TransactionServiceImpl instance;

    private final TransactionDao transactionDao;
    private final BookDao bookDao;
    private final UserDao userDao;
    private final NotificationDao notificationDao;

    // Layer 1 concurrency control: fine-grained lock keyed by bookId
    private final ConcurrentHashMap<Integer, ReentrantLock> bookLocks = new ConcurrentHashMap<>();

    public TransactionServiceImpl() {
        this(new TransactionDaoJdbcImpl(), new BookDaoJdbcImpl(), new UserDaoJdbcImpl(), new NotificationDaoJdbcImpl());
    }

    public TransactionServiceImpl(TransactionDao transactionDao, BookDao bookDao, UserDao userDao) {
        this(transactionDao, bookDao, userDao, new NotificationDaoJdbcImpl());
    }

    public TransactionServiceImpl(TransactionDao transactionDao, BookDao bookDao, UserDao userDao, NotificationDao notificationDao) {
        this.transactionDao = Objects.requireNonNull(transactionDao, "TransactionDao must not be null");
        this.bookDao = Objects.requireNonNull(bookDao, "BookDao must not be null");
        this.userDao = Objects.requireNonNull(userDao, "UserDao must not be null");
        this.notificationDao = notificationDao != null ? notificationDao : new NotificationDaoJdbcImpl();
    }

    public static TransactionServiceImpl getInstance() {
        if (instance == null) {
            synchronized (TransactionServiceImpl.class) {
                if (instance == null) {
                    instance = new TransactionServiceImpl();
                }
            }
        }
        return instance;
    }

    @Override
    public Transaction borrowBook(int userId, int bookId)
            throws BookUnavailableException, DuplicateEntityException, MemberNotFoundException,
                   BookNotFoundException, DatabaseOperationException, LibraryException {

        // 1. Verify Member existence and borrow limits
        Optional<User> userOpt = userDao.findById(userId);
        if (userOpt.isEmpty()) {
            throw new MemberNotFoundException(userId);
        }

        User user = userOpt.get();
        if (!(user instanceof Member)) {
            throw new LibraryException("Only members can borrow books. User role: " + user.getRole());
        }

        Member member = (Member) user;
        int activeLoanCount = userDao.countActiveLoans(userId);
        member.setActiveLoansCount(activeLoanCount);
        if (!member.canBorrow()) {
            throw new LibraryException("Member has reached maximum borrow limit of " + member.getMaxBorrowLimit() + " books.");
        }

        // 2. Verify Book existence and catalog availability
        Optional<Book> bookOpt = bookDao.findById(bookId);
        if (bookOpt.isEmpty()) {
            throw new BookNotFoundException(bookId);
        }

        Book book = bookOpt.get();
        if (book.getAvailableCopies() <= 0) {
            throw new BookUnavailableException(bookId, book.getTitle());
        }

        // 3. Thread Synchronization: Lock on Book ID
        ReentrantLock lock = bookLocks.computeIfAbsent(bookId, id -> new ReentrantLock());
        lock.lock();
        Connection conn = null;
        try {
            // Layer 2: ACID Transaction Boundary
            conn = DBConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            // Layer 3: Atomic conditional stock decrement
            boolean decremented = bookDao.decrementAvailableCopies(conn, bookId);
            if (!decremented) {
                conn.rollback();
                throw new BookUnavailableException(bookId, book.getTitle());
            }

            // Create Transaction record (14 days standard duration)
            Timestamp now = new Timestamp(System.currentTimeMillis());
            Timestamp dueDate = Timestamp.valueOf(now.toLocalDateTime().plusDays(Transaction.DEFAULT_LOAN_DAYS));
            Transaction newTx = new Transaction(bookId, userId, now, dueDate);
            Transaction savedTx = transactionDao.createTransaction(conn, newTx);

            conn.commit();

            savedTx.setBook(book);
            savedTx.setUser(member);
            return savedTx;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Rollback failed during borrow transaction", ex);
                }
            }
            throw new DatabaseOperationException("Borrow transaction failed: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    LOGGER.log(Level.WARNING, "Closing connection failed", ex);
                }
            }
            lock.unlock();
        }
    }

    @Override
    public Transaction returnBook(int transactionId, int requestingUserId, boolean isLibrarian)
            throws LibraryException, DatabaseOperationException {

        // 1. Retrieve transaction
        Optional<Transaction> txOpt = transactionDao.findById(transactionId);
        if (txOpt.isEmpty()) {
            throw new LibraryException("Transaction not found with ID: " + transactionId);
        }

        Transaction tx = txOpt.get();

        // 2. Authorization check
        if (!isLibrarian && !Objects.equals(tx.getUserId(), requestingUserId)) {
            throw new UnauthorizedAccessException(
                    "Unauthorized attempt to return book transaction belonging to another user", "LIBRARIAN");
        }

        // 3. Double-return prevention
        if (tx.isReturned() || tx.getStatus() == TransactionStatus.RETURNED) {
            throw new LibraryException("Book has already been returned");
        }

        // 4. Calculate fine
        Timestamp returnDate = new Timestamp(System.currentTimeMillis());
        tx.setReturnDate(returnDate);
        BigDecimal fine = tx.calculateFine(Transaction.DEFAULT_DAILY_FINE);

        // 5. ACID return execution
        Connection conn = null;
        try {
            conn = DBConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            boolean recorded = transactionDao.recordReturn(conn, transactionId, returnDate, fine.doubleValue());
            if (!recorded) {
                conn.rollback();
                throw new DatabaseOperationException("Failed to update transaction return record: " + transactionId);
            }

            boolean incremented = bookDao.incrementAvailableCopies(conn, tx.getBookId());
            if (!incremented) {
                conn.rollback();
                throw new DatabaseOperationException("Failed to increment book stock: " + tx.getBookId());
            }

            conn.commit();

            tx.setStatus(TransactionStatus.RETURNED);
            tx.setFineAmount(fine);
            return tx;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Rollback failed during return transaction", ex);
                }
            }
            throw new DatabaseOperationException("Return transaction failed: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    LOGGER.log(Level.WARNING, "Closing connection failed", ex);
                }
            }
        }
    }

    @Override
    public List<Transaction> getActiveLoansByUserId(int userId) throws DatabaseOperationException {
        List<Transaction> loans = transactionDao.findActiveLoansByUserId(userId);
        populateNavigationalProperties(loans);
        return loans;
    }

    @Override
    public List<Transaction> getBorrowHistoryByUserId(int userId) throws DatabaseOperationException {
        List<Transaction> history = transactionDao.findByUserId(userId);
        populateNavigationalProperties(history);
        return history;
    }

    @Override
    public List<Transaction> getAllTransactions(String statusFilter) throws DatabaseOperationException {
        List<Transaction> transactions = transactionDao.findAllWithFilter(statusFilter);
        populateNavigationalProperties(transactions);
        return transactions;
    }

    @Override
    public int processOverdueTransactions() throws DatabaseOperationException {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        List<Transaction> overdueLoans = transactionDao.findOverdueTransactions(now);
        int processedCount = 0;

        for (Transaction tx : overdueLoans) {
            if (tx.getStatus() == TransactionStatus.BORROWED) {
                transactionDao.updateStatusBatch(Collections.singletonList(tx.getId()), TransactionStatus.OVERDUE);
                tx.setStatus(TransactionStatus.OVERDUE);
                processedCount++;

                // Enqueue or insert notification alert if user has due_date_alerts enabled
                try {
                    Optional<User> uOpt = userDao.findById(tx.getUserId());
                    if (uOpt.isPresent() && uOpt.get().isDueDateAlerts()) {
                        String title = "Overdue Book Reminder";
                        String msg = "Your borrowed book (Transaction #" + tx.getId() + ") is past its due date. " +
                                "Please return it promptly to avoid further fines.";
                        Notification alert = new Notification(tx.getUserId(), title, msg, NotificationType.DUE_DATE);
                        notificationDao.save(alert);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to dispatch overdue alert for transaction " + tx.getId(), e);
                }
            }
        }

        return processedCount;
    }

    @Override
    public Optional<Transaction> getTransactionById(int transactionId) throws DatabaseOperationException {
        Optional<Transaction> txOpt = transactionDao.findById(transactionId);
        txOpt.ifPresent(tx -> populateNavigationalProperties(Collections.singletonList(tx)));
        return txOpt;
    }

    private void populateNavigationalProperties(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) return;
        for (Transaction tx : transactions) {
            if (tx.getBook() == null && tx.getBookId() != null) {
                try {
                    bookDao.findById(tx.getBookId()).ifPresent(tx::setBook);
                } catch (DatabaseOperationException ignored) {}
            }
            if (tx.getUser() == null && tx.getUserId() != null) {
                try {
                    userDao.findById(tx.getUserId()).ifPresent(tx::setUser);
                } catch (DatabaseOperationException ignored) {}
            }
        }
    }
}
