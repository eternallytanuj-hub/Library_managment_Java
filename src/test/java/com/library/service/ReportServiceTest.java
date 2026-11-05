package com.library.service;

import com.library.dao.BaseDaoTest;
import com.library.dao.BookDao;
import com.library.dao.ReportDao;
import com.library.dao.TransactionDao;
import com.library.dao.UserDao;
import com.library.dao.dto.ReportSummaryDTO;
import com.library.dao.impl.BookDaoJdbcImpl;
import com.library.dao.impl.ReportDaoJdbcImpl;
import com.library.dao.impl.TransactionDaoJdbcImpl;
import com.library.dao.impl.UserDaoJdbcImpl;
import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Transaction;
import com.library.service.impl.ReportServiceImpl;
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

@DisplayName("ReportService Integration Tests")
class ReportServiceTest extends BaseDaoTest {

    private BookDao bookDao;
    private UserDao userDao;
    private TransactionDao transactionDao;
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        bookDao = new BookDaoJdbcImpl();
        userDao = new UserDaoJdbcImpl();
        transactionDao = new TransactionDaoJdbcImpl();
        ReportDao reportDao = new ReportDaoJdbcImpl();
        reportService = new ReportServiceImpl(reportDao);
    }

    @Test
    @DisplayName("Should generate summary metrics and map into ReportSummaryDTO")
    void testSummaryMetricsAndDTO() throws Exception {
        Member m1 = new Member("MEM-1001", "Member 1", "m1@test.com", "hash");
        userDao.save(m1);
        Member m2 = new Member("MEM-1002", "Member 2", "m2@test.com", "hash");
        userDao.save(m2);

        Book b1 = bookDao.save(new Book("978-001", "Book 1", "Author 1", "Science", 5, 3)); // 2 borrowed
        Book b2 = bookDao.save(new Book("978-002", "Book 2", "Author 2", "Fiction", 4, 4)); // 0 borrowed

        transactionDao.save(new Transaction(b1.getId(), m1.getId(),
                new Timestamp(System.currentTimeMillis()),
                new Timestamp(System.currentTimeMillis() + 86400000L)));

        Map<String, Integer> metrics = reportService.getSummaryMetrics();
        assertNotNull(metrics);
        assertEquals(2, metrics.get("totalBooks"));
        assertEquals(9, metrics.get("totalCopies"));
        assertEquals(7, metrics.get("availableCopies"));
        assertEquals(2, metrics.get("borrowedCopies"));
        assertEquals(2, metrics.get("totalMembers"));

        ReportSummaryDTO dto = reportService.getReportSummary();
        assertNotNull(dto);
        assertEquals(2, dto.getTotalBooks());
        assertEquals(9, dto.getTotalCopies());
        assertEquals(7, dto.getAvailableCopies());
        assertEquals(2, dto.getBorrowedCopies());
        assertEquals(2, dto.getTotalMembers());
        assertTrue(dto.getAvailabilityPercentage() > 70.0 && dto.getAvailabilityPercentage() < 80.0);
    }

    @Test
    @DisplayName("Should aggregate genre distribution")
    void testGenreDistribution() throws Exception {
        bookDao.save(new Book("978-003", "Sci 1", "Author", "Science", 2));
        bookDao.save(new Book("978-004", "Sci 2", "Author", "Science", 3));
        bookDao.save(new Book("978-005", "Hist 1", "Author", "History", 4));

        Map<String, Integer> distribution = reportService.getGenreDistribution();
        assertEquals(2, distribution.get("Science"));
        assertEquals(1, distribution.get("History"));
    }

    @Test
    @DisplayName("Should aggregate availability ratio")
    void testAvailabilityRatio() throws Exception {
        bookDao.save(new Book("978-006", "Ratio Book", "Author", "Science", 10, 7));

        Map<String, Integer> ratio = reportService.getAvailabilityRatio();
        assertEquals(7, ratio.get("Available Copies"));
        assertEquals(3, ratio.get("Borrowed Copies"));
    }

    @Test
    @DisplayName("Should query monthly trends and top borrowed books ranking")
    void testTrendsAndTopBooks() throws Exception {
        Member member = new Member("MEM-1003", "Reader", "r@test.com", "hash");
        userDao.save(member);
        Book book = bookDao.save(new Book("978-007", "Top Book", "Famous Author", "Tech", 5, 4));

        transactionDao.save(new Transaction(book.getId(), member.getId(),
                new Timestamp(System.currentTimeMillis()),
                new Timestamp(System.currentTimeMillis() + 86400000L)));

        Map<String, Integer> trends = reportService.getMonthlyBorrowTrends();
        assertNotNull(trends);
        assertFalse(trends.isEmpty());

        List<Map<String, Object>> topBooks = reportService.getTopBorrowedBooks(5);
        assertNotNull(topBooks);
        assertFalse(topBooks.isEmpty());
        assertEquals("Top Book", topBooks.get(0).get("title"));
    }
}
