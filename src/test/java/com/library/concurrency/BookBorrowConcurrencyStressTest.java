package com.library.concurrency;

import com.library.config.AppConfig;
import com.library.config.DBConnectionFactory;
import com.library.dao.BookDao;
import com.library.dao.TransactionDao;
import com.library.dao.UserDao;
import com.library.dao.impl.BookDaoJdbcImpl;
import com.library.dao.impl.TransactionDaoJdbcImpl;
import com.library.dao.impl.UserDaoJdbcImpl;
import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Transaction;
import com.library.model.TransactionStatus;
import com.library.model.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Empirical Stress and Concurrency Verification Test Suite.
 *
 * Verifies Milestone 1 concurrency defenses under intense thread contention:
 * 1. 20-50 concurrent worker threads competing to borrow the last remaining copy (available_copies = 1).
 * 2. Strict empirical validation that exactly 1 thread succeeds in acquiring the last copy.
 * 3. Strict empirical validation that all remaining threads fail safely without unhandled exceptions.
 * 4. Strict empirical validation that available_copies never drops below 0 (zero negative inventory).
 * 5. Database-level CHECK constraint validation preventing negative stock even on direct updates.
 * 6. Interleaved concurrent borrow and return operations preserving total inventory invariants.
 */
@DisplayName("Milestone 1 Concurrency & Stress Verification Tests")
public class BookBorrowConcurrencyStressTest {

    private BookDao bookDao;
    private TransactionDao transactionDao;
    private UserDao userDao;

    @BeforeAll
    static void initTestSuite() {
        AppConfig.loadProperties();
    }

    @BeforeEach
    void resetDatabaseState() throws Exception {
        bookDao = new BookDaoJdbcImpl();
        transactionDao = new TransactionDaoJdbcImpl();
        userDao = new UserDaoJdbcImpl();

        try (Connection conn = DBConnectionFactory.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");
            stmt.execute("TRUNCATE TABLE search_history");
            stmt.execute("TRUNCATE TABLE notifications");
            stmt.execute("TRUNCATE TABLE transactions");
            stmt.execute("TRUNCATE TABLE books");
            stmt.execute("TRUNCATE TABLE users");
            stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }

    private List<User> createTestMembers(int count, String prefix) throws Exception {
        List<User> members = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            Member m = new Member();
            m.setMembershipId(prefix + "-MEM-" + i);
            m.setName(prefix + " User " + i);
            m.setEmail(prefix.toLowerCase() + ".user" + i + "@librarytest.com");
            m.setPasswordHash("hashed_pwd_" + i);
            members.add(userDao.save(m));
        }
        return members;
    }

    @Test
    @DisplayName("Challenge 1: 20 concurrent threads racing for the LAST available copy (available_copies = 1)")
    void testConcurrentBorrow_20Threads_SingleAvailableCopy() throws Exception {
        final int threadCount = 20;
        final Book book = bookDao.save(new Book("STRESS-20-ISBN", "Concurrent Contention Book 20", "Author X", "Tech", 1, 1));
        final int bookId = book.getId();

        final List<User> members = createTestMembers(threadCount, "TH20");
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);

        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger exhaustedCount = new AtomicInteger(0);
        final List<Throwable> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final User member = members.get(i);
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize all 20 threads to start at the exact same instant

