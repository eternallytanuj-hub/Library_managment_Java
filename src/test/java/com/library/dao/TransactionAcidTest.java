package com.library.dao;

import com.library.config.DBConnectionFactory;
import com.library.dao.impl.BookDaoJdbcImpl;
import com.library.dao.impl.TransactionDaoJdbcImpl;
import com.library.dao.impl.UserDaoJdbcImpl;
import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Transaction;
import com.library.model.TransactionStatus;
import com.library.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ACID Transaction Atomicity and Concurrency Safety Tests")
class TransactionAcidTest extends BaseDaoTest {

    private BookDao bookDao;
    private TransactionDao transactionDao;
    private UserDao userDao;

    private User sampleUser;

    @BeforeEach
    void setUp() throws Exception {
        bookDao = new BookDaoJdbcImpl();
        transactionDao = new TransactionDaoJdbcImpl();
        userDao = new UserDaoJdbcImpl();

        Member mem = new Member();
        mem.setMembershipId("ACID-MEM");
        mem.setName("ACID Test User");
        mem.setEmail("acid@test.com");
        mem.setPasswordHash("pass");
        sampleUser = userDao.save(mem);
    }

    @Test
    @DisplayName("Should rollback inventory decrement if transaction insertion fails (ACID Atomicity)")
    void testBorrowRollback_OnTransactionInsertFailure() throws Exception {
        Book book = bookDao.save(new Book("ACID-ISBN-1", "ACID Book", "Author", "Science", 3, 3));
        int bookId = book.getId();

        Connection conn = DBConnectionFactory.getConnection();
        try {
            conn.setAutoCommit(false);

            // Step 1: Decrement inventory (stock becomes 2 in uncommitted transaction)
            boolean decremented = bookDao.decrementAvailableCopies(conn, bookId);
            assertTrue(decremented);

            // Step 2: Simulate failure (e.g. invalid foreign key userId = 99999)
            Transaction failedTx = new Transaction(bookId, 99999,
                    new Timestamp(System.currentTimeMillis()),
                    new Timestamp(System.currentTimeMillis() + 86400000L * 14),
                    null, TransactionStatus.BORROWED, 0.0);

            try {
                transactionDao.createTransaction(conn, failedTx);
            } catch (SQLException expectedFkFailure) {
                // Step 3: Rollback on error
                conn.rollback();
            }
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }

        // Verify with clean independent query: stock must be intact (still 3)!
        Book verifiedBook = bookDao.findById(bookId).orElseThrow();
        assertEquals(3, verifiedBook.getAvailableCopies(),
                "Rollback must restore available copies to original 3 with zero phantom stock decrements");
    }

    @Test
    @DisplayName("Should safely handle concurrent borrow requests with atomic inventory boundaries")
    void testConcurrentBorrowing_AtomicInventoryCheck() throws Exception {
        // Book with exactly 2 available copies
        Book book = bookDao.save(new Book("CONCURRENT-ISBN", "Concurrent Book", "Author", "Tech", 2, 2));
        int bookId = book.getId();

        int numberOfThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(numberOfThreads);

        AtomicInteger successfulBorrows = new AtomicInteger(0);
        AtomicInteger failedBorrows = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    startSignal.await(); // Synchronize all threads to hit concurrently

                    Connection conn = null;
                    try {
                        conn = DBConnectionFactory.getConnection();
                        conn.setAutoCommit(false);

                        // Atomic decrement
                        boolean stockSecured = bookDao.decrementAvailableCopies(conn, bookId);
                        if (!stockSecured) {
                            conn.rollback();
                            failedBorrows.incrementAndGet();
                            return;
                        }

                        // Insert transaction
                        Transaction tx = new Transaction(bookId, sampleUser.getId(),
                                new Timestamp(System.currentTimeMillis()),
                                new Timestamp(System.currentTimeMillis() + 86400000L * 14),
                                null, TransactionStatus.BORROWED, 0.0);
                        transactionDao.createTransaction(conn, tx);

                        conn.commit();
                        successfulBorrows.incrementAndGet();
                    } catch (Exception ex) {
                        if (conn != null) {
                            try {
                                conn.rollback();
                            } catch (SQLException ignored) {}
                        }
                        failedBorrows.incrementAndGet();
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
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        // Release all threads simultaneously
        startSignal.countDown();
        boolean finishedInTime = doneSignal.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finishedInTime, "Concurrent borrows should finish within timeout");
        assertEquals(2, successfulBorrows.get(), "Exactly 2 threads must succeed in borrowing");
        assertEquals(3, failedBorrows.get(), "Remaining 3 threads must fail due to zero available stock");

        // Verify database inventory state
        Book finalBook = bookDao.findById(bookId).orElseThrow();
        assertEquals(0, finalBook.getAvailableCopies(), "Available copies must be exactly 0, never negative");
        assertEquals(2, transactionDao.countActiveLoans(), "Exactly 2 transactions should be recorded");
    }
}
