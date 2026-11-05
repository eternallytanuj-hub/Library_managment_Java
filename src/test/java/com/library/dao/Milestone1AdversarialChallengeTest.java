package com.library.dao;

import com.library.config.DBConnectionFactory;
import com.library.dao.impl.BookDaoJdbcImpl;
import com.library.dao.impl.NotificationDaoJdbcImpl;
import com.library.dao.impl.SearchHistoryDaoJdbcImpl;
import com.library.dao.impl.TransactionDaoJdbcImpl;
import com.library.dao.impl.UserDaoJdbcImpl;
import com.library.exception.DatabaseOperationException;
import com.library.exception.DuplicateEntityException;
import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Notification;
import com.library.model.NotificationType;
import com.library.model.Role;
import com.library.model.SearchHistory;
import com.library.model.Transaction;
import com.library.model.TransactionStatus;
import com.library.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Empirical Adversarial Challenge Test Suite for Milestone 1:
 * 1. ACID Transaction Rollbacks & Multi-step Atomicity
 * 2. SQL Injection Fuzzing & PreparedStatement Neutralization
 * 3. Database Constraints, Model Invariants, and Boundary Conditions
 */
@DisplayName("Milestone 1 Empirical Adversarial Challenge Tests")
public class Milestone1AdversarialChallengeTest extends BaseDaoTest {

    private BookDaoJdbcImpl bookDao;
    private UserDaoJdbcImpl userDao;
    private TransactionDaoJdbcImpl transactionDao;
    private NotificationDaoJdbcImpl notificationDao;
    private SearchHistoryDaoJdbcImpl searchHistoryDao;

    private Member testMember;
    private Book testBook;

    @BeforeEach
    void setUpTestData() throws Exception {
        bookDao = new BookDaoJdbcImpl();
        userDao = new UserDaoJdbcImpl();
        transactionDao = new TransactionDaoJdbcImpl();
        notificationDao = new NotificationDaoJdbcImpl();
        searchHistoryDao = new SearchHistoryDaoJdbcImpl();

        Member m = new Member();
        m.setMembershipId("M1-ADV-MEM");
        m.setName("Adversarial Tester");
        m.setEmail("adversarial@test.com");
        m.setPasswordHash("$2a$10$hashedsecret");
        testMember = (Member) userDao.save(m);

        Book b = new Book("ADV-ISBN-001", "Security Engineering", "Ross Anderson", "Security", 10, 10);
        testBook = bookDao.save(b);
    }

    // =========================================================================
    // SECTION 1: ACID TRANSACTION ROLLBACK ATOMICITY
    // =========================================================================
    @Nested
    @DisplayName("1. ACID Transaction Rollback Atomicity Tests")
    class AcidRollbackTests {

        @Test
        @DisplayName("Multi-step borrow failure: 2 books decremented, 1 tx inserted, step 4 fails -> 100% rollback")
        void testMultiStepBorrowFailure_RollsBackAllState100Percent() throws Exception {
            Book book2 = bookDao.save(new Book("ADV-ISBN-002", "Database Internals", "Alex Petrov", "Database", 5, 5));

            int initialBook1Copies = testBook.getAvailableCopies(); // 10
            int initialBook2Copies = book2.getAvailableCopies();     // 5

            Connection conn = DBConnectionFactory.getConnection();
            try {
                conn.setAutoCommit(false);

                // Step 1: Decrement Book 1
                boolean dec1 = bookDao.decrementAvailableCopies(conn, testBook.getId());
                assertTrue(dec1, "Book 1 decrement should succeed in uncommitted transaction");

                // Step 2: Decrement Book 2
                boolean dec2 = bookDao.decrementAvailableCopies(conn, book2.getId());
                assertTrue(dec2, "Book 2 decrement should succeed in uncommitted transaction");

                // Step 3: Insert Transaction 1 for Book 1
                Transaction tx1 = new Transaction(testBook.getId(), testMember.getId(),
                        new Timestamp(System.currentTimeMillis()),
                        new Timestamp(System.currentTimeMillis() + 86400000L * 7),
                        null, TransactionStatus.BORROWED, 0.0);
                transactionDao.createTransaction(conn, tx1);
                assertTrue(tx1.getId() > 0, "Transaction 1 should obtain an ID");

                // Step 4: Deliberate failure on Transaction 2 (violating foreign key user_id = 999999)
                Transaction tx2 = new Transaction(book2.getId(), 999999,
                        new Timestamp(System.currentTimeMillis()),
                        new Timestamp(System.currentTimeMillis() + 86400000L * 7),
                        null, TransactionStatus.BORROWED, 0.0);

                try {
                    transactionDao.createTransaction(conn, tx2);
                    fail("Step 4 must fail with ForeignKey violation");
                } catch (SQLException expectedFkException) {
                    // Trigger complete rollback
                    conn.rollback();
                }
            } finally {
                conn.setAutoCommit(true);
                conn.close();
            }

            // Independent clean query verification
            Book refreshedBook1 = bookDao.findById(testBook.getId()).orElseThrow();
            Book refreshedBook2 = bookDao.findById(book2.getId()).orElseThrow();

            assertEquals(initialBook1Copies, refreshedBook1.getAvailableCopies(),
                    "Book 1 stock must be 100% restored to 10 with zero phantom decrements");
            assertEquals(initialBook2Copies, refreshedBook2.getAvailableCopies(),
                    "Book 2 stock must be 100% restored to 5 with zero phantom decrements");

            List<Transaction> activeLoans = transactionDao.findByUserId(testMember.getId());
            assertTrue(activeLoans.isEmpty(), "Zero transactions must remain persisted after rollback");
            assertEquals(0, transactionDao.countActiveLoans(), "Total active loan count must be exactly 0");
        }