                    Connection conn = null;
                    try {
                        conn = DBConnectionFactory.getConnection();
                        conn.setAutoCommit(false);

                        boolean secured = bookDao.decrementAvailableCopies(conn, bookId);
                        if (secured) {
                            Transaction tx = new Transaction(bookId, member.getId(),
                                    new Timestamp(System.currentTimeMillis()),
                                    new Timestamp(System.currentTimeMillis() + 86400000L * 14),
                                    null, TransactionStatus.BORROWED, 0.0);
                            transactionDao.createTransaction(conn, tx);
                            conn.commit();
                            successCount.incrementAndGet();
                        } else {
                            conn.rollback();
                            exhaustedCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        if (conn != null) {
                            try {
                                conn.rollback();
                            } catch (SQLException ignored) {}
                        }
                        unexpectedErrors.add(e);
                    } finally {
                        if (conn != null) {
                            try {
                                conn.setAutoCommit(true);
                                conn.close();
                            } catch (SQLException ignored) {}
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    unexpectedErrors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Unleash all 20 threads simultaneously
        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All 20 threads must finish within the 10-second timeout window");
        assertTrue(unexpectedErrors.isEmpty(), "Zero unexpected exceptions allowed: " + unexpectedErrors);
        assertEquals(1, successCount.get(), "Exactly 1 thread must win the race to borrow the single copy");
        assertEquals(19, exhaustedCount.get(), "All other 19 threads must fail safely with zero unhandled exceptions");

        // Verify database inventory state
        Book verifiedBook = bookDao.findById(bookId).orElseThrow();
        assertEquals(0, verifiedBook.getAvailableCopies(), "Available copies must be exactly 0, NEVER negative");
        assertEquals(1, verifiedBook.getTotalCopies(), "Total copies must remain intact");

        // Verify recorded transactions
        assertEquals(1, transactionDao.countActiveLoans(), "Exactly 1 active loan transaction must be persisted");
    }

    @Test
    @DisplayName("Challenge 2: 50 concurrent threads racing for the LAST available copy (available_copies = 1)")
    void testConcurrentBorrow_50Threads_SingleAvailableCopy() throws Exception {
        final int threadCount = 50;
        final Book book = bookDao.save(new Book("STRESS-50-ISBN", "Concurrent Contention Book 50", "Author Y", "Tech", 1, 1));
        final int bookId = book.getId();

        final List<User> members = createTestMembers(threadCount, "TH50");
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);

        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger exhaustedCount = new AtomicInteger(0);
        final List<Throwable> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final User member = members.get(i);
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize all 50 threads to strike concurrently

                    Connection conn = null;
                    try {
                        conn = DBConnectionFactory.getConnection();
                        conn.setAutoCommit(false);

                        boolean secured = bookDao.decrementAvailableCopies(conn, bookId);
                        if (secured) {
                            Transaction tx = new Transaction(bookId, member.getId(),
                                    new Timestamp(System.currentTimeMillis()),
                                    new Timestamp(System.currentTimeMillis() + 86400000L * 14),
                                    null, TransactionStatus.BORROWED, 0.0);
                            transactionDao.createTransaction(conn, tx);
                            conn.commit();
                            successCount.incrementAndGet();
                        } else {
                            conn.rollback();
                            exhaustedCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        if (conn != null) {
                            try {
                                conn.rollback();
                            } catch (SQLException ignored) {}
                        }
                        unexpectedErrors.add(e);
                    } finally {
                        if (conn != null) {
                            try {
                                conn.setAutoCommit(true);
                                conn.close();
                            } catch (SQLException ignored) {}
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    unexpectedErrors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Unleash all 50 threads simultaneously
        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All 50 threads must finish within the 15-second timeout window");
        assertTrue(unexpectedErrors.isEmpty(), "Zero unexpected exceptions allowed: " + unexpectedErrors);
        assertEquals(1, successCount.get(), "Under 50-thread high contention, exactly 1 thread must succeed");
        assertEquals(49, exhaustedCount.get(), "The other 49 threads must be safely rejected due to exhausted inventory");

        // Verify database inventory state
        Book verifiedBook = bookDao.findById(bookId).orElseThrow();
        assertEquals(0, verifiedBook.getAvailableCopies(), "Available copies must remain 0 and never drop below 0");
        assertEquals(1, verifiedBook.getTotalCopies(), "Total copies must remain intact");

        // Verify recorded transactions
        assertEquals(1, transactionDao.countActiveLoans(), "Exactly 1 loan must be persisted in ledger");
    }

    @Test
    @DisplayName("Challenge 3: 30 concurrent threads attempting to borrow a book with ZERO available copies")
    void testConcurrentBorrow_ZeroAvailableCopies_AllRejected() throws Exception {
        final int threadCount = 30;
        final Book book = bookDao.save(new Book("STRESS-ZERO-ISBN", "Sold Out Book", "Author Z", "Fiction", 5, 0));
        final int bookId = book.getId();

        final List<User> members = createTestMembers(threadCount, "THZERO");
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);

        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger exhaustedCount = new AtomicInteger(0);
        final List<Throwable> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final User member = members.get(i);
            executor.submit(() -> {
                try {
                    startLatch.await();

                    Connection conn = null;
                    try {
                        conn = DBConnectionFactory.getConnection();
                        conn.setAutoCommit(false);

                        boolean secured = bookDao.decrementAvailableCopies(conn, bookId);
                        if (secured) {
                            Transaction tx = new Transaction(bookId, member.getId(),
                                    new Timestamp(System.currentTimeMillis()),
                                    new Timestamp(System.currentTimeMillis() + 86400000L * 14),
                                    null, TransactionStatus.BORROWED, 0.0);
                            transactionDao.createTransaction(conn, tx);
                            conn.commit();
                            successCount.incrementAndGet();
                        } else {
                            conn.rollback();
                            exhaustedCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        if (conn != null) {
                            try {
                                conn.rollback();
                            } catch (SQLException ignored) {}
                        }
                        unexpectedErrors.add(e);
                    } finally {
                        if (conn != null) {
                            try {
                                conn.setAutoCommit(true);
                                conn.close();
                            } catch (SQLException ignored) {}
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    unexpectedErrors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All 30 threads must finish within timeout");
        assertTrue(unexpectedErrors.isEmpty(), "Zero unexpected errors: " + unexpectedErrors);
        assertEquals(0, successCount.get(), "Zero threads must succeed when available_copies = 0");
        assertEquals(30, exhaustedCount.get(), "All 30 threads must be safely rejected");

        Book verifiedBook = bookDao.findById(bookId).orElseThrow();
        assertEquals(0, verifiedBook.getAvailableCopies(), "Available copies must stay 0");
        assertEquals(0, transactionDao.countActiveLoans(), "No active loans created");
    }

    @Test
    @DisplayName("Challenge 4: 50 concurrent threads competing for 5 available copies")
    void testConcurrentBorrow_50Threads_5AvailableCopies() throws Exception {
        final int threadCount = 50;
        final int totalCopies = 5;
        final Book book = bookDao.save(new Book("STRESS-5-ISBN", "Batch 5 Book", "Author W", "Drama", totalCopies, totalCopies));
        final int bookId = book.getId();

        final List<User> members = createTestMembers(threadCount, "TH5COPY");
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);

        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger exhaustedCount = new AtomicInteger(0);
        final List<Throwable> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final User member = members.get(i);
            executor.submit(() -> {
                try {
                    startLatch.await();

                    Connection conn = null;
                    try {
                        conn = DBConnectionFactory.getConnection();
                        conn.setAutoCommit(false);

                        boolean secured = bookDao.decrementAvailableCopies(conn, bookId);
                        if (secured) {
                            Transaction tx = new Transaction(bookId, member.getId(),
                                    new Timestamp(System.currentTimeMillis()),
                                    new Timestamp(System.currentTimeMillis() + 86400000L * 14),
                                    null, TransactionStatus.BORROWED, 0.0);
                            transactionDao.createTransaction(conn, tx);
                            conn.commit();
                            successCount.incrementAndGet();
                        } else {
                            conn.rollback();
                            exhaustedCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        if (conn != null) {
                            try {
                                conn.rollback();
                            } catch (SQLException ignored) {}
                        }
                        unexpectedErrors.add(e);
                    } finally {
                        if (conn != null) {
                            try {
                                conn.setAutoCommit(true);
                                conn.close();
                            } catch (SQLException ignored) {}
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    unexpectedErrors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All 50 threads must complete within timeout");
        assertTrue(unexpectedErrors.isEmpty(), "Zero unexpected errors: " + unexpectedErrors);
        assertEquals(5, successCount.get(), "Exactly 5 threads must succeed in borrowing the 5 copies");
        assertEquals(45, exhaustedCount.get(), "Remaining 45 threads must receive safe rejection");

        Book verifiedBook = bookDao.findById(bookId).orElseThrow();
        assertEquals(0, verifiedBook.getAvailableCopies(), "Available copies must reach exactly 0, never negative");
        assertEquals(5, transactionDao.countActiveLoans(), "Exactly 5 active loans in transaction table");
    }

    @Test
    @DisplayName("Challenge 5: Database CHECK constraint chk_books_available_copies rejects negative stock at DDL layer")
    void testDatabaseCheckConstraint_HardRejectionOnNegativeStock() throws Exception {
        Book book = bookDao.save(new Book("CHECK-ISBN", "Check Constraint Test Book", "Author C", "Science", 2, 0));
        int bookId = book.getId();

        // Attempt direct forced negative stock update bypassing the DAO WHERE condition
        try (Connection conn = DBConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE books SET available_copies = -1 WHERE book_id = ?")) {
            ps.setInt(1, bookId);
            SQLException ex = assertThrows(SQLException.class, ps::executeUpdate,
                    "Database CHECK constraint chk_books_available_copies must reject negative available_copies");
            assertTrue(ex.getMessage().toLowerCase().contains("check") ||
                       ex.getMessage().toLowerCase().contains("constraint"),
                    "Exception message must reflect check constraint failure: " + ex.getMessage());
        }

        // Verify stock remains untouched at 0
        Book current = bookDao.findById(bookId).orElseThrow();
        assertEquals(0, current.getAvailableCopies(), "Available copies must remain 0 after rejected DDL violation");
    }

    @Test
    @DisplayName("Challenge 6: Interleaved concurrent borrows and returns preserve total inventory conservation")
    void testInterleavedBorrowAndReturn_InventoryConservation() throws Exception {
        final int totalCopies = 8;
        final int initialAvailable = 3;
        final int initialBorrowed = totalCopies - initialAvailable; // 5

        final Book book = bookDao.save(new Book("INTERLEAVE-ISBN", "Interleaved Ops Book", "Author I", "History", totalCopies, initialAvailable));
        final int bookId = book.getId();

        // Pre-create members and 5 active transactions
        List<User> initialMembers = createTestMembers(initialBorrowed, "INITMEM");
        List<Transaction> activeLoansToReturn = new ArrayList<>();
        try (Connection conn = DBConnectionFactory.getConnection()) {
            for (User u : initialMembers) {
                Transaction tx = new Transaction(bookId, u.getId(),
                        new Timestamp(System.currentTimeMillis() - 86400000L),
                        new Timestamp(System.currentTimeMillis() + 86400000L * 13),
                        null, TransactionStatus.BORROWED, 0.0);
                activeLoansToReturn.add(transactionDao.createTransaction(conn, tx));
            }
        }

        final int borrowThreads = 20;
        final int returnThreads = initialBorrowed; // 5
        final int totalThreads = borrowThreads + returnThreads;

        List<User> newBorrowers = createTestMembers(borrowThreads, "NEWBORROW");
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);

        AtomicInteger successfulBorrows = new AtomicInteger(0);
        AtomicInteger successfulReturns = new AtomicInteger(0);
        List<Throwable> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());

        // Submit 20 borrow workers
        for (int i = 0; i < borrowThreads; i++) {
            final User member = newBorrowers.get(i);
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Connection conn = null;
                    try {
                        conn = DBConnectionFactory.getConnection();
                        conn.setAutoCommit(false);

                        boolean secured = bookDao.decrementAvailableCopies(conn, bookId);
                        if (secured) {
                            Transaction tx = new Transaction(bookId, member.getId(),
                                    new Timestamp(System.currentTimeMillis()),
                                    new Timestamp(System.currentTimeMillis() + 86400000L * 14),
                                    null, TransactionStatus.BORROWED, 0.0);
                            transactionDao.createTransaction(conn, tx);
                            conn.commit();
                            successfulBorrows.incrementAndGet();
                        } else {
                            conn.rollback();
                        }
                    } catch (Exception e) {
                        if (conn != null) {
                            try {
                                conn.rollback();
                            } catch (SQLException ignored) {}
                        }
                        unexpectedErrors.add(e);
                    } finally {
                        if (conn != null) {
                            try {
                                conn.setAutoCommit(true);
                                conn.close();
                            } catch (SQLException ignored) {}
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    unexpectedErrors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Submit 5 return workers
        for (int i = 0; i < returnThreads; i++) {
            final Transaction loan = activeLoansToReturn.get(i);
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Connection conn = null;
                    try {
                        conn = DBConnectionFactory.getConnection();
                        conn.setAutoCommit(false);

                        boolean returned = transactionDao.recordReturn(conn, loan.getId(), new Timestamp(System.currentTimeMillis()), 0.0);
                        if (returned) {
                            boolean incremented = bookDao.incrementAvailableCopies(conn, bookId);
                            assertTrue(incremented, "Stock increment must succeed on valid return");
                            conn.commit();
                            successfulReturns.incrementAndGet();
                        } else {
                            conn.rollback();
                        }
                    } catch (Exception e) {
                        if (conn != null) {
                            try {
                                conn.rollback();
                            } catch (SQLException ignored) {}
                        }
                        unexpectedErrors.add(e);
                    } finally {
                        if (conn != null) {
                            try {
                                conn.setAutoCommit(true);
                                conn.close();
                            } catch (SQLException ignored) {}
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    unexpectedErrors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All interleaved workers must complete within timeout");
        assertTrue(unexpectedErrors.isEmpty(), "Zero unexpected errors in interleaved ops: " + unexpectedErrors);
        assertEquals(5, successfulReturns.get(), "All 5 returns must successfully process");

        // Verify total inventory conservation law: availableCopies + activeLoans == totalCopies
        Book verifiedBook = bookDao.findById(bookId).orElseThrow();
        int finalAvailable = verifiedBook.getAvailableCopies();
        int finalActiveLoans = transactionDao.countActiveLoans();

        assertTrue(finalAvailable >= 0, "Available copies must never be negative: " + finalAvailable);
        assertTrue(finalAvailable <= totalCopies, "Available copies must never exceed total copies: " + finalAvailable);
        assertEquals(totalCopies, finalAvailable + finalActiveLoans,
                "Conservation of inventory: available (" + finalAvailable + ") + active (" + finalActiveLoans + ") must equal total (" + totalCopies + ")");
    }
}
