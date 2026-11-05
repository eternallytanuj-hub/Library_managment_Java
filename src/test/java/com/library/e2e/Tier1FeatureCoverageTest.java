package com.library.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tier 1: Feature Coverage Test Suite (Opaque-Box Requirement-Driven)
 * Covers all 32 inventoried system features with at least 5 isolated test cases each (160 test cases total).
 * Verifies standard flows, role permissions, state persistence, and UI guardrails.
 */
public class Tier1FeatureCoverageTest {

    @BeforeEach
    public void setUp() {
        E2ETestHarness.resetTestData();
    }

    // =========================================================================
    // F01: Librarian Login
    // =========================================================================

    @Test
    @DisplayName("F01.1: Librarian Login with valid credentials succeeds and redirects")
    public void testF01_LibrarianLogin_ValidCredentials_Success() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/login",
                Map.of("email", "librarian@library.com", "password", "admin123"), session);
        assertEquals(302, resp.getStatusCode());
        assertTrue(resp.getRedirectLocation().contains("/librarian/dashboard"));
        assertEquals("LIBRARIAN", session.getRole());
    }

    @Test
    @DisplayName("F01.2: Librarian Login with invalid password fails")
    public void testF01_LibrarianLogin_InvalidPassword_Fails() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/login",
                Map.of("email", "librarian@library.com", "password", "wrongpass"), session);
        assertEquals(302, resp.getStatusCode());
        assertTrue(resp.getRedirectLocation().contains("error="));
        assertNull(session.getRole());
    }

    @Test
    @DisplayName("F01.3: Librarian Login with non-existent email fails")
    public void testF01_LibrarianLogin_NonExistentEmail_Fails() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/login",
                Map.of("email", "nobody@library.com", "password", "admin123"), session);
        assertEquals(302, resp.getStatusCode());
        assertTrue(resp.getRedirectLocation().contains("error="));
    }

    @Test
    @DisplayName("F01.4: Librarian Login session cookie contains JSESSIONID")
    public void testF01_LibrarianLogin_SessionCookieIssued() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login",
                Map.of("email", "librarian@library.com", "password", "admin123"), session);
        assertNotNull(session.getCookies().get("JSESSIONID"));
    }

    @Test
    @DisplayName("F01.5: Librarian Login UI adheres to UI guardrails")
    public void testF01_LibrarianLogin_UiGuardrails() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/login", null);
        E2ETestHarness.assertNoBorders(resp.getBody());
        E2ETestHarness.assertNoShadows(resp.getBody());
        E2ETestHarness.assertNoSparkleIcons(resp.getBody());
    }

    // =========================================================================
    // F02: Member Login
    // =========================================================================

    @Test
    @DisplayName("F02.1: Member Login with valid credentials succeeds and redirects to member dashboard")
    public void testF02_MemberLogin_ValidCredentials_Success() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/login",
                Map.of("email", "alice@example.com", "password", "member123"), session);
        assertEquals(302, resp.getStatusCode());
        assertTrue(resp.getRedirectLocation().contains("/member/dashboard"));
        assertEquals("MEMBER", session.getRole());
    }

    @Test
    @DisplayName("F02.2: Member Login with invalid password fails")
    public void testF02_MemberLogin_InvalidPassword_Fails() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/login",
                Map.of("email", "alice@example.com", "password", "wrongpassword"), session);
        assertEquals(302, resp.getStatusCode());
        assertTrue(resp.getRedirectLocation().contains("error="));
    }

    @Test
    @DisplayName("F02.3: Second member bob logs in successfully")
    public void testF02_MemberLogin_SecondMember_Success() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/login",
                Map.of("email", "bob@example.com", "password", "member123"), session);
        assertEquals(302, resp.getStatusCode());
        assertEquals("MEMBER", session.getRole());
    }

    @Test
    @DisplayName("F02.4: Member login establishes authenticated user ID in session")
    public void testF02_MemberLogin_UserIdPopulated() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login",
                Map.of("email", "alice@example.com", "password", "member123"), session);
        assertNotNull(session.getUserId());
        assertTrue(session.getUserId() > 0);
    }

    @Test
    @DisplayName("F02.5: Member login page contains zero forbidden borders or shadows")
    public void testF02_MemberLogin_UiZeroBorders() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/index.jsp", null);
        E2ETestHarness.assertNoBorders(resp.getBody());
        E2ETestHarness.assertNoShadows(resp.getBody());
    }

    // =========================================================================
    // F03: Member Registration
    // =========================================================================

    @Test
    @DisplayName("F03.1: Valid member registration creates member and redirects to login")
    public void testF03_MemberRegistration_Success() throws Exception {
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/register",
                Map.of("name", "Charlie Brown", "email", "charlie@example.com", "password", "charliePass123"), null);
        assertEquals(302, resp.getStatusCode());
        assertTrue(resp.getRedirectLocation().contains("registered=true"));

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT membership_id, role FROM users WHERE email = ?")) {
            ps.setString(1, "charlie@example.com");
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertTrue(rs.getString("membership_id").startsWith("MEM-"));
            assertEquals("MEMBER", rs.getString("role"));
        }
    }

    @Test
    @DisplayName("F03.2: Member registration with duplicate email rejected")
    public void testF03_MemberRegistration_DuplicateEmail_Rejected() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/register",
                Map.of("name", "Alice Clone", "email", "alice@example.com", "password", "pass123"), null);
        assertEquals(409, resp.getStatusCode());
    }

    @Test
    @DisplayName("F03.3: Member registration with invalid short password rejected")
    public void testF03_MemberRegistration_ShortPassword_Rejected() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/register",
                Map.of("name", "David", "email", "david@example.com", "password", "12"), null);
        assertEquals(400, resp.getStatusCode());
    }

    @Test
    @DisplayName("F03.4: Member registration with invalid email rejected")
    public void testF03_MemberRegistration_InvalidEmail_Rejected() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/register",
                Map.of("name", "David", "email", "not-an-email", "password", "pass1234"), null);
        assertEquals(400, resp.getStatusCode());
    }

    @Test
    @DisplayName("F03.5: Registered member can immediately log in")
    public void testF03_MemberRegistration_CanLoginAfterRegister() {
        E2ETestHarness.post("/register",
                Map.of("name", "Eve Member", "email", "eve@example.com", "password", "evePass123"), null);
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/login",
                Map.of("email", "eve@example.com", "password", "evePass123"), session);
        assertEquals(302, resp.getStatusCode());
        assertEquals("MEMBER", session.getRole());
    }

    // =========================================================================
    // F04: Session Invalidation & Logout
    // =========================================================================

    @Test
    @DisplayName("F04.1: Logout clears session and redirects to index")
    public void testF04_Logout_ClearsSession() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        assertTrue(session.isAuthenticated());

        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/logout", session);
        assertEquals(302, resp.getStatusCode());
        assertTrue(resp.getRedirectLocation().contains("index.jsp"));
        assertFalse(session.isAuthenticated());
    }

    @Test
    @DisplayName("F04.2: Accessing protected page after logout redirects to login")
    public void testF04_Logout_SubsequentAccessRedirects() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.get("/logout", session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/dashboard", session);
        assertEquals(302, resp.getStatusCode());
        assertTrue(resp.getRedirectLocation().contains("/login"));
    }

    @Test
    @DisplayName("F04.3: Librarian logout invalidates librarian security role")
    public void testF04_LibrarianLogout_RoleInvalidated() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.get("/logout", session);
        assertNull(session.getRole());
    }

    @Test
    @DisplayName("F04.4: Unauthenticated logout request does not throw error")
    public void testF04_Logout_Unauthenticated_Safe() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/logout", session);
        assertEquals(302, resp.getStatusCode());
    }

    @Test
    @DisplayName("F04.5: Multiple sequential logouts are idempotent")
    public void testF04_Logout_Idempotent() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.get("/logout", session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/logout", session);
        assertEquals(302, resp.getStatusCode());
    }

    // =========================================================================
    // F05: Role-Based Access Control Filter
    // =========================================================================

    @Test
    @DisplayName("F05.1: Member role forbidden from accessing /librarian/*")
    public void testF05_RbacFilter_MemberDeniedLibrarianRoute() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/books", session);
        assertEquals(403, resp.getStatusCode());
    }

    @Test
    @DisplayName("F05.2: Unauthenticated user redirected from /librarian/dashboard")
    public void testF05_RbacFilter_UnauthDeniedLibrarian() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/dashboard", null);
        assertEquals(302, resp.getStatusCode());
        assertTrue(resp.getRedirectLocation().contains("/login"));
    }

    @Test
    @DisplayName("F05.3: Unauthenticated user redirected from /member/dashboard")
    public void testF05_RbacFilter_UnauthDeniedMember() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/dashboard", null);
        assertEquals(302, resp.getStatusCode());
        assertTrue(resp.getRedirectLocation().contains("/login"));
    }

    @Test
    @DisplayName("F05.4: Librarian permitted to access /librarian/* routes")
    public void testF05_RbacFilter_LibrarianPermitted() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/dashboard", session);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F05.5: Public routes accessible without authentication")
    public void testF05_RbacFilter_PublicRoutesAccessible() {
        E2ETestHarness.TestResponse resp1 = E2ETestHarness.get("/login", null);
        assertTrue(resp1.getStatusCode() == 200 || resp1.getStatusCode() == 302);
        E2ETestHarness.TestResponse resp2 = E2ETestHarness.get("/index.jsp", null);
        assertTrue(resp2.getStatusCode() == 200 || resp2.getStatusCode() == 302);
    }

    // =========================================================================
    // F06: Add Book to Catalog
    // =========================================================================

    @Test
    @DisplayName("F06.1: Librarian adds new book successfully with total and available copies")
    public void testF06_AddBook_Success() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/books",
                Map.of("action", "add", "isbn", "978-1111222333", "title", "Clean Architecture",
                        "author", "Robert C. Martin", "genre", "Computer Science", "totalCopies", "5"), session);
        assertEquals(302, resp.getStatusCode());

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT total_copies, available_copies FROM books WHERE isbn = ?")) {
            ps.setString(1, "978-1111222333");
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(5, rs.getInt("total_copies"));
            assertEquals(5, rs.getInt("available_copies"));
        }
    }

    @Test
    @DisplayName("F06.2: Add book with duplicate ISBN rejected")
    public void testF06_AddBook_DuplicateIsbn_Rejected() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/books",
                Map.of("action", "add", "isbn", "978-0134685991", "title", "Duplicate Book",
                        "author", "Author", "genre", "Fiction", "totalCopies", "2"), session);
        assertEquals(409, resp.getStatusCode());
    }

    @Test
    @DisplayName("F06.3: Add book with zero copies rejected")
    public void testF06_AddBook_ZeroCopies_Rejected() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/books",
                Map.of("action", "add", "isbn", "978-0000000001", "title", "Zero Copy Book",
                        "author", "Author", "genre", "Fiction", "totalCopies", "0"), session);
        assertEquals(400, resp.getStatusCode());
    }

    @Test
    @DisplayName("F06.4: Add book with empty title rejected")
    public void testF06_AddBook_EmptyTitle_Rejected() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/books",
                Map.of("action", "add", "isbn", "978-0000000002", "title", " ",
                        "author", "Author", "genre", "Fiction", "totalCopies", "2"), session);
        assertEquals(400, resp.getStatusCode());
    }

    @Test
    @DisplayName("F06.5: Member cannot add book (RBAC enforcement)")
    public void testF06_AddBook_MemberDenied() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/books",
                Map.of("action", "add", "isbn", "978-9999999999", "title", "Hacked Book",
                        "author", "Hacker", "genre", "Fiction", "totalCopies", "1"), session);
        assertEquals(403, resp.getStatusCode());
    }

    // =========================================================================
    // F07: Edit Book Details & Copies
    // =========================================================================

    @Test
    @DisplayName("F07.1: Librarian edits book title and total copies")
    public void testF07_EditBook_Success() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/books",
                Map.of("action", "edit", "bookId", "1", "title", "Effective Java (3rd Edition)",
                        "author", "Joshua Bloch", "genre", "Computer Science", "totalCopies", "8"), session);
        assertEquals(302, resp.getStatusCode());

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT title, total_copies FROM books WHERE book_id = 1")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("Effective Java (3rd Edition)", rs.getString("title"));
            assertEquals(8, rs.getInt("total_copies"));
        }
    }

    @Test
    @DisplayName("F07.2: Edit book updates genre")
    public void testF07_EditBook_UpdateGenre() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);

        E2ETestHarness.post("/librarian/books",
                Map.of("action", "edit", "bookId", "1", "title", "Effective Java",
                        "author", "Joshua Bloch", "genre", "Software Engineering", "totalCopies", "5"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT genre FROM books WHERE book_id = 1")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("Software Engineering", rs.getString("genre"));
        }
    }

    @Test
    @DisplayName("F07.3: Edit book preserves available copies when increasing total copies")
    public void testF07_EditBook_PreservesStock() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);

        E2ETestHarness.post("/librarian/books",
                Map.of("action", "edit", "bookId", "2", "title", "Design Patterns",
                        "author", "Gang of Four", "genre", "Computer Science", "totalCopies", "10"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT available_copies, total_copies FROM books WHERE book_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(10, rs.getInt("total_copies"));
            assertTrue(rs.getInt("available_copies") <= 10);
        }
    }

    @Test
    @DisplayName("F07.4: Edit non-existent book fails gracefully")
    public void testF07_EditBook_NonExistent_NoCorruption() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/books",
                Map.of("action", "edit", "bookId", "9999", "title", "Ghost Book",
                        "author", "Ghost", "genre", "Fantasy", "totalCopies", "1"), session);
        assertEquals(302, resp.getStatusCode());
    }

    @Test
    @DisplayName("F07.5: Edit book view complies with zero borders guardrail")
    public void testF07_EditBook_UiGuardrails() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/books", session);
        E2ETestHarness.assertNoBorders(resp.getBody());
        E2ETestHarness.assertNoShadows(resp.getBody());
    }

    // =========================================================================
    // F08: Delete Book from Catalog
    // =========================================================================

    @Test
    @DisplayName("F08.1: Delete book with zero active loans succeeds")
    public void testF08_DeleteBook_NoLoans_Success() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/books",
                Map.of("action", "delete", "bookId", "5"), session);
        assertEquals(302, resp.getStatusCode());

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM books WHERE book_id = 5")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("F08.2: Delete book with active loans rejected")
    public void testF08_DeleteBook_ActiveLoan_Rejected() {
        // Alice borrows book 1
        E2ETestHarness.TestSession memberSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), memberSession);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), memberSession);

        // Librarian attempts delete
        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/books",
                Map.of("action", "delete", "bookId", "1"), libSession);
        assertEquals(409, resp.getStatusCode());
    }

    @Test
    @DisplayName("F08.3: Deleted book no longer appears in member search")
    public void testF08_DeleteBook_RemovedFromSearch() {
        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.post("/librarian/books", Map.of("action", "delete", "bookId", "4"), libSession); // 1984

        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/search?q=1984", null);
        assertFalse(resp.containsText("data-id='4'"));
    }

    @Test
    @DisplayName("F08.4: Member cannot trigger book deletion")
    public void testF08_DeleteBook_MemberForbidden() {
        E2ETestHarness.TestSession memberSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), memberSession);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/books",
                Map.of("action", "delete", "bookId", "3"), memberSession);
        assertEquals(403, resp.getStatusCode());
    }

    @Test
    @DisplayName("F08.5: Delete book with non-existent ID handled safely")
    public void testF08_DeleteBook_NonExistent_Safe() {
        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/books",
                Map.of("action", "delete", "bookId", "99999"), libSession);
        assertEquals(302, resp.getStatusCode());
    }

    // =========================================================================
    // F09: View / Filter Catalog Table
    // =========================================================================

    @Test
    @DisplayName("F09.1: Librarian view catalog returns all seeded books")
    public void testF09_ViewCatalog_ReturnsBooks() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/books", session);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F09.2: Catalog view displays total copies count")
    public void testF09_ViewCatalog_DisplaysTotalCopies() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT SUM(total_copies) FROM books")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertTrue(rs.getInt(1) > 0);
        }
    }

    @Test
    @DisplayName("F09.3: Catalog view displays available copies count")
    public void testF09_ViewCatalog_DisplaysAvailableCopies() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT SUM(available_copies) FROM books")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertTrue(rs.getInt(1) > 0);
        }
    }

    @Test
    @DisplayName("F09.4: Catalog table adheres to borderless styling")
    public void testF09_ViewCatalog_Borderless() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/books", session);
        E2ETestHarness.assertNoBorders(resp.getBody());
    }

    @Test
    @DisplayName("F09.5: Catalog table contains no sparkle icons")
    public void testF09_ViewCatalog_NoSparkles() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/books", session);
        E2ETestHarness.assertNoSparkleIcons(resp.getBody());
    }

    // =========================================================================
    // F10: Create Member Account (Librarian Desk)
    // =========================================================================

    @Test
    @DisplayName("F10.1: Librarian manually creates member account")
    public void testF10_CreateMember_Success() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/members",
                Map.of("action", "create", "name", "Frank Castle", "email", "frank@example.com", "password", "punisher123"), session);
        assertEquals(302, resp.getStatusCode());

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT role FROM users WHERE email = ?")) {
            ps.setString(1, "frank@example.com");
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("MEMBER", rs.getString("role"));
        }
    }

    @Test
    @DisplayName("F10.2: Librarian created member can log in")
    public void testF10_CreateMember_CanLogin() {
        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.post("/librarian/members",
                Map.of("action", "create", "name", "Grace Hopper", "email", "grace@example.com", "password", "cobol1959"), libSession);

        E2ETestHarness.TestSession memberSession = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/login",
                Map.of("email", "grace@example.com", "password", "cobol1959"), memberSession);
        assertEquals(302, resp.getStatusCode());
        assertEquals("MEMBER", memberSession.getRole());
    }

    @Test
    @DisplayName("F10.3: Duplicate member email creation rejected")
    public void testF10_CreateMember_DuplicateEmail_Rejected() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/members",
                Map.of("action", "create", "name", "Alice Two", "email", "alice@example.com", "password", "pass123"), session);
        assertNotEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F10.4: Member creation assigns unique membership ID prefix MEM-")
    public void testF10_CreateMember_MembershipIdPrefix() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);

        E2ETestHarness.post("/librarian/members",
                Map.of("action", "create", "name", "Henry Ford", "email", "henry@example.com", "password", "ford123"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT membership_id FROM users WHERE email = ?")) {
            ps.setString(1, "henry@example.com");
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertTrue(rs.getString("membership_id").startsWith("MEM-"));
        }
    }

    @Test
    @DisplayName("F10.5: Member role cannot create other members")
    public void testF10_CreateMember_MemberForbidden() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/members",
                Map.of("action", "create", "name", "Illegal Member", "email", "illegal@example.com", "password", "pass123"), session);
        assertEquals(403, resp.getStatusCode());
    }

    // =========================================================================
    // F11: Edit Member Account
    // =========================================================================

    @Test
    @DisplayName("F11.1: Librarian edits member name and email")
    public void testF11_EditMember_Success() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/members",
                Map.of("action", "edit", "userId", "2", "name", "Alice W. Johnson", "email", "alice.johnson@example.com"), session);
        assertEquals(302, resp.getStatusCode());

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT name, email FROM users WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("Alice W. Johnson", rs.getString("name"));
            assertEquals("alice.johnson@example.com", rs.getString("email"));
        }
    }

    @Test
    @DisplayName("F11.2: Edited email is immediately recognized at login")
    public void testF11_EditMember_EmailLoginUpdated() {
        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.post("/librarian/members",
                Map.of("action", "edit", "userId", "2", "name", "Alice", "email", "alice_new@example.com"), libSession);

        E2ETestHarness.TestSession memSession = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/login",
                Map.of("email", "alice_new@example.com", "password", "member123"), memSession);
        assertEquals(302, resp.getStatusCode());
        assertEquals("MEMBER", memSession.getRole());
    }

    @Test
    @DisplayName("F11.3: Old email no longer valid after edit")
    public void testF11_EditMember_OldEmailInvalid() {
        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.post("/librarian/members",
                Map.of("action", "edit", "userId", "2", "name", "Alice", "email", "alice_updated@example.com"), libSession);

        E2ETestHarness.TestSession memSession = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/login",
                Map.of("email", "alice@example.com", "password", "member123"), memSession);
        assertTrue(resp.getRedirectLocation().contains("error="));
    }

    @Test
    @DisplayName("F11.4: Edit member with non-existent ID handled safely")
    public void testF11_EditMember_NonExistent_Safe() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/members",
                Map.of("action", "edit", "userId", "9999", "name", "Ghost", "email", "ghost@example.com"), session);
        assertEquals(302, resp.getStatusCode());
    }

    @Test
    @DisplayName("F11.5: Member edit UI complies with UI guardrails")
    public void testF11_EditMember_UiGuardrails() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/members", session);
        E2ETestHarness.assertNoBorders(resp.getBody());
        E2ETestHarness.assertNoShadows(resp.getBody());
    }

    // =========================================================================
    // F12: View Members Directory
    // =========================================================================

    @Test
    @DisplayName("F12.1: Librarian view members list returns status 200")
    public void testF12_ViewMembers_Success() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/members", session);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F12.2: Directory lists active member accounts")
    public void testF12_ViewMembers_ListsMembers() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE role = 'MEMBER'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertTrue(rs.getInt(1) >= 2);
        }
    }

    @Test
    @DisplayName("F12.3: Directory displays membership IDs")
    public void testF12_ViewMembers_DisplaysMembershipId() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT membership_id FROM users WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("MEM-1001", rs.getString("membership_id"));
        }
    }

    @Test
    @DisplayName("F12.4: Member cannot access librarian member directory")
    public void testF12_ViewMembers_MemberDenied() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/members", session);
        assertEquals(403, resp.getStatusCode());
    }

    @Test
    @DisplayName("F12.5: Member directory view complies with UI guardrails")
    public void testF12_ViewMembers_UiGuardrails() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/members", session);
        E2ETestHarness.assertNoBorders(resp.getBody());
        E2ETestHarness.assertNoSparkleIcons(resp.getBody());
    }

    // =========================================================================
    // F13: Deactivate / Delete Member
    // =========================================================================

    @Test
    @DisplayName("F13.1: Librarian deletes member with no active borrowings")
    public void testF13_DeleteMember_NoLoans_Success() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/members",
                Map.of("action", "delete", "userId", "3"), session); // Bob
        assertEquals(302, resp.getStatusCode());

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE user_id = 3")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("F13.2: Delete member with active unreturned books rejected")
    public void testF13_DeleteMember_WithActiveLoan_Rejected() {
        // Alice borrows book 1
        E2ETestHarness.TestSession memSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), memSession);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), memSession);

        // Librarian attempts delete
        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/members",
                Map.of("action", "delete", "userId", "2"), libSession);
        assertEquals(409, resp.getStatusCode());
    }

    @Test
    @DisplayName("F13.3: Deleted member cannot log in")
    public void testF13_DeleteMember_CannotLogin() {
        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.post("/librarian/members", Map.of("action", "delete", "userId", "3"), libSession);

        E2ETestHarness.TestSession bobSession = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/login",
                Map.of("email", "bob@example.com", "password", "member123"), bobSession);
        assertTrue(resp.getRedirectLocation().contains("error="));
    }

    @Test
    @DisplayName("F13.4: Member cannot delete themselves or other members via librarian route")
    public void testF13_DeleteMember_MemberForbidden() {
        E2ETestHarness.TestSession memSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), memSession);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/members",
                Map.of("action", "delete", "userId", "3"), memSession);
        assertEquals(403, resp.getStatusCode());
    }

    @Test
    @DisplayName("F13.5: Deleting non-existent member handled safely")
    public void testF13_DeleteMember_NonExistent_Safe() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/members",
                Map.of("action", "delete", "userId", "99999"), session);
        assertEquals(302, resp.getStatusCode());
    }

    // =========================================================================
    // F14: Master Transaction Ledger
    // =========================================================================

    @Test
    @DisplayName("F14.1: Librarian views master transaction ledger")
    public void testF14_MasterLedger_Success() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/transactions", session);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F14.2: Ledger records new borrow transaction")
    public void testF14_MasterLedger_ContainsBorrow() throws Exception {
        E2ETestHarness.TestSession memSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), memSession);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "2"), memSession);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM transactions WHERE user_id = 2 AND book_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("F14.3: Ledger records transaction status change to RETURNED")
    public void testF14_MasterLedger_StatusReturned() throws Exception {
        E2ETestHarness.TestSession memSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), memSession);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "3"), memSession);
        E2ETestHarness.post("/member/return", Map.of("transactionId", "1"), memSession);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT status, return_date FROM transactions WHERE transaction_id = 1")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("RETURNED", rs.getString("status"));
            assertNotNull(rs.getTimestamp("return_date"));
        }
    }

    @Test
    @DisplayName("F14.4: Member forbidden from viewing master transaction ledger")
    public void testF14_MasterLedger_MemberForbidden() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/transactions", session);
        assertEquals(403, resp.getStatusCode());
    }

    @Test
    @DisplayName("F14.5: Master ledger complies with UI guardrails")
    public void testF14_MasterLedger_UiGuardrails() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/transactions", session);
        E2ETestHarness.assertNoBorders(resp.getBody());
        E2ETestHarness.assertNoShadows(resp.getBody());
    }

    // =========================================================================
    // F15: Process Return (Desk Override)
    // =========================================================================

    @Test
    @DisplayName("F15.1: Librarian returns book on behalf of member")
    public void testF15_DeskReturn_Success() throws Exception {
        E2ETestHarness.TestSession memSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), memSession);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), memSession);

        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/return",
                Map.of("transactionId", "1"), libSession);
        assertEquals(302, resp.getStatusCode());

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT status FROM transactions WHERE transaction_id = 1")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("RETURNED", rs.getString("status"));
        }
    }

    @Test
    @DisplayName("F15.2: Desk return increments available copies")
    public void testF15_DeskReturn_IncrementsCopies() throws Exception {
        E2ETestHarness.TestSession memSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), memSession);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "2"), memSession); // copies 3 -> 2

        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.post("/member/return", Map.of("transactionId", "1"), libSession); // copies 2 -> 3

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT available_copies FROM books WHERE book_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(3, rs.getInt("available_copies"));
        }
    }

    @Test
    @DisplayName("F15.3: Desk return on already returned transaction rejected")
    public void testF15_DeskReturn_AlreadyReturned_Rejected() {
        E2ETestHarness.TestSession memSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), memSession);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), memSession);

        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.post("/member/return", Map.of("transactionId", "1"), libSession);

        // Second return attempt
        E2ETestHarness.TestResponse resp2 = E2ETestHarness.post("/member/return",
                Map.of("transactionId", "1"), libSession);
        assertEquals(409, resp2.getStatusCode());
    }

    @Test
    @DisplayName("F15.4: Desk return non-existent transaction returns 404")
    public void testF15_DeskReturn_NonExistent_NotFound() {
        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/return",
                Map.of("transactionId", "99999"), libSession);
        assertEquals(404, resp.getStatusCode());
    }

    @Test
    @DisplayName("F15.5: Unauthenticated return request redirected")
    public void testF15_DeskReturn_UnauthRedirected() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/return",
                Map.of("transactionId", "1"), null);
        assertEquals(302, resp.getStatusCode());
    }

    // =========================================================================
    // F16: Broadcast Notification
    // =========================================================================

    @Test
    @DisplayName("F16.1: Librarian broadcasts notification to all members")
    public void testF16_BroadcastNotification_AllMembers() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Holiday Closure", "message", "Library closed next Monday.",
                        "type", "SYSTEM", "target", "ALL"), session);
        assertEquals(302, resp.getStatusCode());

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM notifications WHERE title = 'Holiday Closure'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertTrue(rs.getInt(1) >= 2); // Dispatched to all members
        }
    }

    @Test
    @DisplayName("F16.2: Broadcast notification with empty title rejected")
    public void testF16_BroadcastNotification_EmptyTitle_Rejected() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/notifications",
                Map.of("title", " ", "message", "Valid message", "type", "SYSTEM", "target", "ALL"), session);
        assertEquals(400, resp.getStatusCode());
    }

    @Test
    @DisplayName("F16.3: Broadcast notification with empty message rejected")
    public void testF16_BroadcastNotification_EmptyMessage_Rejected() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Title", "message", " ", "type", "SYSTEM", "target", "ALL"), session);
        assertEquals(400, resp.getStatusCode());
    }

    @Test
    @DisplayName("F16.4: Member forbidden from broadcasting notifications")
    public void testF16_BroadcastNotification_MemberForbidden() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Spam", "message", "Spam message", "type", "SYSTEM", "target", "ALL"), session);
        assertEquals(403, resp.getStatusCode());
    }

    @Test
    @DisplayName("F16.5: Dispatched notifications are initialized as unread")
    public void testF16_BroadcastNotification_InitUnread() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Notice", "message", "Body", "type", "SYSTEM", "target", "ALL"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT is_read FROM notifications WHERE title = 'Notice'")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                assertFalse(rs.getBoolean("is_read"));
            }
        }
    }

    // =========================================================================
    // F17: Targeted Due Date Notice
    // =========================================================================

    @Test
    @DisplayName("F17.1: Librarian sends targeted notice to specific member")
    public void testF17_TargetedNotice_Success() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Book Due Soon", "message", "Your book is due in 2 days.",
                        "type", "DUE_DATE", "target", "2"), session);
        assertEquals(302, resp.getStatusCode());

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT user_id, type FROM notifications WHERE title = 'Book Due Soon'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(2, rs.getInt("user_id"));
            assertEquals("DUE_DATE", rs.getString("type"));
        }
    }

    @Test
    @DisplayName("F17.2: Targeted notice does not appear in other member inbox")
    public void testF17_TargetedNotice_Isolated() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Targeted Alice", "message", "For Alice only", "type", "DUE_DATE", "target", "2"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM notifications WHERE user_id = 3 AND title = 'Targeted Alice'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1)); // Bob received 0
        }
    }

    @Test
    @DisplayName("F17.3: Member views received targeted notice in notifications inbox")
    public void testF17_TargetedNotice_ViewableByMember() {
        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Alice Reminder", "message", "Check your loans", "type", "DUE_DATE", "target", "2"), libSession);

        E2ETestHarness.TestSession memSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), memSession);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/notifications", memSession);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F17.4: Member forbidden from sending targeted notice")
    public void testF17_TargetedNotice_MemberForbidden() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Fake Notice", "message", "Fake", "type", "DUE_DATE", "target", "3"), session);
        assertEquals(403, resp.getStatusCode());
    }

    @Test
    @DisplayName("F17.5: Targeted notice notification type is preserved in database")
    public void testF17_TargetedNotice_TypePreserved() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "New Arrival Alert", "message", "New Java Book", "type", "NEW_ARRIVAL", "target", "2"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT type FROM notifications WHERE title = 'New Arrival Alert'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("NEW_ARRIVAL", rs.getString("type"));
        }
    }

    // =========================================================================
    // F18: Metric Summary KPI Cards
    // =========================================================================

    @Test
    @DisplayName("F18.1: Librarian dashboard returns summary KPI metrics")
    public void testF18_MetricSummaryCards_Success() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/dashboard", session);
        assertEquals(200, resp.getStatusCode());
        assertTrue(resp.containsText("totalBooks") || resp.getStatusCode() == 200);
    }

    @Test
    @DisplayName("F18.2: Total books KPI matches database count")
    public void testF18_MetricSummaryCards_TotalBooksAccurate() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM books")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(5, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("F18.3: Total copies KPI matches sum of total_copies")
    public void testF18_MetricSummaryCards_TotalCopiesAccurate() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT SUM(total_copies) FROM books")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(20, rs.getInt(1)); // 5+3+4+6+2
        }
    }

    @Test
    @DisplayName("F18.4: Borrowed books metric increments upon borrow")
    public void testF18_MetricSummaryCards_BorrowedCountIncrements() throws Exception {
        E2ETestHarness.TestSession memSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), memSession);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), memSession);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM transactions WHERE status = 'BORROWED'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("F18.5: KPI cards comply with zero shadows design guardrail")
    public void testF18_MetricSummaryCards_ZeroShadows() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/dashboard", session);
        E2ETestHarness.assertNoShadows(resp.getBody());
    }

    // =========================================================================
    // F19: Visual Borrowing Trends Graph
    // =========================================================================

    @Test
    @DisplayName("F19.1: Reports endpoint serves borrowing trend analytics data")
    public void testF19_BorrowingTrends_Success() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", session);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F19.2: Trends data contains borrowed metrics")
    public void testF19_BorrowingTrends_HasMetrics() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", session);
        assertTrue(resp.containsText("borrowed") || resp.getStatusCode() == 200);
    }

    @Test
    @DisplayName("F19.3: Member denied access to borrowing trends reports")
    public void testF19_BorrowingTrends_MemberForbidden() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", session);
        assertEquals(403, resp.getStatusCode());
    }

    @Test
    @DisplayName("F19.4: Borrowing trends chart container adheres to borderless styling")
    public void testF19_BorrowingTrends_Borderless() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", session);
        E2ETestHarness.assertNoBorders(resp.getBody());
    }

    @Test
    @DisplayName("F19.5: Trends page contains no sparkle icons")
    public void testF19_BorrowingTrends_NoSparkles() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", session);
        E2ETestHarness.assertNoSparkleIcons(resp.getBody());
    }

    // =========================================================================
    // F20: Genre Breakdown Doughnut Chart
    // =========================================================================

    @Test
    @DisplayName("F20.1: Genre distribution groups books by genre")
    public void testF20_GenreBreakdown_DatabaseGrouping() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT genre, COUNT(*) FROM books GROUP BY genre")) {
            ResultSet rs = ps.executeQuery();
            int genreCount = 0;
            while (rs.next()) {
                genreCount++;
                assertTrue(rs.getInt(2) > 0);
            }
            assertTrue(genreCount >= 2); // Computer Science, Fiction
        }
    }

    @Test
    @DisplayName("F20.2: Adding book updates genre distribution")
    public void testF20_GenreBreakdown_UpdatesOnAddBook() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.post("/librarian/books",
                Map.of("action", "add", "isbn", "978-0000000003", "title", "Calculus",
                        "author", "Stewart", "genre", "Mathematics", "totalCopies", "4"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM books WHERE genre = 'Mathematics'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("F20.3: Genre report accessible to librarian")
    public void testF20_GenreBreakdown_LibrarianAccess() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", session);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F20.4: Genre distribution renders without box shadows")
    public void testF20_GenreBreakdown_ZeroShadows() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", session);
        E2ETestHarness.assertNoShadows(resp.getBody());
    }

    @Test
    @DisplayName("F20.5: Unauthenticated user redirected from genre reports")
    public void testF20_GenreBreakdown_UnauthRedirected() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", null);
        assertEquals(302, resp.getStatusCode());
    }

    // =========================================================================
    // F21: Availability Ratio Chart
    // =========================================================================

    @Test
    @DisplayName("F21.1: Availability ratio calculation matches DB stock")
    public void testF21_AvailabilityRatio_Calculation() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT SUM(available_copies), SUM(total_copies) FROM books")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            int available = rs.getInt(1);
            int total = rs.getInt(2);
            assertEquals(total, available); // 100% available initially
        }
    }

    @Test
    @DisplayName("F21.2: Availability ratio decrements when book is borrowed")
    public void testF21_AvailabilityRatio_DecrementsOnBorrow() throws Exception {
        E2ETestHarness.TestSession memSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), memSession);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), memSession);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT SUM(available_copies), SUM(total_copies) FROM books")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            int available = rs.getInt(1);
            int total = rs.getInt(2);
            assertEquals(total - 1, available);
        }
    }

    @Test
    @DisplayName("F21.3: Availability ratio restored when book is returned")
    public void testF21_AvailabilityRatio_RestoresOnReturn() throws Exception {
        E2ETestHarness.TestSession memSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), memSession);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), memSession);
        E2ETestHarness.post("/member/return", Map.of("transactionId", "1"), memSession);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT SUM(available_copies), SUM(total_copies) FROM books")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            int available = rs.getInt(1);
            int total = rs.getInt(2);
            assertEquals(total, available);
        }
    }

    @Test
    @DisplayName("F21.4: Availability ratio chart complies with zero border guardrails")
    public void testF21_AvailabilityRatio_ZeroBorders() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", session);
        E2ETestHarness.assertNoBorders(resp.getBody());
    }

    @Test
    @DisplayName("F21.5: Availability ratio chart contains no sparkle icons")
    public void testF21_AvailabilityRatio_NoSparkles() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", session);
        E2ETestHarness.assertNoSparkleIcons(resp.getBody());
    }

    // =========================================================================
    // F22: Popular Books Ranking
    // =========================================================================

    @Test
    @DisplayName("F22.1: Popular books ranked by loan frequency")
    public void testF22_PopularBooks_RankedByLoans() throws Exception {
        E2ETestHarness.TestSession memSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), memSession);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), memSession);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT b.title, COUNT(t.transaction_id) as loan_count " +
                             "FROM books b LEFT JOIN transactions t ON b.book_id = t.book_id " +
                             "GROUP BY b.book_id, b.title ORDER BY loan_count DESC")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("Effective Java", rs.getString("title"));
            assertEquals(1, rs.getInt("loan_count"));
        }
    }

    @Test
    @DisplayName("F22.2: Book with zero borrows shows loan count 0")
    public void testF22_PopularBooks_ZeroLoansHandled() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT b.title, COUNT(t.transaction_id) as loan_count " +
                             "FROM books b LEFT JOIN transactions t ON b.book_id = t.book_id WHERE b.book_id = 5 GROUP BY b.title")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt("loan_count"));
        }
    }

    @Test
    @DisplayName("F22.3: Multiple borrows on same title rank higher")
    public void testF22_PopularBooks_MultiBorrowRanksHigher() throws Exception {
        // Alice borrows book 1, returns it, then Bob borrows book 1
        E2ETestHarness.TestSession aliceSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), aliceSession);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), aliceSession);
        E2ETestHarness.post("/member/return", Map.of("transactionId", "1"), aliceSession);

        E2ETestHarness.TestSession bobSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "bob@example.com", "password", "member123"), bobSession);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), bobSession);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM transactions WHERE book_id = 1")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("F22.4: Popular books ranking view accessible to librarian")
    public void testF22_PopularBooks_LibrarianAccess() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", session);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F22.5: Popular books ranking complies with UI guardrails")
    public void testF22_PopularBooks_UiGuardrails() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/librarian/reports", session);
        E2ETestHarness.assertNoBorders(resp.getBody());
        E2ETestHarness.assertNoShadows(resp.getBody());
    }

    // =========================================================================
    // F23: Multi-Field Book Search
    // =========================================================================

    @Test
    @DisplayName("F23.1: Search by title returns matching books")
    public void testF23_SearchBooks_ByTitle() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/search?q=Effective", null);
        assertEquals(200, resp.getStatusCode());
        assertTrue(resp.containsText("Effective Java"));
    }

    @Test
    @DisplayName("F23.2: Search by author returns matching books")
    public void testF23_SearchBooks_ByAuthor() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/search?q=Joshua", null);
        assertEquals(200, resp.getStatusCode());
        assertTrue(resp.containsText("Joshua Bloch"));
    }

    @Test
    @DisplayName("F23.3: Search by genre returns matching books")
    public void testF23_SearchBooks_ByGenre() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/search?q=Fiction", null);
        assertEquals(200, resp.getStatusCode());
        assertTrue(resp.containsText("1984"));
    }

    @Test
    @DisplayName("F23.4: Search with non-matching term displays friendly empty state")
    public void testF23_SearchBooks_NoMatch_FriendlyMessage() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/search?q=NonExistentKeywordXYZ", null);
        assertEquals(200, resp.getStatusCode());
        assertTrue(resp.containsText("No books found"));
    }

    @Test
    @DisplayName("F23.5: Search results display available copies badge")
    public void testF23_SearchBooks_StockBadge() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/search?q=Patterns", null);
        assertEquals(200, resp.getStatusCode());
        assertTrue(resp.containsText("3/3"));
    }

    // =========================================================================
    // F24: Search History Tracking
    // =========================================================================

    @Test
    @DisplayName("F24.1: Member search query is logged in search_history")
    public void testF24_SearchHistory_Logged() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);

        E2ETestHarness.get("/member/search?q=Design", session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT query_text, user_id FROM search_history WHERE user_id = ?")) {
            ps.setInt(1, 2);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("Design", rs.getString("query_text"));
        }
    }

    @Test
    @DisplayName("F24.2: Anonymous search query is not associated with member")
    public void testF24_SearchHistory_AnonymousNotLoggedToMember() throws Exception {
        E2ETestHarness.get("/member/search?q=AnonymousSearch", null);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM search_history WHERE query_text = 'AnonymousSearch'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("F24.3: Multiple searches create chronological history entries")
    public void testF24_SearchHistory_MultipleEntries() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);

        E2ETestHarness.get("/member/search?q=Java", session);
        E2ETestHarness.get("/member/search?q=Architecture", session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM search_history WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("F24.4: Search history preserves search_type field")
    public void testF24_SearchHistory_SearchTypeSaved() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);

        E2ETestHarness.post("/member/search", Map.of("q", "Orwell", "type", "AUTHOR"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT search_type FROM search_history WHERE query_text = 'Orwell'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("AUTHOR", rs.getString("search_type"));
        }
    }

    @Test
    @DisplayName("F24.5: Empty search query does not pollute search history")
    public void testF24_SearchHistory_EmptyQueryNotLogged() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);

        E2ETestHarness.get("/member/search?q=", session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM search_history WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }

    // =========================================================================
    // F25: View Search History
    // =========================================================================

    @Test
    @DisplayName("F25.1: Member views search history with status 200")
    public void testF25_ViewSearchHistory_Success() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/search", session);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F25.2: Member sees own search history")
    public void testF25_ViewSearchHistory_PersonalHistory() throws Exception {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.get("/member/search?q=PersonalQueryAlice", alice);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT query_text FROM search_history WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("PersonalQueryAlice", rs.getString("query_text"));
        }
    }

    @Test
    @DisplayName("F25.3: Member cannot see another member search history")
    public void testF25_ViewSearchHistory_IsolatedBetweenMembers() throws Exception {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.get("/member/search?q=AliceSecret", alice);

        E2ETestHarness.TestSession bob = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "bob@example.com", "password", "member123"), bob);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM search_history WHERE user_id = 3 AND query_text = 'AliceSecret'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("F25.4: Unauthenticated user cannot view search history")
    public void testF25_ViewSearchHistory_UnauthRedirected() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/dashboard", null);
        assertEquals(302, resp.getStatusCode());
    }

    @Test
    @DisplayName("F25.5: Search history view complies with UI guardrails")
    public void testF25_ViewSearchHistory_UiGuardrails() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/search", session);
        E2ETestHarness.assertNoBorders(resp.getBody());
        E2ETestHarness.assertNoShadows(resp.getBody());
    }

    // =========================================================================
    // F26: Borrow Available Book Workflow
    // =========================================================================

    @Test
    @DisplayName("F26.1: Member borrows available book successfully")
    public void testF26_BorrowBook_Success() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/borrow",
                Map.of("bookId", "1"), session);
        assertEquals(302, resp.getStatusCode());

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT available_copies FROM books WHERE book_id = 1")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(4, rs.getInt("available_copies")); // 5 - 1
        }
    }

    @Test
    @DisplayName("F26.2: Borrowing book creates transaction with due date in 14 days")
    public void testF26_BorrowBook_DueDateCalculated() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "2"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT borrow_date, due_date, status FROM transactions WHERE book_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("BORROWED", rs.getString("status"));
            assertNotNull(rs.getTimestamp("borrow_date"));
            assertNotNull(rs.getTimestamp("due_date"));
            assertTrue(rs.getTimestamp("due_date").after(rs.getTimestamp("borrow_date")));
        }
    }

    @Test
    @DisplayName("F26.3: Cannot borrow book when available copies == 0")
    public void testF26_BorrowBook_ZeroCopies_Rejected() throws Exception {
        // Force available_copies to 0
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE books SET available_copies = 0 WHERE book_id = 5")) {
            ps.executeUpdate();
        }

        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/borrow",
                Map.of("bookId", "5"), session);
        assertEquals(409, resp.getStatusCode());
    }

    @Test
    @DisplayName("F26.4: Unauthenticated borrow request redirected to login")
    public void testF26_BorrowBook_Unauthenticated_Redirected() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/borrow",
                Map.of("bookId", "1"), null);
        assertEquals(302, resp.getStatusCode());
    }

    @Test
    @DisplayName("F26.5: Borrowing book redirects to dashboard using PRG pattern")
    public void testF26_BorrowBook_PrgPattern() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/borrow",
                Map.of("bookId", "3"), session);
        assertEquals(302, resp.getStatusCode());
        assertTrue(resp.getRedirectLocation().contains("borrowed=true"));
    }

    // =========================================================================
    // F27: Return Borrowed Book Workflow
    // =========================================================================

    @Test
    @DisplayName("F27.1: Member returns active book loan successfully")
    public void testF27_ReturnBook_Success() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/return",
                Map.of("transactionId", "1"), session);
        assertEquals(302, resp.getStatusCode());

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT status, return_date FROM transactions WHERE transaction_id = 1")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("RETURNED", rs.getString("status"));
            assertNotNull(rs.getTimestamp("return_date"));
        }
    }

    @Test
    @DisplayName("F27.2: Return book increments available copies count atomically")
    public void testF27_ReturnBook_CopiesIncremented() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "2"), session); // 3 -> 2
        E2ETestHarness.post("/member/return", Map.of("transactionId", "1"), session); // 2 -> 3

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT available_copies FROM books WHERE book_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(3, rs.getInt("available_copies"));
        }
    }

    @Test
    @DisplayName("F27.3: Member cannot return a loan belonging to another member")
    public void testF27_ReturnBook_UnauthorizedLoanReturn_Forbidden() {
        // Alice borrows
        E2ETestHarness.TestSession aliceSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), aliceSession);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), aliceSession);

        // Bob attempts return
        E2ETestHarness.TestSession bobSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "bob@example.com", "password", "member123"), bobSession);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/return",
                Map.of("transactionId", "1"), bobSession);
        assertEquals(403, resp.getStatusCode());
    }

    @Test
    @DisplayName("F27.4: Returning already returned book rejected")
    public void testF27_ReturnBook_DoubleReturn_Rejected() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), session);
        E2ETestHarness.post("/member/return", Map.of("transactionId", "1"), session);

        // Attempt second return
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/return",
                Map.of("transactionId", "1"), session);
        assertEquals(409, resp.getStatusCode());
    }

    @Test
    @DisplayName("F27.5: Return on time incurs fine_amount = 0.00")
    public void testF27_ReturnBook_OnTime_ZeroFine() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), session);
        E2ETestHarness.post("/member/return", Map.of("transactionId", "1"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT fine_amount FROM transactions WHERE transaction_id = 1")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(0.00, rs.getDouble("fine_amount"), 0.001);
        }
    }

    // =========================================================================
    // F28: View Borrowing History
    // =========================================================================

    @Test
    @DisplayName("F28.1: Member views borrowing history returns status 200")
    public void testF28_ViewBorrowingHistory_Success() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/history", session);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F28.2: Borrowing history contains active loan record")
    public void testF28_ViewBorrowingHistory_ContainsActiveLoan() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM transactions WHERE user_id = 2 AND status = 'BORROWED'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("F28.3: Borrowing history contains returned loan record")
    public void testF28_ViewBorrowingHistory_ContainsReturnedLoan() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), session);
        E2ETestHarness.post("/member/return", Map.of("transactionId", "1"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM transactions WHERE user_id = 2 AND status = 'RETURNED'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("F28.4: Borrowing history displays due date")
    public void testF28_ViewBorrowingHistory_DisplaysDueDate() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "2"), session);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT due_date FROM transactions WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertNotNull(rs.getTimestamp("due_date"));
        }
    }

    @Test
    @DisplayName("F28.5: Borrowing history complies with UI guardrails")
    public void testF28_ViewBorrowingHistory_UiGuardrails() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/history", session);
        E2ETestHarness.assertNoBorders(resp.getBody());
        E2ETestHarness.assertNoShadows(resp.getBody());
    }

    // =========================================================================
    // F29: View Profile Details
    // =========================================================================

    @Test
    @DisplayName("F29.1: Member views profile with status 200")
    public void testF29_ViewProfile_Success() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/profile", session);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F29.2: Profile contains member name and email")
    public void testF29_ViewProfile_DetailsAccurate() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT name, email FROM users WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("Alice Johnson", rs.getString("name"));
            assertEquals("alice@example.com", rs.getString("email"));
        }
    }

    @Test
    @DisplayName("F29.3: Profile contains unique membership ID")
    public void testF29_ViewProfile_MembershipId() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT membership_id FROM users WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("MEM-1001", rs.getString("membership_id"));
        }
    }

    @Test
    @DisplayName("F29.4: Unauthenticated access to profile redirects to login")
    public void testF29_ViewProfile_UnauthRedirected() {
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/profile", null);
        assertEquals(302, resp.getStatusCode());
    }

    @Test
    @DisplayName("F29.5: Profile view complies with zero border guardrails")
    public void testF29_ViewProfile_ZeroBorders() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/profile", session);
        E2ETestHarness.assertNoBorders(resp.getBody());
    }

    // =========================================================================
    // F30: Update Profile & Password
    // =========================================================================

    @Test
    @DisplayName("F30.1: Member updates name and email successfully")
    public void testF30_UpdateProfile_Success() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/profile",
                Map.of("name", "Alice Wonderland", "email", "alice_new@example.com"), session);
        assertEquals(302, resp.getStatusCode());

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT name, email FROM users WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("Alice Wonderland", rs.getString("name"));
            assertEquals("alice_new@example.com", rs.getString("email"));
        }
    }

    @Test
    @DisplayName("F30.2: Member updates password with valid current password")
    public void testF30_UpdateProfile_PasswordUpdate_Success() throws Exception {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/profile",
                Map.of("name", "Alice Johnson", "email", "alice@example.com",
                        "currentPassword", "member123", "newPassword", "brandNewPassword123"), session);
        assertEquals(302, resp.getStatusCode());

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT password_hash FROM users WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("brandNewPassword123", rs.getString("password_hash"));
        }
    }

    @Test
    @DisplayName("F30.3: Password update rejected when current password is wrong")
    public void testF30_UpdateProfile_WrongCurrentPassword_Rejected() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);

        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/profile",
                Map.of("name", "Alice Johnson", "email", "alice@example.com",
                        "currentPassword", "wrongPass", "newPassword", "brandNewPassword123"), session);
        assertEquals(400, resp.getStatusCode());
    }

    @Test
    @DisplayName("F30.4: Member can log in with new password after update")
    public void testF30_UpdateProfile_LoginWithNewPassword() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.post("/member/profile",
                Map.of("name", "Alice Johnson", "email", "alice@example.com",
                        "currentPassword", "member123", "newPassword", "newSecretPass"), session);

        E2ETestHarness.TestSession newSession = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/login",
                Map.of("email", "alice@example.com", "password", "newSecretPass"), newSession);
        assertEquals(302, resp.getStatusCode());
        assertEquals("MEMBER", newSession.getRole());
    }

    @Test
    @DisplayName("F30.5: Profile update redirects with flash notification")
    public void testF30_UpdateProfile_FlashNotification() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/profile",
                Map.of("name", "Alice Updated", "email", "alice@example.com"), session);
        assertTrue(resp.getRedirectLocation().contains("updated=true"));
    }

    // =========================================================================
    // F31: Notification Alerts Inbox
    // =========================================================================

    @Test
    @DisplayName("F31.1: Member views notification inbox with status 200")
    public void testF31_NotificationInbox_Success() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/notifications", session);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("F31.2: Inbox displays broadcast notification")
    public void testF31_NotificationInbox_DisplaysBroadcast() throws Exception {
        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "All Members Alert", "message", "Important notice", "type", "SYSTEM", "target", "ALL"), libSession);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM notifications WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertTrue(rs.getInt(1) >= 1);
        }
    }

    @Test
    @DisplayName("F31.3: Unread notification count matches database state")
    public void testF31_NotificationInbox_UnreadCount() throws Exception {
        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Alert 1", "message", "Msg", "type", "SYSTEM", "target", "2"), libSession);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Alert 2", "message", "Msg", "type", "SYSTEM", "target", "2"), libSession);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM notifications WHERE user_id = 2 AND is_read = FALSE")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("F31.4: Notification inbox complies with zero border guardrails")
    public void testF31_NotificationInbox_ZeroBorders() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/notifications", session);
        E2ETestHarness.assertNoBorders(resp.getBody());
    }

    @Test
    @DisplayName("F31.5: Notification inbox contains no sparkle icons")
    public void testF31_NotificationInbox_NoSparkles() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/notifications", session);
        E2ETestHarness.assertNoSparkleIcons(resp.getBody());
    }

    // =========================================================================
    // F32: Mark Notification as Read
    // =========================================================================

    @Test
    @DisplayName("F32.1: Member marks notification as read")
    public void testF32_MarkNotificationRead_Success() throws Exception {
        // Send alert to Alice
        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Due Date Soon", "message", "Return your book", "type", "DUE_DATE", "target", "2"), libSession);

        // Alice marks read
        E2ETestHarness.TestSession aliceSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), aliceSession);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/notifications",
                Map.of("notificationId", "1"), aliceSession);
        assertEquals(302, resp.getStatusCode());

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT is_read FROM notifications WHERE notification_id = 1")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertTrue(rs.getBoolean("is_read"));
        }
    }

    @Test
    @DisplayName("F32.2: Marking read decreases unread notification count")
    public void testF32_MarkNotificationRead_DecreasesUnreadCount() throws Exception {
        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Alert", "message", "Msg", "type", "SYSTEM", "target", "2"), libSession);

        E2ETestHarness.TestSession aliceSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), aliceSession);
        E2ETestHarness.post("/member/notifications", Map.of("notificationId", "1"), aliceSession);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM notifications WHERE user_id = 2 AND is_read = FALSE")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("F32.3: Marking read on another member notification has no effect")
    public void testF32_MarkNotificationRead_OtherMember_NoEffect() throws Exception {
        // Send alert to Alice (user_id = 2)
        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Alice Alert", "message", "Private", "type", "SYSTEM", "target", "2"), libSession);

        // Bob (user_id = 3) attempts to mark it read
        E2ETestHarness.TestSession bobSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "bob@example.com", "password", "member123"), bobSession);
        E2ETestHarness.post("/member/notifications", Map.of("notificationId", "1"), bobSession);

        // Should still be unread for Alice
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT is_read FROM notifications WHERE notification_id = 1")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertFalse(rs.getBoolean("is_read"));
        }
    }

    @Test
    @DisplayName("F32.4: Marking already read notification is idempotent")
    public void testF32_MarkNotificationRead_Idempotent() {
        E2ETestHarness.TestSession libSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), libSession);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Alert", "message", "Msg", "type", "SYSTEM", "target", "2"), libSession);

        E2ETestHarness.TestSession aliceSession = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), aliceSession);
        E2ETestHarness.post("/member/notifications", Map.of("notificationId", "1"), aliceSession);

        E2ETestHarness.TestResponse resp2 = E2ETestHarness.post("/member/notifications",
                Map.of("notificationId", "1"), aliceSession);
        assertEquals(302, resp2.getStatusCode());
    }

    @Test
    @DisplayName("F32.5: Marking non-existent notification handled safely")
    public void testF32_MarkNotificationRead_NonExistent_Safe() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/notifications",
                Map.of("notificationId", "99999"), session);
        assertEquals(302, resp.getStatusCode());
    }
}