        @Test
        @DisplayName("executeInTransaction helper: RuntimeException midway through transaction rolls back all changes")
        void testExecuteInTransaction_RollsBackOnUnhandledException() throws Exception {
            int originalCopies = testBook.getAvailableCopies();
            String originalName = testMember.getName();

            assertThrows(IllegalStateException.class, () -> {
                bookDao.executeInTransaction(conn -> {
                    // Step 1: Update user profile
                    String sqlUser = "UPDATE users SET name = ? WHERE user_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlUser)) {
                        ps.setString(1, "Corrupted Temp Name");
                        ps.setInt(2, testMember.getId());
                        ps.executeUpdate();
                    }

                    // Step 2: Decrement book stock
                    bookDao.decrementAvailableCopies(conn, testBook.getId());

                    // Step 3: Simulate catastrophic crash midway
                    throw new IllegalStateException("Simulated sudden runtime crash midway through business workflow");
                });
            });

            // Verify clean rollback on fresh connections
            User userRefreshed = userDao.findById(testMember.getId()).orElseThrow();
            Book bookRefreshed = bookDao.findById(testBook.getId()).orElseThrow();

            assertEquals(originalName, userRefreshed.getName(), "User name must not be altered after rollback");
            assertEquals(originalCopies, bookRefreshed.getAvailableCopies(), "Book stock must not be altered after rollback");
        }

