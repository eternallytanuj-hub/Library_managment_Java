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
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TransactionDaoJdbcImpl Integration Tests")
class TransactionDaoJdbcImplTest extends BaseDaoTest {

    private TransactionDao transactionDao;
    private UserDao userDao;
    private BookDao bookDao;

    private User sampleUser;
    private Book sampleBook;

    @BeforeEach
    void setUp() throws Exception {
        transactionDao = new TransactionDaoJdbcImpl();
        userDao = new UserDaoJdbcImpl();
        bookDao = new BookDaoJdbcImpl();

        Member mem = new Member();
        mem.setMembershipId("MEM-TX-1");
        mem.setName("Tx Borrower");
        mem.setEmail("txborrower@test.com");
        mem.setPasswordHash("pass");
        sampleUser = userDao.save(mem);

        sampleBook = bookDao.save(new Book("TX-ISBN-1", "Tx Book Title", "Author", "Fiction", 5, 5));
    }

    @Test
    @DisplayName("Should create transaction and retrieve with joined book and user details")
    void testCreateAndFindById() throws Exception {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp due = new Timestamp(System.currentTimeMillis() + 86400000L * 14);

        Transaction tx = new Transaction(sampleBook.getId(), sampleUser.getId(), now, due, null, TransactionStatus.BORROWED, 0.0);
        Transaction saved = transactionDao.save(tx);

        assertNotNull(saved.getId());
        assertTrue(saved.getId() > 0);

        Optional<Transaction> fetched = transactionDao.findById(saved.getId());
        assertTrue(fetched.isPresent());
        Transaction t = fetched.get();
        assertEquals(sampleBook.getId(), t.getBookId());
        assertEquals(sampleUser.getId(), t.getUserId());
        assertEquals(TransactionStatus.BORROWED, t.getStatus());
        assertNotNull(t.getBook(), "Joined Book object must be populated");
        assertEquals("Tx Book Title", t.getBook().getTitle());
        assertNotNull(t.getUser(), "Joined User object must be populated");
        assertEquals("Tx Borrower", t.getUser().getName());
    }

    @Test
    @DisplayName("Should correctly filter active loans for a given user")
    void testFindActiveLoansByUserId() throws Exception {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp due = new Timestamp(System.currentTimeMillis() + 86400000L * 14);

        Transaction activeTx = new Transaction(sampleBook.getId(), sampleUser.getId(), now, due, null, TransactionStatus.BORROWED, 0.0);
        transactionDao.save(activeTx);

        Transaction returnedTx = new Transaction(sampleBook.getId(), sampleUser.getId(), now, due, now, TransactionStatus.RETURNED, 0.0);
        transactionDao.save(returnedTx);

        List<Transaction> activeLoans = transactionDao.findActiveLoansByUserId(sampleUser.getId());
        assertEquals(1, activeLoans.size(), "Only BORROWED or OVERDUE transactions should be returned");
        assertEquals(TransactionStatus.BORROWED, activeLoans.get(0).getStatus());
    }

    @Test
    @DisplayName("Should record return status atomically and reject duplicate return calls")
    void testRecordReturn_Success() throws Exception {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp due = new Timestamp(System.currentTimeMillis() + 86400000L * 14);

        Transaction tx = transactionDao.save(new Transaction(sampleBook.getId(), sampleUser.getId(), now, due, null, TransactionStatus.BORROWED, 0.0));
        int txId = tx.getId();

        try (Connection conn = DBConnectionFactory.getConnection()) {
            Timestamp returnDate = new Timestamp(System.currentTimeMillis());
            boolean recorded = transactionDao.recordReturn(conn, txId, returnDate, 3.50);
            assertTrue(recorded, "Initial return check-in must succeed");

            Transaction updated = transactionDao.findById(txId).orElseThrow();
            assertEquals(TransactionStatus.RETURNED, updated.getStatus());
            assertEquals(new java.math.BigDecimal("3.50"), updated.getFineAmount());
            assertNotNull(updated.getReturnDate());

            // Second check-in on already RETURNED transaction must fail (return false)
            boolean duplicateReturn = transactionDao.recordReturn(conn, txId, returnDate, 0.00);
            assertFalse(duplicateReturn, "Returning already returned transaction must return false");
        }
    }

    @Test
    @DisplayName("Should detect overdue transactions when due date is in the past")
    void testFindOverdueTransactions() throws Exception {
        Timestamp pastBorrow = new Timestamp(System.currentTimeMillis() - 86400000L * 20);
        Timestamp pastDue = new Timestamp(System.currentTimeMillis() - 86400000L * 6);

        Transaction overdueTx = new Transaction(sampleBook.getId(), sampleUser.getId(), pastBorrow, pastDue, null, TransactionStatus.BORROWED, 0.0);
        transactionDao.save(overdueTx);

        List<Transaction> overdueList = transactionDao.findOverdueTransactions(new Timestamp(System.currentTimeMillis()));
        assertFalse(overdueList.isEmpty());
        assertEquals(overdueTx.getId(), overdueList.get(0).getId());
    }

    @Test
    @DisplayName("Should batch update transaction statuses efficiently")
    void testUpdateStatusBatch() throws Exception {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp due = new Timestamp(System.currentTimeMillis() + 86400000L * 14);

        Transaction t1 = transactionDao.save(new Transaction(sampleBook.getId(), sampleUser.getId(), now, due, null, TransactionStatus.BORROWED, 0.0));
        Transaction t2 = transactionDao.save(new Transaction(sampleBook.getId(), sampleUser.getId(), now, due, null, TransactionStatus.BORROWED, 0.0));

        int updated = transactionDao.updateStatusBatch(List.of(t1.getId(), t2.getId()), TransactionStatus.OVERDUE);
        assertEquals(2, updated);

        assertEquals(TransactionStatus.OVERDUE, transactionDao.findById(t1.getId()).orElseThrow().getStatus());
        assertEquals(TransactionStatus.OVERDUE, transactionDao.findById(t2.getId()).orElseThrow().getStatus());
    }
}
