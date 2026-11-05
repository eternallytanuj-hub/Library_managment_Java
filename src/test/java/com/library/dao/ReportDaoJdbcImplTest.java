package com.library.dao;

import com.library.dao.impl.BookDaoJdbcImpl;
import com.library.dao.impl.ReportDaoJdbcImpl;
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

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ReportDaoJdbcImpl Integration Tests")
class ReportDaoJdbcImplTest extends BaseDaoTest {

    private ReportDao reportDao;
    private BookDao bookDao;
    private UserDao userDao;
    private TransactionDao transactionDao;

    @BeforeEach
    void setUp() {
        reportDao = new ReportDaoJdbcImpl();
        bookDao = new BookDaoJdbcImpl();
        userDao = new UserDaoJdbcImpl();
        transactionDao = new TransactionDaoJdbcImpl();
    }

    @Test
    @DisplayName("Should accurately aggregate summary metrics across catalog and transactions")
    void testSummaryMetrics() throws Exception {
        Member mem = new Member();
        mem.setMembershipId("REP-MEM-1");
        mem.setName("Report Member");
        mem.setEmail("rep@test.com");
        mem.setPasswordHash("pass");
        User user = userDao.save(mem);

        Book b1 = bookDao.save(new Book("REP-ISBN-1", "Book One", "Author 1", "Tech", 5, 4));
        Book b2 = bookDao.save(new Book("REP-ISBN-2", "Book Two", "Author 2", "Fiction", 3, 2));

        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp futureDue = new Timestamp(System.currentTimeMillis() + 86400000L * 14);
        Timestamp pastDue = new Timestamp(System.currentTimeMillis() - 86400000L * 2);

        transactionDao.save(new Transaction(b1.getId(), user.getId(), now, futureDue, null, TransactionStatus.BORROWED, 0.0));
        transactionDao.save(new Transaction(b2.getId(), user.getId(), now, pastDue, null, TransactionStatus.OVERDUE, 2.0));

        Map<String, Integer> metrics = reportDao.getSummaryMetrics();
        assertEquals(2, metrics.get("totalBooks"));
        assertEquals(8, metrics.get("totalCopies"));
        assertEquals(6, metrics.get("availableCopies"));
        assertEquals(2, metrics.get("borrowedCopies"));
        assertEquals(2, metrics.get("activeLoans"));
        assertEquals(1, metrics.get("overdueLoans"));
        assertEquals(1, metrics.get("totalMembers"));
    }

    @Test
    @DisplayName("Should aggregate genre distribution accurately")
    void testGetGenreDistribution() throws Exception {
        bookDao.save(new Book("G-1", "B1", "A1", "Science", 2, 2));
        bookDao.save(new Book("G-2", "B2", "A2", "Science", 3, 3));
        bookDao.save(new Book("G-3", "B3", "A3", "History", 4, 4));

        Map<String, Integer> dist = reportDao.getGenreDistribution();
        assertEquals(2, dist.get("Science"));
        assertEquals(1, dist.get("History"));
    }

    @Test
    @DisplayName("Should calculate availability ratio accurately")
    void testGetAvailabilityRatio() throws Exception {
        bookDao.save(new Book("AR-1", "B1", "A1", "G1", 10, 7));
        bookDao.save(new Book("AR-2", "B2", "A2", "G2", 5, 3));

        Map<String, Integer> ratio = reportDao.getAvailabilityRatio();
        assertEquals(10, ratio.get("Available Copies"));
        assertEquals(5, ratio.get("Borrowed Copies"));
    }

    @Test
    @DisplayName("Should retrieve top borrowed books ranked by loan frequency")
    void testGetTopBorrowedBooks() throws Exception {
        Member mem = new Member();
        mem.setMembershipId("TOP-MEM");
        mem.setName("Top Reader");
        mem.setEmail("top@test.com");
        mem.setPasswordHash("pass");
        User user = userDao.save(mem);

        Book popularBook = bookDao.save(new Book("POP-1", "Most Popular Book", "Famous Author", "Tech", 10, 8));
        Book regularBook = bookDao.save(new Book("POP-2", "Regular Book", "Regular Author", "Tech", 5, 4));

        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp due = new Timestamp(System.currentTimeMillis() + 86400000L * 14);

        // 2 loans for popularBook
        transactionDao.save(new Transaction(popularBook.getId(), user.getId(), now, due, null, TransactionStatus.BORROWED, 0.0));
        transactionDao.save(new Transaction(popularBook.getId(), user.getId(), now, due, null, TransactionStatus.BORROWED, 0.0));

        // 1 loan for regularBook
        transactionDao.save(new Transaction(regularBook.getId(), user.getId(), now, due, null, TransactionStatus.BORROWED, 0.0));

        List<Map<String, Object>> topBooks = reportDao.getTopBorrowedBooks(5);
        assertFalse(topBooks.isEmpty());
        assertEquals("Most Popular Book", topBooks.get(0).get("title"));
        assertEquals(2, ((Number) topBooks.get(0).get("borrowCount")).intValue());
    }

    @Test
    @DisplayName("Should retrieve monthly borrow trends without error")
    void testGetMonthlyBorrowTrends() throws Exception {
        Member mem = new Member();
        mem.setMembershipId("TREND-MEM");
        mem.setName("Trend Reader");
        mem.setEmail("trend@test.com");
        mem.setPasswordHash("pass");
        User user = userDao.save(mem);

        Book book = bookDao.save(new Book("TREND-1", "Trend Book", "Author", "Fiction", 5, 4));
        Timestamp now = new Timestamp(System.currentTimeMillis());
        transactionDao.save(new Transaction(book.getId(), user.getId(), now, now, null, TransactionStatus.BORROWED, 0.0));

        Map<String, Integer> trends = reportDao.getMonthlyBorrowTrends();
        assertNotNull(trends);
        assertFalse(trends.isEmpty(), "Monthly borrow trends must contain at least one record");
    }
}
