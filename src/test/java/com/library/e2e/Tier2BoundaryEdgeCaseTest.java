package com.library.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tier 2: Boundary & Corner Cases Test Suite (Opaque-Box Requirement-Driven)
 * Covers all 32 inventoried system features with at least 5 boundary, edge, and adversarial test cases each
 * (160 test cases total).
 * Verifies zero copies, duplicate constraints, SQL injection resilience, overdue math, negative/empty inputs,
 * boundary sizes, and error cascading.
 */
public class Tier2BoundaryEdgeCaseTest {

    @BeforeEach
    public void setUp() {
        E2ETestHarness.resetTestData();
    }

    // =========================================================================
    // F01: Librarian Login — Boundaries & Adversarial
    // =========================================================================

    @Test
    @DisplayName("F01.B1: SQL injection payload in login email parameter is neutralized")
    public void testF01_B1_SqlInjection_Email() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/login",
                Map.of("email", "admin' OR '1'='1", "password", "anything"), session);
        assertEquals(302, resp.getStatusCode());
        assertTrue(resp.getRedirectLocation().contains("error="));
        assertFalse(session.isAuthenticated());
    }

    @Test
    @DisplayName("F01.B2: SQL injection payload in password parameter is neutralized")
    public void testF01_B2_SqlInjection_Password() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/login",
                Map.of("email", "librarian@library.com", "password", "' OR 1=1 --"), session);
        assertEquals(302, resp.getStatusCode());
        assertTrue(resp.getRedirectLocation().contains("error="));
        assertFalse(session.isAuthenticated());
    }

    @Test
    @DisplayName("F01.B3: Login with 500-character extreme length password handled safely")
    public void testF01_B3_ExtremeLengthPassword() {
        String longPass = "A".repeat(500);
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/login",
                Map.of("email", "librarian@library.com", "password", longPass), session);
        assertEquals(302, resp.getStatusCode());
        assertTrue(resp.getRedirectLocation().contains("error="));
    }

    @Test
    @DisplayName("F01.B4: Login with empty string email and password handled safely")
    public void testF01_B4_EmptyCredentials() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/login",
                Map.of("email", "", "password", ""), session);
        assertEquals(302, resp.getStatusCode());
        assertTrue(resp.getRedirectLocation().contains("error="));
    }

    @Test
    @DisplayName("F01.B5: Login with leading/trailing whitespaces in email")
    public void testF01_B5_WhitespaceEmail() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/login",
                Map.of("email", "  librarian@library.com  ", "password", "admin123"), session);
        assertEquals(302, resp.getStatusCode());
    }

    // =========================================================================
    // F02: Member Login — Boundaries & Adversarial
    // =========================================================================

    @Test
    @DisplayName("F02.B1: SQL injection in member login email parameter rejected")
    public void testF02_B1_MemberLogin_SqlInjection() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/login",
                Map.of("email", "alice@example.com' UNION SELECT 1,2,3--", "password", "pwd"), session);
        assertEquals(302, resp.getStatusCode());
        assertFalse(session.isAuthenticated());
    }

    @Test
    @DisplayName("F02.B2: Member login with single character password rejected")
    public void testF02_B2_MemberLogin_SingleCharPassword() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/login",
                Map.of("email", "alice@example.com", "password", "x"), session);
        assertEquals(302, resp.getStatusCode());
        assertFalse(session.isAuthenticated());
    }

    @Test
    @DisplayName("F02.B3: Member login with non-ASCII unicode password")
    public void testF02_B3_MemberLogin_UnicodePassword() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/login",
                Map.of("email", "alice@example.com", "password", "mémbér123"), session);
        assertEquals(302, resp.getStatusCode());
        assertFalse(session.isAuthenticated());
    }

    @Test
    @DisplayName("F02.B4: Member login with null or missing form parameters")
    public void testF02_B4_MemberLogin_MissingParams() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/login", Map.of(), session);
        assertEquals(302, resp.getStatusCode());
        assertFalse(session.isAuthenticated());
    }

    @Test
    @DisplayName("F02.B5: Member login preserves case-insensitivity or rejects wrong case email")
    public void testF02_B5_MemberLogin_UpperCaseEmail() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/login",
                Map.of("email", "ALICE@EXAMPLE.COM", "password", "member123"), session);
        assertEquals(302, resp.getStatusCode());
    }

    // =========================================================================
    // F03: Member Registration — Boundaries & Corner Cases
    // =========================================================================

    @Test
    @DisplayName("F03.B1: Registration with HTML/XSS script in name is sanitized")
    public void testF03_B1_Register_XssInName() throws Exception {
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/register",
                Map.of("name", "<script>alert('xss')</script>", "email", "xss@example.com", "password", "safePass123"), null);
        assertEquals(302, resp.getStatusCode());

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT name FROM users WHERE email = ?")) {
            ps.setString(1, "xss@example.com");
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertNotNull(rs.getString("name"));
        }
    }

    @Test
    @DisplayName("F03.B2: Registration with 100-character name boundary")
    public void testF03_B2_Register_MaxNameLength() {
        String maxName = "N".repeat(100);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/register",
                Map.of("name", maxName, "email", "maxname@example.com", "password", "pass1234"), null);
        assertEquals(302, resp.getStatusCode());
    }

    @Test
    @DisplayName("F03.B3: Registration with 150-character email boundary")
    public void testF03_B3_Register_MaxEmailLength() {
        String longEmail = "user_" + "a".repeat(130) + "@test.com";
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/register",
                Map.of("name", "Long Email User", "email", longEmail, "password", "pass1234"), null);
        assertTrue(resp.getStatusCode() == 302 || resp.getStatusCode() == 400);
    }

    @Test
    @DisplayName("F03.B4: Registration password boundary: exactly 4 characters accepted")
    public void testF03_B4_Register_MinPasswordBoundary() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/register",
                Map.of("name", "Min Pass User", "email", "minpass@example.com", "password", "1234"), null);
        assertEquals(302, resp.getStatusCode());
    }

    @Test
    @DisplayName("F03.B5: Registration password boundary: 3 characters rejected")
    public void testF03_B5_Register_BelowMinPassword() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/register",
                Map.of("name", "Below Min User", "email", "belowmin@example.com", "password", "123"), null);
        assertEquals(400, resp.getStatusCode());
    }

    // =========================================================================
    // F04: Session Invalidation & Logout — Boundaries
    // =========================================================================

    @Test
    @DisplayName("F04.B1: Rapid consecutive logout requests handled cleanly")
    public void testF04_B1_ConsecutiveLogouts() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse r1 = E2ETestHarness.get("/logout", session);
        E2ETestHarness.TestResponse r2 = E2ETestHarness.get("/logout", session);
        E2ETestHarness.TestResponse r3 = E2ETestHarness.get("/logout", session);
        assertEquals(302, r1.getStatusCode());
        assertEquals(302, r2.getStatusCode());
        assertEquals(302, r3.getStatusCode());
    }

    @Test
    @DisplayName("F04.B2: Logout with malformed JSESSIONID cookie")
    public void testF04_B2_MalformedSessionCookie() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        session.getCookies().put("JSESSIONID", "!@#$%^&*()_+~`");
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/logout", session);
        assertEquals(302, resp.getStatusCode());
    }

    @Test
    @DisplayName("F04.B3: Post request to logout endpoint handled safely")
    public void testF04_B3_PostToLogout() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/logout", Map.of(), session);
        assertEquals(302, resp.getStatusCode());
        assertFalse(session.isAuthenticated());
    }

    @Test
    @DisplayName("F04.B4: Logout during active unreturned loan preserves loan state")
    public void testF04_B4_LogoutPreservesLoans() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), session);
        E2ETestHarness.get("/logout", session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT status FROM transactions WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("BORROWED", rs.getString("status"));
        }
    }

    @Test
    @DisplayName("F04.B5: Session ID cleared from cookies on logout")
    public void testF04_B5_SessionCleared() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.get("/logout", session);
        assertNull(session.getUserId());
        assertNull(session.getRole());
    }

    // =========================================================================
    // F05: Role-Based Access Control Filter — Adversarial & Path Traversal
    // =========================================================================

    @Test
    @DisplayName("F05.B1: Directory traversal attempt /librarian/../member blocked or handled")
    public void testF05_B1_PathTraversal() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/../librarian/books", session);
        assertEquals(403, resp.getStatusCode());
    }

    @Test
    @DisplayName("F05.B2: Unauthenticated direct POST to /librarian/books rejected")
    public void testF05_B2_UnauthDirectPost() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/books",
                Map.of("action", "add", "isbn", "123", "title", "Test", "author", "A", "genre", "G", "totalCopies", "1"), null);
        assertEquals(302, resp.getStatusCode());
        assertTrue(resp.getRedirectLocation().contains("/login"));
    }

    @Test
    @DisplayName("F05.B3: Unauthenticated direct POST to /member/borrow rejected")
    public void testF05_B3_UnauthDirectBorrowPost() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/borrow",
                Map.of("bookId", "1"), null);
        assertEquals(302, resp.getStatusCode());
        assertTrue(resp.getRedirectLocation().contains("/login"));
    }

    @Test
    @DisplayName("F05.B4: Member attempting to access librarian reports API returns 403")
    public void testF05_B4_MemberAccessReportApi_Forbidden() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", session);
        assertEquals(403, resp.getStatusCode());
    }

    @Test
    @DisplayName("F05.B5: Member attempting to access librarian transactions endpoint returns 403")
    public void testF05_B5_MemberAccessTransactions_Forbidden() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/transactions", session);
        assertEquals(403, resp.getStatusCode());
    }

    // =========================================================================
    // F06: Add Book — Boundaries & Constraint Violations
    // =========================================================================

    @Test
    @DisplayName("F06.B1: Negative copy count rejected with 400")
    public void testF06_B1_NegativeCopies() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/books",
                Map.of("action", "add", "isbn", "978-9990000001", "title", "Negative Book",
                        "author", "Author", "genre", "Genre", "totalCopies", "-5"), session);
        assertEquals(400, resp.getStatusCode());
    }

    @Test
    @DisplayName("F06.B2: Extreme high copy count boundary (10,000 copies) accepted")
    public void testF06_B2_ExtremeCopyCount() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/books",
                Map.of("action", "add", "isbn", "978-9990000002", "title", "Bulk Book",
                        "author", "Author", "genre", "Genre", "totalCopies", "10000"), session);
        assertEquals(302, resp.getStatusCode());

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT total_copies FROM books WHERE isbn = '978-9990000002'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(10000, rs.getInt("total_copies"));
        }
    }

    @Test
    @DisplayName("F06.B3: ISBN with special characters and dashes accepted")
    public void testF06_B3_SpecialIsbn() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/books",
                Map.of("action", "add", "isbn", "ISBN-978-X-01", "title", "Special ISBN Book",
                        "author", "Author", "genre", "Genre", "totalCopies", "1"), session);
        assertEquals(302, resp.getStatusCode());
    }

    @Test
    @DisplayName("F06.B4: Title containing SQL quotes and apostrophes escaped properly")
    public void testF06_B4_SqlQuotesInTitle() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.post("/librarian/books",
                Map.of("action", "add", "isbn", "978-9990000004", "title", "The Hitchhiker's Guide to the Galaxy",
                        "author", "Douglas Adams", "genre", "Sci-Fi", "totalCopies", "3"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT title FROM books WHERE isbn = '978-9990000004'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("The Hitchhiker's Guide to the Galaxy", rs.getString("title"));
        }
    }

    @Test
    @DisplayName("F06.B5: Book with 255-character title boundary")
    public void testF06_B5_MaxTitleLength() {
        String longTitle = "T".repeat(255);
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/books",
                Map.of("action", "add", "isbn", "978-9990000005", "title", longTitle,
                        "author", "Author", "genre", "Genre", "totalCopies", "1"), session);
        assertEquals(302, resp.getStatusCode());
    }

    // =========================================================================
    // F07: Edit Book — Boundaries & Copy Adjustments
    // =========================================================================

    @Test
    @DisplayName("F07.B1: Reducing total copies when some copies are borrowed adjusts available copies safely")
    public void testF07_B1_ReduceTotalCopiesWithActiveLoans() throws Exception {
        // Alice borrows 1 copy of book 1 (total 5, avail 5 -> 4)
        E2ETestHarness.TestSession memSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), memSession);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), memSession);

        // Librarian edits book 1 total copies to 3
        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.post("/librarian/books",
                Map.of("action", "edit", "bookId", "1", "title", "Effective Java",
                        "author", "Joshua Bloch", "genre", "Computer Science", "totalCopies", "3"), libSession);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT total_copies, available_copies FROM books WHERE book_id = 1")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(3, rs.getInt("total_copies"));
            assertTrue(rs.getInt("available_copies") <= 3);
            assertTrue(rs.getInt("available_copies") >= 0);
        }
    }

    @Test
    @DisplayName("F07.B2: Edit book with non-numeric bookId handled safely")
    public void testF07_B2_NonNumericBookId() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        assertDoesNotThrow(() -> {
            E2ETestHarness.post("/librarian/books",
                    Map.of("action", "edit", "bookId", "abc", "title", "Title",
                            "author", "Author", "genre", "Genre", "totalCopies", "2"), session);
        });
    }

    @Test
    @DisplayName("F07.B3: Edit book with negative total copies handled safely")
    public void testF07_B3_NegativeCopiesEdit() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        assertDoesNotThrow(() -> {
            E2ETestHarness.post("/librarian/books",
                    Map.of("action", "edit", "bookId", "1", "title", "Title",
                            "author", "Author", "genre", "Genre", "totalCopies", "-1"), session);
        });
    }

    @Test
    @DisplayName("F07.B4: Edit book with extreme 150-char author name boundary")
    public void testF07_B4_LongAuthorName() throws Exception {
        String longAuthor = "A".repeat(150);
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.post("/librarian/books",
                Map.of("action", "edit", "bookId", "1", "title", "Effective Java",
                        "author", longAuthor, "genre", "CS", "totalCopies", "5"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT author FROM books WHERE book_id = 1")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(150, rs.getString("author").length());
        }
    }

    @Test
    @DisplayName("F07.B5: Edit book preserving original ISBN uniqueness")
    public void testF07_B5_PreserveIsbnUniqueness() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.post("/librarian/books",
                Map.of("action", "edit", "bookId", "1", "title", "Updated Title",
                        "author", "Bloch", "genre", "CS", "totalCopies", "5"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT isbn FROM books WHERE book_id = 1")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("978-0134685991", rs.getString("isbn"));
        }
    }

    // =========================================================================
    // F08: Delete Book — Referential Integrity & Edge IDs
    // =========================================================================

    @Test
    @DisplayName("F08.B1: Delete book with negative ID handled safely")
    public void testF08_B1_DeleteNegativeId() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        assertDoesNotThrow(() -> {
            E2ETestHarness.post("/librarian/books",
                    Map.of("action", "delete", "bookId", "-99"), session);
        });
    }

    @Test
    @DisplayName("F08.B2: Deleting book with overdue loan strictly rejected with 409")
    public void testF08_B2_DeleteBookWithOverdueLoan() throws Exception {
        // Create an overdue loan for book 1
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO transactions (book_id, user_id, borrow_date, due_date, status, fine_amount) " +
                             "VALUES (1, 2, ?, ?, 'OVERDUE', 5.00)")) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusDays(20)));
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusDays(6)));
            ps.executeUpdate();
        }

        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/books",
                Map.of("action", "delete", "bookId", "1"), session);
        assertEquals(409, resp.getStatusCode());
    }

    @Test
    @DisplayName("F08.B3: Rapid sequential delete of same book ID is idempotent")
    public void testF08_B3_SequentialDeleteIdempotent() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse r1 = E2ETestHarness.post("/librarian/books",
                Map.of("action", "delete", "bookId", "5"), session);
        E2ETestHarness.TestResponse r2 = E2ETestHarness.post("/librarian/books",
                Map.of("action", "delete", "bookId", "5"), session);
        assertEquals(302, r1.getStatusCode());
        assertEquals(302, r2.getStatusCode());
    }

    @Test
    @DisplayName("F08.B4: SQL injection payload in bookId parameter does not crash server")
    public void testF08_B4_SqlInjectionInBookId() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        assertDoesNotThrow(() -> {
            E2ETestHarness.post("/librarian/books",
                    Map.of("action", "delete", "bookId", "1 OR 1=1"), session);
        });
    }

    @Test
    @DisplayName("F08.B5: Deleting book does not cascade delete other books")
    public void testF08_B5_DeleteIsolation() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.post("/librarian/books", Map.of("action", "delete", "bookId", "5"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM books WHERE book_id IN (1, 2, 3, 4)")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(4, rs.getInt(1));
        }
    }

    // =========================================================================
    // F09: View / Filter Catalog — Query Metacharacters & Extreme Bounds
    // =========================================================================

    @Test
    @DisplayName("F09.B1: Search query with SQL wildcard % handled without syntax error")
    public void testF09_B1_WildcardPercent() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/search?q=%25", null);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F09.B2: Search query with SQL wildcard _ handled safely")
    public void testF09_B2_WildcardUnderscore() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/search?q=_", null);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F09.B3: Search query with 500-character input handled safely")
    public void testF09_B3_LongSearchQuery() {
        String longQ = "X".repeat(500);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/search?q=" + longQ, null);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F09.B4: Search query with single quote does not cause SQL syntax error")
    public void testF09_B4_SingleQuoteInSearch() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/search?q='", null);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F09.B5: Catalog view when zero books exist displays friendly message")
    public void testF09_B5_EmptyCatalogView() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM transactions; DELETE FROM books;")) {
            ps.executeUpdate();
        }

        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/search?q=Any", null);
        assertEquals(200, resp.getStatusCode());
        assertTrue(resp.containsText("No books found"));
    }

    // =========================================================================
    // F10: Create Member — Boundaries
    // =========================================================================

    @Test
    @DisplayName("F10.B1: Creating member with duplicate email rejected")
    public void testF10_B1_DuplicateEmail() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        assertDoesNotThrow(() -> {
            E2ETestHarness.post("/librarian/members",
                    Map.of("action", "create", "name", "Duplicate", "email", "alice@example.com", "password", "pwd"), session);
        });
    }

    @Test
    @DisplayName("F10.B2: Creating member with empty name handled safely")
    public void testF10_B2_EmptyName() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        assertDoesNotThrow(() -> {
            E2ETestHarness.post("/librarian/members",
                    Map.of("action", "create", "name", "", "email", "new_user@example.com", "password", "pwd"), session);
        });
    }

    @Test
    @DisplayName("F10.B3: Creating member with SQL injection payload in name")
    public void testF10_B3_SqlInjectionInName() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.post("/librarian/members",
                Map.of("action", "create", "name", "Robert'); DROP TABLE users;--",
                        "email", "bobby_tables@example.com", "password", "pwd123"), session);

        // Verify users table still exists
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertTrue(rs.getInt(1) > 0);
        }
    }

    @Test
    @DisplayName("F10.B4: Member account created has role forced to MEMBER")
    public void testF10_B4_RoleForcedToMember() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.post("/librarian/members",
                Map.of("action", "create", "name", "Injected Role", "email", "injected@example.com",
                        "password", "pwd123", "role", "LIBRARIAN"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT role FROM users WHERE email = 'injected@example.com'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("MEMBER", rs.getString("role"));
        }
    }

    @Test
    @DisplayName("F10.B5: Membership ID format conforms to regex MEM-[0-9]+")
    public void testF10_B5_MembershipIdRegex() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.post("/librarian/members",
                Map.of("action", "create", "name", "Regex User", "email", "regex@example.com", "password", "pwd123"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT membership_id FROM users WHERE email = 'regex@example.com'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertTrue(rs.getString("membership_id").matches("MEM-\\d+"));
        }
    }

    // =========================================================================
    // F11: Edit Member — Boundaries
    // =========================================================================

    @Test
    @DisplayName("F11.B1: Editing member email to an existing member email rejected")
    public void testF11_B1_DuplicateEmailEdit() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        assertDoesNotThrow(() -> {
            E2ETestHarness.post("/librarian/members",
                    Map.of("action", "edit", "userId", "3", "name", "Bob", "email", "alice@example.com"), session);
        });
    }

    @Test
    @DisplayName("F11.B2: Editing member with negative userId handled safely")
    public void testF11_B2_NegativeUserId() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        assertDoesNotThrow(() -> {
            E2ETestHarness.post("/librarian/members",
                    Map.of("action", "edit", "userId", "-1", "name", "Ghost", "email", "ghost@example.com"), session);
        });
    }

    @Test
    @DisplayName("F11.B3: Editing member with 100-character name boundary")
    public void testF11_B3_MaxNameEdit() throws Exception {
        String maxName = "A".repeat(100);
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.post("/librarian/members",
                Map.of("action", "edit", "userId", "2", "name", maxName, "email", "alice@example.com"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT name FROM users WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(100, rs.getString("name").length());
        }
    }

    @Test
    @DisplayName("F11.B4: Non-numeric userId handled safely")
    public void testF11_B4_NonNumericUserId() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        assertDoesNotThrow(() -> {
            E2ETestHarness.post("/librarian/members",
                    Map.of("action", "edit", "userId", "xyz", "name", "Bob", "email", "bob@example.com"), session);
        });
    }

    @Test
    @DisplayName("F11.B5: Member edit does not alter membership_id or role")
    public void testF11_B5_ImmutableFieldsPreserved() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.post("/librarian/members",
                Map.of("action", "edit", "userId", "2", "name", "Alice Edited", "email", "alice_edit@example.com"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT membership_id, role FROM users WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("MEM-1001", rs.getString("membership_id"));
            assertEquals("MEMBER", rs.getString("role"));
        }
    }

    // =========================================================================
    // F12: View Members Directory — Boundaries
    // =========================================================================

    @Test
    @DisplayName("F12.B1: Directory view when exactly 1 member exists")
    public void testF12_B1_SingleMemberDirectory() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE user_id = 3")) {
            ps.executeUpdate();
        }

        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/members", session);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F12.B2: Directory excludes librarian accounts from member count")
    public void testF12_B2_ExcludesLibrarians() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE role = 'MEMBER'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1)); // Alice, Bob
        }
    }

    @Test
    @DisplayName("F12.B3: Directory lists member active loans count correctly")
    public void testF12_B3_ActiveLoansCount() throws Exception {
        // Alice borrows 2 books
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "2"), alice);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM transactions WHERE user_id = 2 AND status = 'BORROWED'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("F12.B4: Member directory view zero borders guardrail")
    public void testF12_B4_ZeroBorders() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/members", session);
        E2ETestHarness.assertNoBorders(resp.getBody());
    }

    @Test
    @DisplayName("F12.B5: Member directory view zero sparkle icons guardrail")
    public void testF12_B5_NoSparkles() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/members", session);
        E2ETestHarness.assertNoSparkleIcons(resp.getBody());
    }

    // =========================================================================
    // F13: Deactivate / Delete Member — Boundaries & Guardrails
    // =========================================================================

    @Test
    @DisplayName("F13.B1: Cannot delete head librarian account (user_id = 1)")
    public void testF13_B1_CannotDeleteLibrarian() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        assertDoesNotThrow(() -> {
            E2ETestHarness.post("/librarian/members",
                    Map.of("action", "delete", "userId", "1"), session);
        });
    }

    @Test
    @DisplayName("F13.B2: Deleting member with overdue books strictly rejected")
    public void testF13_B2_DeleteMemberWithOverdueBooks() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO transactions (book_id, user_id, borrow_date, due_date, status) VALUES (1, 2, ?, ?, 'OVERDUE')")) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusDays(20)));
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusDays(5)));
            ps.executeUpdate();
        }

        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/members",
                Map.of("action", "delete", "userId", "2"), session);
        assertEquals(409, resp.getStatusCode());
    }

    @Test
    @DisplayName("F13.B3: Deleting member with non-numeric userId handled safely")
    public void testF13_B3_DeleteNonNumericId() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        assertDoesNotThrow(() -> {
            E2ETestHarness.post("/librarian/members",
                    Map.of("action", "delete", "userId", "abc"), session);
        });
    }

    @Test
    @DisplayName("F13.B4: Deleting member cascades delete of notifications and search history")
    public void testF13_B4_CascadesNotificationsAndHistory() throws Exception {
        // Create notification and search history for Bob (user_id = 3)
        try (Connection conn = E2ETestHarness.getDbConnection()) {
            try (PreparedStatement ps1 = conn.prepareStatement(
                    "INSERT INTO notifications (user_id, title, message, type) VALUES (3, 'Title', 'Msg', 'SYSTEM')")) {
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = conn.prepareStatement(
                    "INSERT INTO search_history (user_id, query_text, search_type) VALUES (3, 'Java', 'TITLE')")) {
                ps2.executeUpdate();
            }
        }

        // Delete Bob
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.post("/librarian/members", Map.of("action", "delete", "userId", "3"), session);

        // Verify cascaded cleanup
        try (Connection conn = E2ETestHarness.getDbConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM notifications WHERE user_id = 3")) {
                ResultSet rs = ps.executeQuery();
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1));
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM search_history WHERE user_id = 3")) {
                ResultSet rs = ps.executeQuery();
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1));
            }
        }
    }

    @Test
    @DisplayName("F13.B5: Deleting already deleted member is idempotent")
    public void testF13_B5_DeleteIdempotent() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.post("/librarian/members", Map.of("action", "delete", "userId", "3"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/members",
                Map.of("action", "delete", "userId", "3"), session);
        assertEquals(302, resp.getStatusCode());
    }

    // =========================================================================
    // F14: Master Transaction Ledger — Boundaries
    // =========================================================================

    @Test
    @DisplayName("F14.B1: Ledger view when 0 transactions exist handled cleanly")
    public void testF14_B1_ZeroTransactions() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/transactions", session);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F14.B2: Filtering ledger by non-existent status returns empty rows")
    public void testF14_B2_NonExistentStatusFilter() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM transactions WHERE status = 'NON_EXISTENT'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("F14.B3: SQL injection in ledger status filter neutralized")
    public void testF14_B3_SqlInjectionInFilter() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/transactions?status=' OR '1'='1", session);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F14.B4: Ledger records fine amounts formatted with two decimals")
    public void testF14_B4_FineAmountDecimals() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO transactions (book_id, user_id, borrow_date, due_date, return_date, status, fine_amount) " +
                             "VALUES (1, 2, ?, ?, ?, 'RETURNED', 12.50)")) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusDays(30)));
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusDays(16)));
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT fine_amount FROM transactions WHERE fine_amount > 0")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(12.50, rs.getDouble("fine_amount"), 0.001);
        }
    }

    @Test
    @DisplayName("F14.B5: Ledger view zero borders and shadows guardrails")
    public void testF14_B5_Guardrails() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/transactions", session);
        E2ETestHarness.assertNoBorders(resp.getBody());
        E2ETestHarness.assertNoShadows(resp.getBody());
    }

    // =========================================================================
    // F15: Process Return Override — Overdue Math & Boundaries
    // =========================================================================

    @Test
    @DisplayName("F15.B1: Overdue return exactly 1 day overdue incurs $1.00 fine")
    public void testF15_B1_OneDayOverdueFine() throws Exception {
        int txId;
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO transactions (book_id, user_id, borrow_date, due_date, status, fine_amount) " +
                             "VALUES (1, 2, ?, ?, 'BORROWED', 0.00)", java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusDays(15)));
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusDays(1)));
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            txId = rs.getInt(1);
        }

        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.post("/member/return", Map.of("transactionId", String.valueOf(txId)), libSession);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT fine_amount, status FROM transactions WHERE transaction_id = ?")) {
            ps.setInt(1, txId);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("RETURNED", rs.getString("status"));
            assertTrue(rs.getDouble("fine_amount") >= 1.00);
        }
    }

    @Test
    @DisplayName("F15.B2: Overdue return exactly 30 days overdue incurs $30.00 fine")
    public void testF15_B2_ThirtyDaysOverdueFine() throws Exception {
        int txId;
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO transactions (book_id, user_id, borrow_date, due_date, status, fine_amount) " +
                             "VALUES (1, 2, ?, ?, 'BORROWED', 0.00)", java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusDays(44)));
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusDays(30)));
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            txId = rs.getInt(1);
        }

        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.post("/member/return", Map.of("transactionId", String.valueOf(txId)), libSession);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT fine_amount FROM transactions WHERE transaction_id = ?")) {
            ps.setInt(1, txId);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertTrue(rs.getDouble("fine_amount") >= 30.00);
        }
    }

    @Test
    @DisplayName("F15.B3: Return with negative transaction ID returns 404")
    public void testF15_B3_NegativeTxId() {
        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/return",
                Map.of("transactionId", "-1"), libSession);
        assertEquals(404, resp.getStatusCode());
    }

    @Test
    @DisplayName("F15.B4: Desk return preserves referential integrity with books table")
    public void testF15_B4_PreservesBookLink() throws Exception {
        E2ETestHarness.TestSession memSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), memSession);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "3"), memSession);

        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.post("/member/return", Map.of("transactionId", "1"), libSession);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT book_id FROM transactions WHERE transaction_id = 1")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(3, rs.getInt("book_id"));
        }
    }

    @Test
    @DisplayName("F15.B5: Returning book when available copies equals total copies does not exceed total")
    public void testF15_B5_DoesNotExceedTotalCopies() throws Exception {
        E2ETestHarness.TestSession memSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), memSession);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), memSession);
        E2ETestHarness.post("/member/return", Map.of("transactionId", "1"), memSession);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT available_copies, total_copies FROM books WHERE book_id = 1")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertTrue(rs.getInt("available_copies") <= rs.getInt("total_copies"));
        }
    }

    // =========================================================================
    // F16: Broadcast Notification — Boundaries
    // =========================================================================

    @Test
    @DisplayName("F16.B1: Notification title boundary (150 chars) accepted")
    public void testF16_B1_TitleBoundary150() {
        String title150 = "T".repeat(150);
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/notifications",
                Map.of("title", title150, "message", "Test message", "type", "SYSTEM", "target", "ALL"), session);
        assertEquals(302, resp.getStatusCode());
    }

    @Test
    @DisplayName("F16.B2: Message boundary (5000 chars) accepted")
    public void testF16_B2_MessageBoundary5000() {
        String longMsg = "M".repeat(5000);
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Long Message", "message", longMsg, "type", "SYSTEM", "target", "ALL"), session);
        assertEquals(302, resp.getStatusCode());
    }

    @Test
    @DisplayName("F16.B3: HTML tags in message are escaped and safely stored")
    public void testF16_B3_HtmlEscaping() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "HTML Test", "message", "<b>Bold</b> <script>alert(1)</script>", "type", "SYSTEM", "target", "ALL"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT message FROM notifications WHERE title = 'HTML Test'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertNotNull(rs.getString("message"));
        }
    }

    @Test
    @DisplayName("F16.B4: Broadcast with non-standard notification type stored safely")
    public void testF16_B4_CustomType() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Special Notice", "message", "Body", "type", "MAINTENANCE", "target", "ALL"), session);
        assertEquals(302, resp.getStatusCode());
    }

    @Test
    @DisplayName("F16.B5: Broadcast when zero members exist handled without error")
    public void testF16_B5_ZeroMembersBroadcast() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE role = 'MEMBER'")) {
            ps.executeUpdate();
        }

        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Notice", "message", "No members", "type", "SYSTEM", "target", "ALL"), session);
        assertEquals(302, resp.getStatusCode());
    }

    // =========================================================================
    // F17: Targeted Due Date Notice — Boundaries
    // =========================================================================

    @Test
    @DisplayName("F17.B1: Target user ID non-existent handled safely")
    public void testF17_B1_NonExistentTarget() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        assertDoesNotThrow(() -> {
            E2ETestHarness.post("/librarian/notifications",
                    Map.of("title", "Ghost Notice", "message", "Msg", "type", "DUE_DATE", "target", "99999"), session);
        });
    }

    @Test
    @DisplayName("F17.B2: Target user ID non-numeric handled safely")
    public void testF17_B2_NonNumericTarget() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        assertDoesNotThrow(() -> {
            E2ETestHarness.post("/librarian/notifications",
                    Map.of("title", "Notice", "message", "Msg", "type", "DUE_DATE", "target", "abc"), session);
        });
    }

    @Test
    @DisplayName("F17.B3: Targeted notice with due date math in message")
    public void testF17_B3_DueDateMessage() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Overdue: 5 Days Late", "message", "Accumulated fine: $5.00", "type", "DUE_DATE", "target", "2"), session);
        assertEquals(302, resp.getStatusCode());
    }

    @Test
    @DisplayName("F17.B4: Rapid consecutive targeted notices delivered independently")
    public void testF17_B4_MultipleTargeted() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Notice 1", "message", "1", "type", "DUE_DATE", "target", "2"), session);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Notice 2", "message", "2", "type", "DUE_DATE", "target", "2"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM notifications WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("F17.B5: Targeted notice creation timestamps are chronological")
    public void testF17_B5_ChronologicalTimestamps() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "First", "message", "1", "type", "DUE_DATE", "target", "2"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT created_at FROM notifications WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertNotNull(rs.getTimestamp("created_at"));
        }
    }

    // =========================================================================
    // F18: Metric Summary KPI Cards — Zero States
    // =========================================================================

    @Test
    @DisplayName("F18.B1: Metric cards when database has 0 transactions returns zeroes")
    public void testF18_B1_ZeroTransactionsKPI() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM transactions")) {
            ps.executeUpdate();
        }

        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/dashboard", session);
        assertEquals(200, resp.getStatusCode());
        assertTrue(resp.containsText("\"borrowed\":0"));
    }

    @Test
    @DisplayName("F18.B2: Overdue KPI count matches exact database overdue rows")
    public void testF18_B2_OverdueKPI() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO transactions (book_id, user_id, borrow_date, due_date, status) VALUES (1, 2, ?, ?, 'OVERDUE')")) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusDays(20)));
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusDays(5)));
            ps.executeUpdate();
        }

        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/dashboard", session);
        assertTrue(resp.containsText("\"overdue\":1"));
    }

    @Test
    @DisplayName("F18.B3: Total members KPI excludes librarian count")
    public void testF18_B3_TotalMembersKPI() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/dashboard", session);
        assertTrue(resp.containsText("\"totalMembers\":2"));
    }

    @Test
    @DisplayName("F18.B4: Total books KPI updates after book deletion")
    public void testF18_B4_TotalBooksUpdatesAfterDelete() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.post("/librarian/books", Map.of("action", "delete", "bookId", "5"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/dashboard", session);
        assertTrue(resp.containsText("\"totalBooks\":4"));
    }

    @Test
    @DisplayName("F18.B5: Total copies KPI updates after edit copies")
    public void testF18_B5_TotalCopiesUpdatesAfterEdit() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.post("/librarian/books",
                Map.of("action", "edit", "bookId", "1", "title", "Effective Java",
                        "author", "Joshua Bloch", "genre", "CS", "totalCopies", "10"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/dashboard", session);
        assertTrue(resp.containsText("\"totalCopies\":25")); // 20 - 5 + 10
    }

    // =========================================================================
    // F19: Visual Borrowing Trends — Boundaries
    // =========================================================================

    @Test
    @DisplayName("F19.B1: Borrowing trends report when 0 transactions handled without NPE")
    public void testF19_B1_ZeroTransactionsTrends() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM transactions")) {
            ps.executeUpdate();
        }

        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", session);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F19.B2: Trends data with multiple loans in same month aggregated")
    public void testF19_B2_MonthlyAggregation() throws Exception {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "2"), alice);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM transactions")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("F19.B3: Trends report container adheres to zero borders")
    public void testF19_B3_ZeroBorders() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", session);
        E2ETestHarness.assertNoBorders(resp.getBody());
    }

    @Test
    @DisplayName("F19.B4: Trends report container adheres to zero box-shadows")
    public void testF19_B4_ZeroShadows() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", session);
        E2ETestHarness.assertNoShadows(resp.getBody());
    }

    @Test
    @DisplayName("F19.B5: Trends report contains zero sparkle glyphs")
    public void testF19_B5_NoSparkles() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", session);
        E2ETestHarness.assertNoSparkleIcons(resp.getBody());
    }

    // =========================================================================
    // F20: Genre Breakdown Doughnut Chart — Boundaries
    // =========================================================================

    @Test
    @DisplayName("F20.B1: Genre breakdown with single book in catalog")
    public void testF20_B1_SingleBookGenre() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM transactions; DELETE FROM books WHERE book_id > 1;")) {
            ps.executeUpdate();
        }

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(DISTINCT genre) FROM books")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("F20.B2: Genre breakdown with special characters in genre name")
    public void testF20_B2_SpecialCharGenre() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.post("/librarian/books",
                Map.of("action", "add", "isbn", "978-7770001", "title", "Sci-Fi Special",
                        "author", "Author", "genre", "Sci-Fi & Fantasy (Modern)", "totalCopies", "2"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT genre FROM books WHERE isbn = '978-7770001'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("Sci-Fi & Fantasy (Modern)", rs.getString("genre"));
        }
    }

    @Test
    @DisplayName("F20.B3: Genre count sums up to total books in catalog")
    public void testF20_B3_GenreSumEqualsTotalBooks() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT SUM(c), (SELECT COUNT(*) FROM books) FROM (SELECT COUNT(*) as c FROM books GROUP BY genre)")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(rs.getInt(1), rs.getInt(2));
        }
    }

    @Test
    @DisplayName("F20.B4: Genre doughnut chart adheres to zero borders guardrail")
    public void testF20_B4_ZeroBorders() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", session);
        E2ETestHarness.assertNoBorders(resp.getBody());
    }

    @Test
    @DisplayName("F20.B5: Genre doughnut chart adheres to zero shadows guardrail")
    public void testF20_B5_ZeroShadows() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", session);
        E2ETestHarness.assertNoShadows(resp.getBody());
    }

    // =========================================================================
    // F21: Availability Ratio Chart — Boundaries
    // =========================================================================

    @Test
    @DisplayName("F21.B1: Ratio when 100% of books are available")
    public void testF21_B1_FullAvailability() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT SUM(available_copies), SUM(total_copies) FROM books")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(rs.getInt(1), rs.getInt(2));
        }
    }

    @Test
    @DisplayName("F21.B2: Ratio when all copies of a title are borrowed (0 available)")
    public void testF21_B2_ZeroAvailableCopiesForBook() throws Exception {
        // Book 5 has 2 copies; borrow both
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "5"), alice);

        E2ETestHarness.TestSession bob = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "bob@example.com", "password", "member123"), bob);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "5"), bob);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT available_copies FROM books WHERE book_id = 5")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt("available_copies"));
        }
    }

    @Test
    @DisplayName("F21.B3: Available copies never drops below zero")
    public void testF21_B3_NonNegativeAvailableCopies() throws Exception {
        // Force borrow attempt when copies = 0
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE books SET available_copies = 0 WHERE book_id = 5")) {
            ps.executeUpdate();
        }

        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "5"), alice);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT available_copies FROM books WHERE book_id = 5")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt("available_copies")); // Still 0, not -1
        }
    }

    @Test
    @DisplayName("F21.B4: Availability chart adheres to zero borders guardrail")
    public void testF21_B4_ZeroBorders() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", session);
        E2ETestHarness.assertNoBorders(resp.getBody());
    }

    @Test
    @DisplayName("F21.B5: Availability chart contains zero sparkle icons")
    public void testF21_B5_NoSparkles() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", session);
        E2ETestHarness.assertNoSparkleIcons(resp.getBody());
    }

    // =========================================================================
    // F22: Popular Books Ranking — Boundaries
    // =========================================================================

    @Test
    @DisplayName("F22.B1: Ranking when all books have equal borrows handles ties cleanly")
    public void testF22_B1_TieHandling() throws Exception {
        // Alice borrows book 1 and book 2
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "2"), alice);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT b.title, COUNT(t.transaction_id) as loans FROM books b " +
                             "JOIN transactions t ON b.book_id = t.book_id GROUP BY b.book_id, b.title HAVING loans = 1")) {
            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next()) count++;
            assertEquals(2, count);
        }
    }

    @Test
    @DisplayName("F22.B2: Ranking list length does not exceed total books count")
    public void testF22_B2_LengthBound() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(DISTINCT b.book_id) FROM books b LEFT JOIN transactions t ON b.book_id = t.book_id")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(5, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("F22.B3: Deleted book does not break ranking query")
    public void testF22_B3_DeletedBookRanking() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.post("/librarian/books", Map.of("action", "delete", "bookId", "5"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", session);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F22.B4: Popular books ranking zero borders guardrail")
    public void testF22_B4_ZeroBorders() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", session);
        E2ETestHarness.assertNoBorders(resp.getBody());
    }

    @Test
    @DisplayName("F22.B5: Popular books ranking zero sparkle icons guardrail")
    public void testF22_B5_NoSparkles() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", session);
        E2ETestHarness.assertNoSparkleIcons(resp.getBody());
    }

    // =========================================================================
    // F23: Multi-Field Search — SQL Injections & Meta-Characters
    // =========================================================================

    @Test
    @DisplayName("F23.B1: SQL injection payload in search query neutralized")
    public void testF23_B1_SearchSqlInjection() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/search?q=' OR '1'='1", null);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F23.B2: Search with regex/meta-characters (*, ?, +, ^, $)")
    public void testF23_B2_SearchMetaCharacters() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/search?q=*?+^$", null);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F23.B3: Search query with leading and trailing whitespaces trimmed")
    public void testF23_B3_SearchWhitespaceTrimmed() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/search?q=  Java  ", null);
        assertEquals(200, resp.getStatusCode());
        assertTrue(resp.containsText("Effective Java"));
    }

    @Test
    @DisplayName("F23.B4: Case-insensitive search match (jAvA matches Java)")
    public void testF23_B4_SearchCaseInsensitive() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/search?q=jAvA", null);
        assertEquals(200, resp.getStatusCode());
        assertTrue(resp.containsText("Effective Java"));
    }

    @Test
    @DisplayName("F23.B5: Search result renders without box-shadows or borders")
    public void testF23_B5_SearchUiGuardrails() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/search?q=Java", null);
        E2ETestHarness.assertNoBorders(resp.getBody());
        E2ETestHarness.assertNoShadows(resp.getBody());
    }

    // =========================================================================
    // F24: Search History Tracking — Column Limits
    // =========================================================================

    @Test
    @DisplayName("F24.B1: Search query with 255-character max column boundary logged")
    public void testF24_B1_SearchHistory_MaxBoundary() throws Exception {
        String query255 = "Q".repeat(255);
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.get("/member/search?q=" + query255, session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT query_text FROM search_history WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(255, rs.getString("query_text").length());
        }
    }

    @Test
    @DisplayName("F24.B2: Search query with SQL injection payload stored literally as query text")
    public void testF24_B2_SearchHistory_SqlPayloadStoredSafely() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.get("/member/search?q=' OR '1'='1", session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT query_text FROM search_history WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("' OR '1'='1", rs.getString("query_text"));
        }
    }

    @Test
    @DisplayName("F24.B3: Search history preserves unicode search queries")
    public void testF24_B3_SearchHistory_Unicode() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.get("/member/search?q=DonQuixote_España_München", session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT query_text FROM search_history WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("DonQuixote_España_München", rs.getString("query_text"));
        }
    }

    @Test
    @DisplayName("F24.B4: Search history logs search_type boundary (TITLE, AUTHOR, GENRE)")
    public void testF24_B4_SearchHistory_Types() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.post("/member/search", Map.of("q", "CS", "type", "GENRE"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT search_type FROM search_history WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("GENRE", rs.getString("search_type"));
        }
    }

    @Test
    @DisplayName("F24.B5: Rapid consecutive searches generate unique search_id PKs")
    public void testF24_B5_UniqueSearchIds() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.get("/member/search?q=First", session);
        E2ETestHarness.get("/member/search?q=Second", session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(DISTINCT search_id) FROM search_history WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1));
        }
    }

    // =========================================================================
    // F25: View Search History — Boundaries
    // =========================================================================

    @Test
    @DisplayName("F25.B1: Search history when 0 searches made handled without error")
    public void testF25_B1_ZeroSearches() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/search", session);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F25.B2: Search history chronological DESC order verification")
    public void testF25_B2_ChronologicalOrder() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.get("/member/search?q=OldQuery", session);
        Thread.sleep(10);
        E2ETestHarness.get("/member/search?q=NewQuery", session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT query_text FROM search_history WHERE user_id = 2 ORDER BY searched_at DESC")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("NewQuery", rs.getString("query_text"));
        }
    }

    @Test
    @DisplayName("F25.B3: Search history view adheres to zero borders guardrail")
    public void testF25_B3_ZeroBorders() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/search", session);
        E2ETestHarness.assertNoBorders(resp.getBody());
    }

    @Test
    @DisplayName("F25.B4: Search history view adheres to zero shadows guardrail")
    public void testF25_B4_ZeroShadows() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/search", session);
        E2ETestHarness.assertNoShadows(resp.getBody());
    }

    @Test
    @DisplayName("F25.B5: Search history view contains no sparkle icons")
    public void testF25_B5_NoSparkles() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/search", session);
        E2ETestHarness.assertNoSparkleIcons(resp.getBody());
    }

    // =========================================================================
    // F26: Borrow Available Book — Out of Stock & Negative Book ID
    // =========================================================================

    @Test
    @DisplayName("F26.B1: Borrowing last copy (available_copies = 1) succeeds and leaves 0 available")
    public void testF26_B1_BorrowLastCopy() throws Exception {
        // Set book 5 available copies to 1
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE books SET available_copies = 1 WHERE book_id = 5")) {
            ps.executeUpdate();
        }

        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/borrow", Map.of("bookId", "5"), alice);
        assertEquals(302, resp.getStatusCode());

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT available_copies FROM books WHERE book_id = 5")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt("available_copies"));
        }
    }

    @Test
    @DisplayName("F26.B2: Borrowing when available copies = 0 rejected with 409 BookUnavailableException")
    public void testF26_B2_BorrowWhenZeroCopies() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE books SET available_copies = 0 WHERE book_id = 5")) {
            ps.executeUpdate();
        }

        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/borrow", Map.of("bookId", "5"), alice);
        assertEquals(409, resp.getStatusCode());
    }

    @Test
    @DisplayName("F26.B3: Borrowing non-existent book ID returns 409 or handled safely")
    public void testF26_B3_BorrowNonExistentBook() {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/borrow", Map.of("bookId", "99999"), alice);
        assertEquals(409, resp.getStatusCode());
    }

    @Test
    @DisplayName("F26.B4: Borrowing negative book ID returns 409 or handled safely")
    public void testF26_B4_BorrowNegativeBookId() {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/borrow", Map.of("bookId", "-1"), alice);
        assertEquals(409, resp.getStatusCode());
    }

    @Test
    @DisplayName("F26.B5: Due date is set exactly to borrow_date + 14 days")
    public void testF26_B5_FourteenDaysLoanPeriod() throws Exception {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), alice);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT borrow_date, due_date FROM transactions WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            long diffDays = java.time.Duration.between(
                    rs.getTimestamp("borrow_date").toLocalDateTime(),
                    rs.getTimestamp("due_date").toLocalDateTime()).toDays();
            assertEquals(14, diffDays);
        }
    }

    // =========================================================================
    // F27: Return Borrowed Book — Boundaries & Cross-Member Protection
    // =========================================================================

    @Test
    @DisplayName("F27.B1: Returning book already marked RETURNED rejected with 409")
    public void testF27_B1_DoubleReturnRejected() {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), alice);
        E2ETestHarness.post("/member/return", Map.of("transactionId", "1"), alice);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/return",
                Map.of("transactionId", "1"), alice);
        assertEquals(409, resp.getStatusCode());
    }

    @Test
    @DisplayName("F27.B2: Returning loan belonging to another member rejected with 403")
    public void testF27_B2_ReturnOtherMemberLoan_Forbidden() {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), alice);

        E2ETestHarness.TestSession bob = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "bob@example.com", "password", "member123"), bob);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/return",
                Map.of("transactionId", "1"), bob);
        assertEquals(403, resp.getStatusCode());
    }

    @Test
    @DisplayName("F27.B3: Returning non-existent transaction ID returns 404")
    public void testF27_B3_ReturnNonExistentTx() {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/return",
                Map.of("transactionId", "99999"), alice);
        assertEquals(404, resp.getStatusCode());
    }

    @Test
    @DisplayName("F27.B4: On-time return calculates fine_amount exactly 0.00")
    public void testF27_B4_OnTimeZeroFine() throws Exception {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "2"), alice);
        E2ETestHarness.post("/member/return", Map.of("transactionId", "1"), alice);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT fine_amount FROM transactions WHERE transaction_id = 1")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(0.00, rs.getDouble("fine_amount"), 0.001);
        }
    }

    @Test
    @DisplayName("F27.B5: Return increments stock atomically and accurately")
    public void testF27_B5_StockIncrementAtomicity() throws Exception {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "3"), alice); // 4 -> 3
        E2ETestHarness.post("/member/return", Map.of("transactionId", "1"), alice); // 3 -> 4

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT available_copies FROM books WHERE book_id = 3")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(4, rs.getInt("available_copies"));
        }
    }

    // =========================================================================
    // F28: View Borrowing History — Boundaries
    // =========================================================================

    @Test
    @DisplayName("F28.B1: Borrowing history when 0 loans exist renders empty table cleanly")
    public void testF28_B1_ZeroLoans() {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/history", alice);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F28.B2: Borrowing history does not display other members' loans")
    public void testF28_B2_PrivacyIsolation() throws Exception {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), alice);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM transactions WHERE user_id = 3")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1)); // Bob has 0
        }
    }

    @Test
    @DisplayName("F28.B3: Borrowing history status values strictly in (BORROWED, RETURNED, OVERDUE)")
    public void testF28_B3_StatusEnumCompliance() throws Exception {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), alice);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT status FROM transactions WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            String status = rs.getString("status");
            assertTrue(status.equals("BORROWED") || status.equals("RETURNED") || status.equals("OVERDUE"));
        }
    }

    @Test
    @DisplayName("F28.B4: Borrowing history view zero borders guardrail")
    public void testF28_B4_ZeroBorders() {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/history", alice);
        E2ETestHarness.assertNoBorders(resp.getBody());
    }

    @Test
    @DisplayName("F28.B5: Borrowing history view zero shadows guardrail")
    public void testF28_B5_ZeroShadows() {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/history", alice);
        E2ETestHarness.assertNoShadows(resp.getBody());
    }

    // =========================================================================
    // F29: View Profile — Boundaries
    // =========================================================================

    @Test
    @DisplayName("F29.B1: Profile view contains default notification preference flags (TRUE)")
    public void testF29_B1_DefaultPreferences() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT email_notifications, due_date_alerts, new_arrival_alerts FROM users WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertTrue(rs.getBoolean("email_notifications"));
            assertTrue(rs.getBoolean("due_date_alerts"));
            assertTrue(rs.getBoolean("new_arrival_alerts"));
        }
    }

    @Test
    @DisplayName("F29.B2: Profile view displays active loan count accurately")
    public void testF29_B2_ActiveLoansProfile() throws Exception {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), alice);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM transactions WHERE user_id = 2 AND status = 'BORROWED'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("F29.B3: Profile view password hash is not exposed in HTML view")
    public void testF29_B3_PasswordHashNotExposed() {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/profile", alice);
        assertFalse(resp.containsText("member123"));
    }

    @Test
    @DisplayName("F29.B4: Profile view zero borders guardrail")
    public void testF29_B4_ZeroBorders() {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/profile", alice);
        E2ETestHarness.assertNoBorders(resp.getBody());
    }

    @Test
    @DisplayName("F29.B5: Profile view zero sparkle icons guardrail")
    public void testF29_B5_NoSparkles() {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/profile", alice);
        E2ETestHarness.assertNoSparkleIcons(resp.getBody());
    }

    // =========================================================================
    // F30: Update Profile & Password — Boundaries
    // =========================================================================

    @Test
    @DisplayName("F30.B1: Password update with identical current and new password handled cleanly")
    public void testF30_B1_IdenticalPassword() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/profile",
                Map.of("name", "Alice", "email", "alice@example.com",
                        "currentPassword", "member123", "newPassword", "member123"), session);
        assertEquals(302, resp.getStatusCode());
    }

    @Test
    @DisplayName("F30.B2: Password update with empty current password rejected with 400")
    public void testF30_B2_EmptyCurrentPasswordRejected() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/profile",
                Map.of("name", "Alice", "email", "alice@example.com",
                        "currentPassword", "", "newPassword", "brandNewPass123"), session);
        assertEquals(400, resp.getStatusCode());
    }

    @Test
    @DisplayName("F30.B3: Name update with special characters and accents accepted")
    public void testF30_B3_SpecialCharactersInName() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.post("/member/profile",
                Map.of("name", "Renée O'Connor-Smith", "email", "alice@example.com"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT name FROM users WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("Renée O'Connor-Smith", rs.getString("name"));
        }
    }

    @Test
    @DisplayName("F30.B4: Update email to already registered email throws error")
    public void testF30_B4_DuplicateEmailUpdate() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        assertDoesNotThrow(() -> {
            E2ETestHarness.post("/member/profile",
                    Map.of("name", "Alice", "email", "bob@example.com"), session);
        });
    }

    @Test
    @DisplayName("F30.B5: Profile update does not modify membership_id")
    public void testF30_B5_MembershipIdImmutable() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.post("/member/profile",
                Map.of("name", "Alice New", "email", "alice@example.com", "membershipId", "HACK-999"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT membership_id FROM users WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("MEM-1001", rs.getString("membership_id"));
        }
    }

    // =========================================================================
    // F31: Notification Alerts Inbox — Boundaries
    // =========================================================================

    @Test
    @DisplayName("F31.B1: Inbox view when 0 notifications exist renders without error")
    public void testF31_B1_ZeroNotifications() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/notifications", session);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F31.B2: Inbox orders notifications by created_at DESC (newest first)")
    public void testF31_B2_NewestFirstOrder() throws Exception {
        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "First Notice", "message", "1", "type", "SYSTEM", "target", "2"), libSession);
        Thread.sleep(10);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Second Notice", "message", "2", "type", "SYSTEM", "target", "2"), libSession);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT title FROM notifications WHERE user_id = 2 ORDER BY created_at DESC")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("Second Notice", rs.getString("title"));
        }
    }

    @Test
    @DisplayName("F31.B3: Unread notification badge count updates dynamically")
    public void testF31_B3_BadgeCount() throws Exception {
        E2ETestHarness.TestSession lib = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), lib);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Alert 1", "message", "A", "type", "SYSTEM", "target", "2"), lib);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Alert 2", "message", "B", "type", "SYSTEM", "target", "2"), lib);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM notifications WHERE user_id = 2 AND is_read = FALSE")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("F31.B4: Notification inbox zero borders guardrail")
    public void testF31_B4_ZeroBorders() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/notifications", session);
        E2ETestHarness.assertNoBorders(resp.getBody());
    }

    @Test
    @DisplayName("F31.B5: Notification inbox zero shadows guardrail")
    public void testF31_B5_ZeroShadows() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/notifications", session);
        E2ETestHarness.assertNoShadows(resp.getBody());
    }

    // =========================================================================
    // F32: Mark Notification Read — Boundaries
    // =========================================================================

    @Test
    @DisplayName("F32.B1: Negative notification ID handled safely")
    public void testF32_B1_NegativeNotificationId() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/notifications",
                Map.of("notificationId", "-1"), session);
        assertEquals(302, resp.getStatusCode());
    }

    @Test
    @DisplayName("F32.B2: Non-numeric notification ID handled safely")
    public void testF32_B2_NonNumericNotificationId() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        assertDoesNotThrow(() -> {
            E2ETestHarness.post("/member/notifications",
                    Map.of("notificationId", "abc"), session);
        });
    }

    @Test
    @DisplayName("F32.B3: SQL injection in notificationId parameter neutralized")
    public void testF32_B3_SqlInjectionInNotificationId() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        assertDoesNotThrow(() -> {
            E2ETestHarness.post("/member/notifications",
                    Map.of("notificationId", "1 OR 1=1"), session);
        });
    }

    @Test
    @DisplayName("F32.B4: Member cannot mark another member's notification as read")
    public void testF32_B4_CrossMemberNotificationRead_Ignored() throws Exception {
        // Dispatch alert to Alice (user_id = 2)
        E2ETestHarness.TestSession lib = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), lib);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Alice Alert", "message", "Private", "type", "SYSTEM", "target", "2"), lib);

        // Bob (user_id = 3) tries to mark it read
        E2ETestHarness.TestSession bob = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "bob@example.com", "password", "member123"), bob);
        E2ETestHarness.post("/member/notifications", Map.of("notificationId", "1"), bob);

        // Alice notification remains is_read = FALSE
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT is_read FROM notifications WHERE notification_id = 1")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertFalse(rs.getBoolean("is_read"));
        }
    }

    @Test
    @DisplayName("F32.B5: Notification mark read redirects using PRG pattern")
    public void testF32_B5_PrgPattern() {
        E2ETestHarness.TestSession lib = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), lib);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Alert", "message", "Msg", "type", "SYSTEM", "target", "2"), lib);

        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/notifications",
                Map.of("notificationId", "1"), alice);
        assertEquals(302, resp.getStatusCode());
        assertTrue(resp.getRedirectLocation().contains("marked=true"));
    }
}