        @Test
        @DisplayName("Return operation failure: Book incremented, return recorded, step 3 fails -> state remains BORROWED")
        void testReturnOperationFailure_RollbackRestoresBorrowedState() throws Exception {
            // First, create a legitimately borrowed transaction
            Book borrowedBook = bookDao.save(new Book("ADV-ISBN-003", "Clean Architecture", "Robert Martin", "Architecture", 3, 2));
            Transaction tx = new Transaction(borrowedBook.getId(), testMember.getId(),
                    new Timestamp(System.currentTimeMillis() - 86400000L),
                    new Timestamp(System.currentTimeMillis() + 86400000L * 13),
                    null, TransactionStatus.BORROWED, 0.0);
            Transaction savedTx = transactionDao.save(tx);

            int preReturnStock = borrowedBook.getAvailableCopies(); // 2

            Connection conn = DBConnectionFactory.getConnection();
            try {
                conn.setAutoCommit(false);

                // Step 1: Record return in ledger
                boolean returnRecorded = transactionDao.recordReturn(conn, savedTx.getId(),
                        new Timestamp(System.currentTimeMillis()), 0.0);
                assertTrue(returnRecorded, "Return record step should succeed");

                // Step 2: Increment available copies
                boolean stockIncremented = bookDao.incrementAvailableCopies(conn, borrowedBook.getId());
                assertTrue(stockIncremented, "Stock increment step should succeed");

                // Step 3: Simulate failure (e.g. invalid notification insert with fk violation)
                String invalidSql = "INSERT INTO notifications (user_id, title, message, type) VALUES (999999, 'T', 'M', 'GENERAL')";
                try (PreparedStatement ps = conn.prepareStatement(invalidSql)) {
                    ps.executeUpdate();
                    fail("Should have failed with FK constraint error");
                } catch (SQLException expectedFk) {
                    conn.rollback();
                }
            } finally {
                conn.setAutoCommit(true);
                conn.close();
            }

            // Verify independent database state
            Transaction unreturnedTx = transactionDao.findById(savedTx.getId()).orElseThrow();
            assertEquals(TransactionStatus.BORROWED, unreturnedTx.getStatus(),
                    "Transaction status must remain BORROWED after rollback");

            Book unreturnedBook = bookDao.findById(borrowedBook.getId()).orElseThrow();
            assertEquals(preReturnStock, unreturnedBook.getAvailableCopies(),
                    "Available copies must remain at 2 after rollback");
        }
    }

    // =========================================================================
    // SECTION 2: SQL INJECTION FUZZING (PREPAREDSTATEMENT IMMUNITY)
    // =========================================================================
    @Nested
    @DisplayName("2. SQL Injection Fuzzing & Immunity Tests")
    class SqlInjectionFuzzingTests {

        private static final String[] SQLI_PAYLOADS = {
                "' OR '1'='1",
                "' OR '1'='1' -- ",
                "' OR '1'='1' /*",
                "admin' --",
                "admin@library.com' OR 1=1 --",
                "'; DROP TABLE users; --",
                "'; DROP TABLE books; --",
                "'; DROP TABLE transactions; --",
                "' UNION SELECT 1, 'admin', 'hacked', 'hacked', 'LIBRARIAN', 1, 1, 1, CURRENT_TIMESTAMP --",
                "' UNION SELECT 1, '999', 'Hacked Title', 'Hacked Author', 'Hacked Genre', 99, 99, CURRENT_TIMESTAMP --",
                "\"' OR \"\"=\"\"",
                "1; SELECT pg_sleep(5); --",
                "1' WAITFOR DELAY '0:0:5'--",
                "CHAR(0x27) OR 1=1",
                "Robert'); DROP TABLE users; --",
                "test@test.com' AND (SELECT COUNT(*) FROM users) > 0 --",
                "\\'; DROP TABLE users; --",
                "%'; DROP TABLE books; --"
        };

        @Test
        @DisplayName("UserDao.findByEmail: Fuzzing with SQL injection payloads must return empty and preserve tables")
        void testFindByEmail_SqlInjectionFuzzing() throws Exception {
            for (String payload : SQLI_PAYLOADS) {
                Optional<User> result = userDao.findByEmail(payload);
                assertFalse(result.isPresent(),
                        "Payload '" + payload + "' must NOT authenticate or return an arbitrary user");
            }
            // Verify tables still exist and data is uncorrupted
            assertTableExists("users");
            Optional<User> validUser = userDao.findByEmail("adversarial@test.com");
            assertTrue(validUser.isPresent(), "Legitimate user must still exist untouched");
        }

        @Test
        @DisplayName("UserDao.findByMembershipId: Fuzzing with SQL injection payloads must return empty")
        void testFindByMembershipId_SqlInjectionFuzzing() throws Exception {
            for (String payload : SQLI_PAYLOADS) {
                Optional<User> result = userDao.findByMembershipId(payload);
                assertFalse(result.isPresent(),
                        "Payload '" + payload + "' must NOT return an arbitrary user by membershipId");
            }
            assertTableExists("users");
        }

        @Test
        @DisplayName("BookDao.findByIsbn: Fuzzing with SQL injection payloads must return empty and preserve tables")
        void testFindByIsbn_SqlInjectionFuzzing() throws Exception {
            for (String payload : SQLI_PAYLOADS) {
                Optional<Book> result = bookDao.findByIsbn(payload);
                assertFalse(result.isPresent(),
                        "Payload '" + payload + "' must NOT return books via SQL injection");
            }
            assertTableExists("books");
            Optional<Book> validBook = bookDao.findByIsbn("ADV-ISBN-001");
            assertTrue(validBook.isPresent(), "Legitimate book must still exist untouched");
        }

        @Test
        @DisplayName("BookDao.searchBooks: Fuzzing query and searchType must execute safely without injection")
        void testSearchBooks_SqlInjectionFuzzing() throws Exception {
            String[] searchTypes = {"TITLE", "AUTHOR", "GENRE", "ALL", null, "TITLE' OR '1'='1", "INVALID_FIELD"};

            for (String payload : SQLI_PAYLOADS) {
                for (String searchType : searchTypes) {
                    List<Book> results = bookDao.searchBooks(payload, searchType);
                    assertNotNull(results, "Search with payload must return non-null list");
                    // If payload does not contain literal substring of our test book, results should be 0
                    for (Book b : results) {
                        assertTrue(b.getTitle().contains(payload) || b.getAuthor().contains(payload) || b.getGenre().contains(payload),
                                "Returned book must match literal search string, not SQL injection logic");
                    }
                }
            }
            assertTableExists("books");
        }

        @Test
        @DisplayName("BookDao.findByGenre: Fuzzing genre with SQL injection payloads must return empty list")
        void testFindByGenre_SqlInjectionFuzzing() throws Exception {
            for (String payload : SQLI_PAYLOADS) {
                List<Book> results = bookDao.findByGenre(payload);
                assertTrue(results.isEmpty(),
                        "Payload '" + payload + "' must NOT return books via genre SQL injection");
            }
            assertTableExists("books");
        }

        @Test
        @DisplayName("TransactionDao.findAllWithFilter: Fuzzing statusFilter must safely bind parameter")
        void testFindAllWithFilter_SqlInjectionFuzzing() throws Exception {
            for (String payload : SQLI_PAYLOADS) {
                List<Transaction> results = transactionDao.findAllWithFilter(payload);
                assertNotNull(results, "Query with payload must safely execute and return list");
                for (Transaction t : results) {
                    assertEquals(payload.trim().toUpperCase(), t.getStatus().name(),
                            "Only literal status match could possibly return rows");
                }
            }
            assertTableExists("transactions");
        }

        @Test
        @DisplayName("Data persistence with adversarial payloads: Special characters, SQL statements stored verbatim")
        void testStoreAdversarialPayloadsVerbatim() throws Exception {
            String maliciousPayload = "Robert'); DROP TABLE users; --";

            // Store in user profile
            boolean profileUpdated = userDao.updateProfile(testMember.getId(), maliciousPayload, "bobby@tables.com");
            assertTrue(profileUpdated);

            User reloaded = userDao.findById(testMember.getId()).orElseThrow();
            assertEquals(maliciousPayload, reloaded.getName(), "Name must be stored verbatim with SQL keywords intact");
            assertTableExists("users");

            // Store in search history
            SearchHistory sh = new SearchHistory(testMember.getId(), maliciousPayload, "ALL");
            SearchHistory savedSh = searchHistoryDao.save(sh);
            assertNotNull(savedSh.getId());

            SearchHistory reloadedSh = searchHistoryDao.findById(savedSh.getId()).orElseThrow();
            assertEquals(maliciousPayload, reloadedSh.getQueryText(), "Search query text must be stored verbatim");

            // Store in notifications
            Notification n = new Notification(testMember.getId(), maliciousPayload, "Alert: ' OR 1=1; --", NotificationType.GENERAL);
            Notification savedN = notificationDao.save(n);
            assertNotNull(savedN.getId());

            Notification reloadedN = notificationDao.findById(savedN.getId()).orElseThrow();
            assertEquals(maliciousPayload, reloadedN.getTitle(), "Notification title must be stored verbatim");
        }

        private void assertTableExists(String tableName) throws SQLException {
            try (Connection conn = DBConnectionFactory.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
                assertTrue(rs.next(), "Table " + tableName + " must exist and be queryable");
            }
        }
    }

    // =========================================================================
    // SECTION 3: CONSTRAINT BOUNDARIES & MODEL/DB INTEGRITY
    // =========================================================================
    @Nested
    @DisplayName("3. Constraint Boundaries & Integrity Tests")
    class ConstraintBoundaryTests {

        @Test
        @DisplayName("Unique email violation: Rejects duplicate email with DuplicateEntityException")
        void testUniqueEmailConstraint() throws Exception {
            Member duplicateMember = new Member();
            duplicateMember.setMembershipId("M1-ADV-DIFF");
            duplicateMember.setName("Duplicate Email Person");
            duplicateMember.setEmail("adversarial@test.com"); // existing email
            duplicateMember.setPasswordHash("pass");

            assertThrows(DuplicateEntityException.class, () -> userDao.save(duplicateMember),
                    "Saving duplicate email must throw DuplicateEntityException");
        }

        @Test
        @DisplayName("Unique membershipId violation: Rejects duplicate membershipId with DuplicateEntityException")
        void testUniqueMembershipIdConstraint() throws Exception {
            Member duplicateMember = new Member();
            duplicateMember.setMembershipId("M1-ADV-MEM"); // existing membership ID
            duplicateMember.setName("Duplicate Membership Person");
            duplicateMember.setEmail("different@test.com");
            duplicateMember.setPasswordHash("pass");

            assertThrows(DuplicateEntityException.class, () -> userDao.save(duplicateMember),
                    "Saving duplicate membership ID must throw DuplicateEntityException");
        }

        @Test
        @DisplayName("Unique ISBN violation: Rejects duplicate ISBN with DuplicateEntityException")
        void testUniqueIsbnConstraint() throws Exception {
            Book duplicateBook = new Book("ADV-ISBN-001", "Duplicate ISBN Title", "Another Author", "Security", 5, 5);

            assertThrows(DuplicateEntityException.class, () -> bookDao.save(duplicateBook),
                    "Saving duplicate ISBN must throw DuplicateEntityException");
        }

        @Test
        @DisplayName("Check constraint: Database rejects invalid role values")
        void testInvalidRoleDatabaseCheckConstraint() throws Exception {
            String sql = "INSERT INTO users (membership_id, name, email, password_hash, role) " +
                         "VALUES ('MEM-INV-ROLE', 'Invalid Role User', 'invrole@test.com', 'pass', 'SUPERADMIN')";

            try (Connection conn = DBConnectionFactory.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                assertThrows(SQLException.class, ps::executeUpdate,
                        "Database CHECK constraint chk_users_role must reject role outside ('LIBRARIAN', 'MEMBER')");
            }
        }

        @Test
        @DisplayName("Model & DB boundary: Negative total copies rejected by model and database constraint")
        void testNegativeTotalCopiesConstraint() throws Exception {
            // Java model invariant validation
            assertThrows(IllegalArgumentException.class, () -> {
                new Book("NEG-ISBN-1", "Neg Book", "Author", "Genre", -1, 0);
            }, "Book constructor must reject negative total copies");

            Book validBook = new Book("NEG-ISBN-1", "Neg Book", "Author", "Genre", 5, 5);
            assertThrows(IllegalArgumentException.class, () -> {
                validBook.setTotalCopies(-3);
            }, "setTotalCopies must reject negative total copies");

            // Direct database CHECK constraint
            String sql = "INSERT INTO books (isbn, title, author, genre, total_copies, available_copies) " +
                         "VALUES ('NEG-DB-ISBN', 'Neg DB Book', 'Author', 'Genre', -5, 0)";
            try (Connection conn = DBConnectionFactory.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                assertThrows(SQLException.class, ps::executeUpdate,
                        "Database CHECK constraint chk_books_total_copies must reject total_copies < 0");
            }
        }

        @Test
        @DisplayName("Model & DB boundary: Available copies > total copies rejected by model and database constraint")
        void testAvailableCopiesExceedingTotalConstraint() throws Exception {
            // Java model invariant validation
            assertThrows(IllegalArgumentException.class, () -> {
                new Book("EXCEED-ISBN", "Exceed Book", "Author", "Genre", 3, 5);
            }, "Book constructor must reject available copies > total copies");

            Book book = new Book("EXCEED-ISBN", "Exceed Book", "Author", "Genre", 3, 3);
            assertThrows(IllegalArgumentException.class, () -> {
                book.setAvailableCopies(4);
            }, "setAvailableCopies must reject available copies > total copies");

            // Direct database CHECK constraint
            String sql = "INSERT INTO books (isbn, title, author, genre, total_copies, available_copies) " +
                         "VALUES ('EXCEED-DB-ISBN', 'Exceed DB Book', 'Author', 'Genre', 2, 5)";
            try (Connection conn = DBConnectionFactory.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                assertThrows(SQLException.class, ps::executeUpdate,
                        "Database CHECK constraint chk_books_available_copies must reject available_copies > total_copies");
            }
        }

        @Test
        @DisplayName("Atomic inventory boundary: decrement when available copies = 0 must return false and preserve 0")
        void testAtomicDecrementBoundary_AtZeroStock() throws Exception {
            Book zeroBook = bookDao.save(new Book("ZERO-ISBN", "Zero Stock Book", "Author", "Genre", 2, 0));

            try (Connection conn = DBConnectionFactory.getConnection()) {
                boolean decremented = bookDao.decrementAvailableCopies(conn, zeroBook.getId());
                assertFalse(decremented, "Decrementing at 0 available stock must return false");
            }

            Book refreshed = bookDao.findById(zeroBook.getId()).orElseThrow();
            assertEquals(0, refreshed.getAvailableCopies(), "Available copies must remain exactly 0, never negative");
        }

        @Test
        @DisplayName("Atomic inventory boundary: increment when available copies = total copies must return false")
        void testAtomicIncrementBoundary_AtMaxStock() throws Exception {
            Book fullBook = bookDao.save(new Book("FULL-ISBN", "Full Stock Book", "Author", "Genre", 3, 3));

            try (Connection conn = DBConnectionFactory.getConnection()) {
                boolean incremented = bookDao.incrementAvailableCopies(conn, fullBook.getId());
                assertFalse(incremented, "Incrementing when available == total must return false");
            }

            Book refreshed = bookDao.findById(fullBook.getId()).orElseThrow();
            assertEquals(3, refreshed.getAvailableCopies(), "Available copies must remain at total_copies 3");
        }

        @Test
        @DisplayName("Foreign key RESTRICT constraint: Cannot delete user with active loan")
        void testForeignKeyRestrictOnUserDeleteWithActiveLoans() throws Exception {
            // Create active loan
            Transaction tx = new Transaction(testBook.getId(), testMember.getId(),
                    new Timestamp(System.currentTimeMillis()),
                    new Timestamp(System.currentTimeMillis() + 86400000L * 14),
                    null, TransactionStatus.BORROWED, 0.0);
            transactionDao.save(tx);

            // Attempt to delete user
            assertThrows(DatabaseOperationException.class, () -> userDao.deleteById(testMember.getId()),
                    "Deleting user with active loan must fail with DatabaseOperationException (FK RESTRICT)");

            // Verify user still exists
            assertTrue(userDao.findById(testMember.getId()).isPresent(), "User must not be deleted");
        }

        @Test
        @DisplayName("Foreign key RESTRICT constraint: Cannot delete book with active loan")
        void testForeignKeyRestrictOnBookDeleteWithActiveLoans() throws Exception {
            // Create active loan
            Transaction tx = new Transaction(testBook.getId(), testMember.getId(),
                    new Timestamp(System.currentTimeMillis()),
                    new Timestamp(System.currentTimeMillis() + 86400000L * 14),
                    null, TransactionStatus.BORROWED, 0.0);
            transactionDao.save(tx);

            // Attempt to delete book
            assertThrows(DatabaseOperationException.class, () -> bookDao.deleteById(testBook.getId()),
                    "Deleting book with active loan must fail with DatabaseOperationException (FK RESTRICT)");

            // Verify book still exists
            assertTrue(bookDao.findById(testBook.getId()).isPresent(), "Book must not be deleted");
        }

        @Test
        @DisplayName("Check constraint: Database rejects invalid transaction status")
        void testInvalidTransactionStatusCheckConstraint() throws Exception {
            String sql = "INSERT INTO transactions (book_id, user_id, borrow_date, due_date, status) " +
                         "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'CANCELLED')";
            try (Connection conn = DBConnectionFactory.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, testBook.getId());
                ps.setInt(2, testMember.getId());
                assertThrows(SQLException.class, ps::executeUpdate,
                        "chk_transactions_status must reject status outside ('BORROWED', 'RETURNED', 'OVERDUE')");
            }
        }

        @Test
        @DisplayName("Check constraint: Database rejects invalid notification type")
        void testInvalidNotificationTypeCheckConstraint() throws Exception {
            String sql = "INSERT INTO notifications (user_id, title, message, type) " +
                         "VALUES (?, 'Title', 'Message', 'PHISHING_ALERT')";
            try (Connection conn = DBConnectionFactory.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, testMember.getId());
                assertThrows(SQLException.class, ps::executeUpdate,
                        "chk_notifications_type must reject type outside ('DUE_DATE', 'NEW_ARRIVAL', 'SYSTEM', 'GENERAL')");
            }
        }

        @Test
        @DisplayName("Check constraint: Database rejects invalid search history type")
        void testInvalidSearchHistoryTypeCheckConstraint() throws Exception {
            String sql = "INSERT INTO search_history (user_id, query_text, search_type) " +
                         "VALUES (?, 'java', 'INVALIDSUBJECT')";
            try (Connection conn = DBConnectionFactory.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, testMember.getId());
                assertThrows(SQLException.class, ps::executeUpdate,
                        "chk_search_history_type must reject search_type outside ('TITLE', 'AUTHOR', 'GENRE', 'ALL')");
            }
        }
    }
}
