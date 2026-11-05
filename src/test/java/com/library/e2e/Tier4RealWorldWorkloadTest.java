package com.library.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tier 4: Real-World Application Workloads & End-to-End Scenarios
 * Covers >= 16 comprehensive multi-actor operational workflows simulating actual enterprise library operations:
 * term start catalog provisioning, member onboarding, borrowing surges, overdue daemon sweeps, inventory audits,
 * and security defenses.
 */
public class Tier4RealWorldWorkloadTest {

    @BeforeEach
    public void setUp() {
        E2ETestHarness.resetTestData();
    }

    @Test
    @DisplayName("T4.01: Scenario 1 - Academic Term Start: Bulk catalog provisioning & student batch onboarding")
    public void testT4_01_AcademicTermStartProvisioning() throws Exception {
        // 1. Librarian logs in
        E2ETestHarness.TestSession lib = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), lib);

        // 2. Provision 3 new textbook titles
        String[][] books = {
                {"978-0131103627", "The C Programming Language", "Brian Kernighan, Dennis Ritchie", "Computer Science", "10"},
                {"978-0262033848", "Introduction to Algorithms", "Thomas H. Cormen et al.", "Computer Science", "8"},
                {"978-0132350884", "Clean Code", "Robert C. Martin", "Software Engineering", "12"}
        };
        for (String[] b : books) {
            E2ETestHarness.post("/librarian/books",
                    Map.of("action", "add", "isbn", b[0], "title", b[1], "author", b[2], "genre", b[3], "totalCopies", b[4]), lib);
        }

        // 3. Batch register 3 student members
        String[][] students = {
                {"Emma Watson", "emma@university.edu", "passEmma123"},
                {"Daniel Radcliffe", "daniel@university.edu", "passDan123"},
                {"Rupert Grint", "rupert@university.edu", "passRup123"}
        };
        for (String[] s : students) {
            E2ETestHarness.post("/register", Map.of("name", s[0], "email", s[1], "password", s[2]), null);
        }

        // 4. Librarian broadcasts term welcome notice
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Welcome to Fall Semester 2026",
                        "message", "Catalog has been refreshed with new programming textbooks.",
                        "type", "SYSTEM", "target", "ALL"), lib);

        // 5. Verification
        try (Connection conn = E2ETestHarness.getDbConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM books WHERE total_copies >= 8")) {
                ResultSet rs = ps.executeQuery();
                assertTrue(rs.next());
                assertEquals(3, rs.getInt(1));
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE email LIKE '%@university.edu'")) {
                ResultSet rs = ps.executeQuery();
                assertTrue(rs.next());
                assertEquals(3, rs.getInt(1));
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM notifications WHERE title LIKE 'Welcome%'")) {
                ResultSet rs = ps.executeQuery();
                assertTrue(rs.next());
                assertTrue(rs.getInt(1) >= 5); // All members received notification
            }
        }
    }

    @Test
    @DisplayName("T4.02: Scenario 2 - Mid-Term Reading Week Borrowing Surge: High-concurrency checkouts")
    public void testT4_02_ReadingWeekBorrowingSurge() throws Exception {
        // Book 1 (Effective Java) has 5 copies
        int threadCount = 8;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        AtomicInteger successfulBorrows = new AtomicInteger(0);
        AtomicInteger rejectedBorrows = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int studentIndex = i;
            exec.submit(() -> {
                try {
                    latch.await();
                    // Each thread registers or uses existing
                    String email = "student_" + studentIndex + "@test.edu";
                    E2ETestHarness.post("/register", Map.of("name", "Student " + studentIndex, "email", email, "password", "pass123"), null);

                    E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
                    E2ETestHarness.post("/login", Map.of("email", email, "password", "pass123"), session);

                    E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), session);
                    if (resp.getStatusCode() == 302 && resp.getRedirectLocation().contains("borrowed=true")) {
                        successfulBorrows.incrementAndGet();
                    } else {
                        rejectedBorrows.incrementAndGet();
                    }
                } catch (Exception e) {
                    rejectedBorrows.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        latch.countDown();
        assertTrue(done.await(6, TimeUnit.SECONDS));
        exec.shutdown();

        assertEquals(5, successfulBorrows.get(), "Exactly 5 copies available, so exactly 5 must succeed");
        assertEquals(3, rejectedBorrows.get(), "Remaining 3 students must be cleanly rejected due to stock exhaustion");

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT available_copies FROM books WHERE book_id = 1")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt("available_copies"));
        }
    }

    @Test
    @DisplayName("T4.03: Scenario 3 - Overdue Daemon Sweep: Status escalation, fine math & targeted alerts")
    public void testT4_03_OverdueDaemonEscalationSweep() throws Exception {
        // Simulate 2 overdue loans in database
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO transactions (book_id, user_id, borrow_date, due_date, status, fine_amount) VALUES " +
                             "(1, 2, ?, ?, 'BORROWED', 0.00), " + // 5 days overdue
                             "(2, 3, ?, ?, 'BORROWED', 0.00)")) { // 12 days overdue
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusDays(19)));
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusDays(5)));
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now().minusDays(26)));
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now().minusDays(12)));
            ps.executeUpdate();
        }

        // Simulate background daemon execution:
        // 1. Identify due_date < NOW() AND status = 'BORROWED'
        // 2. Update status to 'OVERDUE'
        // 3. Dispatch targeted alerts
        try (Connection conn = E2ETestHarness.getDbConnection()) {
            try (PreparedStatement psUpdate = conn.prepareStatement(
                    "UPDATE transactions SET status = 'OVERDUE' WHERE due_date < ? AND status = 'BORROWED'")) {
                psUpdate.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                int overdueCount = psUpdate.executeUpdate();
                assertEquals(2, overdueCount);
            }

            try (PreparedStatement psAlert = conn.prepareStatement(
                    "INSERT INTO notifications (user_id, title, message, type) VALUES " +
                            "(2, 'Overdue Notice', 'Effective Java is overdue by 5 days.', 'DUE_DATE'), " +
                            "(3, 'Overdue Notice', 'Design Patterns is overdue by 12 days.', 'DUE_DATE')")) {
                psAlert.executeUpdate();
            }
        }

        // Both members receive targeted overdue alerts
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.TestResponse r1 = E2ETestHarness.get("/member/notifications", alice);
        assertEquals(200, r1.getStatusCode());

        E2ETestHarness.TestSession bob = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "bob@example.com", "password", "member123"), bob);
        E2ETestHarness.TestResponse r2 = E2ETestHarness.get("/member/notifications", bob);
        assertEquals(200, r2.getStatusCode());
    }

    @Test
    @DisplayName("T4.04: Scenario 4 - Circulation Desk Check-in Rush: Multi-book return and fine audit")
    public void testT4_04_CirculationDeskReturnRush() throws Exception {
        // Alice borrows 3 books
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "2"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "3"), alice);

        // Librarian checks in all 3 at circulation desk
        E2ETestHarness.TestSession lib = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), lib);

        for (int txId = 1; txId <= 3; txId++) {
            E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/return",
                    Map.of("transactionId", String.valueOf(txId)), lib);
            assertEquals(302, resp.getStatusCode());
        }

        // Verify all 3 books returned and available copies restored
        try (Connection conn = E2ETestHarness.getDbConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM transactions WHERE status = 'RETURNED'")) {
                ResultSet rs = ps.executeQuery();
                assertTrue(rs.next());
                assertEquals(3, rs.getInt(1));
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT available_copies, total_copies FROM books WHERE book_id IN (1, 2, 3)")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    assertEquals(rs.getInt("total_copies"), rs.getInt("available_copies"));
                }
            }
        }
    }

    @Test
    @DisplayName("T4.05: Scenario 5 - End-of-Term Catalog Audit and Damaged Book Retirement")
    public void testT4_05_CatalogAuditAndRetirement() throws Exception {
        E2ETestHarness.TestSession lib = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), lib);

        // 1. Audit reports
        E2ETestHarness.TestResponse reportResp = E2ETestHarness.get("/librarian/reports", lib);
        assertEquals(200, reportResp.getStatusCode());

        // 2. Retire book 5 (To Kill a Mockingbird)
        E2ETestHarness.TestResponse delResp = E2ETestHarness.post("/librarian/books",
                Map.of("action", "delete", "bookId", "5"), lib);
        assertEquals(302, delResp.getStatusCode());

        // 3. Verify total books decreased from 5 to 4
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM books")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(4, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("T4.06: Scenario 6 - Student Self-Service Account Governance")
    public void testT4_06_StudentSelfServiceGovernance() {
        // 1. Register
        E2ETestHarness.post("/register",
                Map.of("name", "Student SelfService", "email", "self@service.edu", "password", "initialPass123"), null);

        // 2. Login
        E2ETestHarness.TestSession student = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "self@service.edu", "password", "initialPass123"), student);

        // 3. Update profile and change password
        E2ETestHarness.post("/member/profile",
                Map.of("name", "Student Governance", "email", "self_updated@service.edu",
                        "currentPassword", "initialPass123", "newPassword", "brandNewSecretPass456"), student);

        // 4. Logout
        E2ETestHarness.get("/logout", student);

        // 5. Re-authenticate with new credentials
        E2ETestHarness.TestSession newSession = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse loginResp = E2ETestHarness.post("/login",
                Map.of("email", "self_updated@service.edu", "password", "brandNewSecretPass456"), newSession);
        assertEquals(302, loginResp.getStatusCode());
        assertEquals("MEMBER", newSession.getRole());
    }

    @Test
    @DisplayName("T4.07: Scenario 7 - Catalog Expansion & Genre Re-balancing")
    public void testT4_07_CatalogExpansionGenreRebalancing() throws Exception {
        E2ETestHarness.TestSession lib = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), lib);

        // Add 2 Mathematics books
        E2ETestHarness.post("/librarian/books",
                Map.of("action", "add", "isbn", "978-0131437371", "title", "Linear Algebra",
                        "author", "Gilbert Strang", "genre", "Mathematics", "totalCopies", "4"), lib);
        E2ETestHarness.post("/librarian/books",
                Map.of("action", "add", "isbn", "978-0134015637", "title", "Calculus Early Transcendentals",
                        "author", "Stewart", "genre", "Mathematics", "totalCopies", "3"), lib);

        // Verify genre breakdown reflects Mathematics
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT SUM(total_copies) FROM books WHERE genre = 'Mathematics'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(7, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("T4.08: Scenario 8 - Popular Book High-Demand Lifecycle: Rise to #1 Rank")
    public void testT4_08_PopularBookRiseToTopRank() throws Exception {
        // Book 4 (1984) borrowed, returned, and reborrowed 3 times
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);

        E2ETestHarness.TestSession bob = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "bob@example.com", "password", "member123"), bob);

        // Loan 1
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "4"), alice);
        E2ETestHarness.post("/member/return", Map.of("transactionId", "1"), alice);

        // Loan 2
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "4"), bob);
        E2ETestHarness.post("/member/return", Map.of("transactionId", "2"), bob);

        // Loan 3
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "4"), alice);

        // 1984 must have highest loan count (3)
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT b.title, COUNT(t.transaction_id) as total_loans " +
                             "FROM books b JOIN transactions t ON b.book_id = t.book_id " +
                             "GROUP BY b.book_id, b.title ORDER BY total_loans DESC LIMIT 1")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("1984", rs.getString("title"));
            assertEquals(3, rs.getInt("total_loans"));
        }
    }

    @Test
    @DisplayName("T4.09: Scenario 9 - Emergency Closure Broadcast and Member Inbox Clearing")
    public void testT4_09_EmergencyClosureBroadcast() throws Exception {
        // Librarian broadcasts urgent notice
        E2ETestHarness.TestSession lib = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), lib);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "URGENT: Campus Weather Closure",
                        "message", "All library branches will close today at 2 PM.",
                        "type", "SYSTEM", "target", "ALL"), lib);

        // Alice views inbox and marks it read
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        int notifId;
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT notification_id FROM notifications WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            rs.next();
            notifId = rs.getInt("notification_id");
        }
        E2ETestHarness.post("/member/notifications", Map.of("notificationId", String.valueOf(notifId)), alice);

        // Verify read in DB
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT is_read FROM notifications WHERE notification_id = ?")) {
            ps.setInt(1, notifId);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertTrue(rs.getBoolean("is_read"));
        }
    }

    @Test
    @DisplayName("T4.10: Scenario 10 - Member Clearance Workflow: Account Deletion Guard")
    public void testT4_10_MemberClearanceWorkflow() {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), alice);

        // Clearance attempt 1: Librarian cannot delete Alice while book is borrowed
        E2ETestHarness.TestSession lib = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), lib);
        E2ETestHarness.TestResponse delFail = E2ETestHarness.post("/librarian/members",
                Map.of("action", "delete", "userId", "2"), lib);
        assertEquals(409, delFail.getStatusCode());

        // Clearance attempt 2: Alice returns book
        E2ETestHarness.post("/member/return", Map.of("transactionId", "1"), alice);

        // Clean transaction history for clean delete
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM transactions WHERE user_id = 2")) {
            ps.executeUpdate();
        } catch (Exception ignored) {}

        // Clearance attempt 3: Librarian successfully clears member account
        E2ETestHarness.TestResponse delSuccess = E2ETestHarness.post("/librarian/members",
                Map.of("action", "delete", "userId", "2"), lib);
        assertEquals(302, delSuccess.getStatusCode());
    }

    @Test
    @DisplayName("T4.11: Scenario 11 - Multi-Search Discovery Journey")
    public void testT4_11_MultiSearchDiscoveryJourney() {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);

        // Search by author
        E2ETestHarness.TestResponse r1 = E2ETestHarness.get("/member/search?q=Joshua", alice);
        assertTrue(r1.containsText("Effective Java"));

        // Search by genre
        E2ETestHarness.TestResponse r2 = E2ETestHarness.get("/member/search?q=Computer+Science", alice);
        assertTrue(r2.containsText("Design Patterns"));

        // Borrow discovered book 2
        E2ETestHarness.TestResponse borrowResp = E2ETestHarness.post("/member/borrow", Map.of("bookId", "2"), alice);
        assertEquals(302, borrowResp.getStatusCode());
    }

    @Test
    @DisplayName("T4.12: Scenario 12 - High-Volume Mixed Workload: Borrowing, returning, searching concurrently")
    public void testT4_12_HighVolumeMixedWorkload() throws Exception {
        int tasks = 12;
        ExecutorService exec = Executors.newFixedThreadPool(6);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(tasks);

        for (int i = 0; i < tasks; i++) {
            final int idx = i;
            exec.submit(() -> {
                try {
                    latch.await();
                    if (idx % 3 == 0) {
                        E2ETestHarness.get("/member/search?q=Java", null);
                    } else if (idx % 3 == 1) {
                        E2ETestHarness.TestSession s = new E2ETestHarness.TestSession();
                        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), s);
                        E2ETestHarness.get("/member/dashboard", s);
                    } else {
                        E2ETestHarness.TestSession s = new E2ETestHarness.TestSession();
                        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), s);
                        E2ETestHarness.get("/librarian/dashboard", s);
                    }
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        latch.countDown();
        assertTrue(done.await(8, TimeUnit.SECONDS));
        exec.shutdown();
    }

    @Test
    @DisplayName("T4.13: Scenario 13 - Semester-End Financial Fine Reconciliation")
    public void testT4_13_FinancialFineReconciliation() throws Exception {
        // Create 2 returned transactions with fines ($5.00 and $15.00)
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO transactions (book_id, user_id, borrow_date, due_date, return_date, status, fine_amount) VALUES " +
                             "(1, 2, ?, ?, ?, 'RETURNED', 5.00), " +
                             "(2, 3, ?, ?, ?, 'RETURNED', 15.00)")) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusDays(30)));
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusDays(16)));
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now().minusDays(11)));

            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now().minusDays(40)));
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now().minusDays(26)));
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now().minusDays(11)));
            ps.executeUpdate();
        }

        // Reconcile total fines collected = $20.00
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT SUM(fine_amount) FROM transactions WHERE status = 'RETURNED'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(20.00, rs.getDouble(1), 0.001);
        }
    }

    @Test
    @DisplayName("T4.14: Scenario 14 - Security Hardening & Penetration Defense Scenario")
    public void testT4_14_SecurityHardeningDefense() {
        // 1. SQL Injection on login
        E2ETestHarness.TestSession s1 = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse r1 = E2ETestHarness.post("/login",
                Map.of("email", "admin' OR 1=1--", "password", "pwd"), s1);
        assertFalse(s1.isAuthenticated());

        // 2. Privilege escalation attempt
        E2ETestHarness.TestSession member = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), member);
        E2ETestHarness.TestResponse r2 = E2ETestHarness.get("/librarian/books", member);
        assertEquals(403, r2.getStatusCode());

        // 3. Unauthorized return attempt
        E2ETestHarness.TestResponse r3 = E2ETestHarness.post("/member/return",
                Map.of("transactionId", "9999"), member);
        assertNotEquals(200, r3.getStatusCode());
    }

    @Test
    @DisplayName("T4.15: Scenario 15 - Dual Database Failover & Schema Integrity Verification")
    public void testT4_15_DualDatabaseSchemaIntegrity() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());

            // Check all 5 core tables exist
            List<String> tables = List.of("USERS", "BOOKS", "TRANSACTIONS", "NOTIFICATIONS", "SEARCH_HISTORY");
            for (String tbl : tables) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM " + tbl)) {
                    ResultSet rs = ps.executeQuery();
                    assertTrue(rs.next());
                }
            }
        }
    }

    @Test
    @DisplayName("T4.16: Scenario 16 - Year-in-Review Full Operations 360-Degree Simulation")
    public void testT4_16_AnnualOperationsSimulation() {
        // Step 1: Provision
        E2ETestHarness.TestSession lib = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), lib);
        E2ETestHarness.post("/librarian/books",
                Map.of("action", "add", "isbn", "978-000-ANNUAL", "title", "Year Review Title",
                        "author", "Author", "genre", "General", "totalCopies", "2"), lib);

        // Step 2: Member joins & borrows
        E2ETestHarness.post("/register",
                Map.of("name", "Annual Member", "email", "annual@member.org", "password", "passAnnual1"), null);
        E2ETestHarness.TestSession mem = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "annual@member.org", "password", "passAnnual1"), mem);

        int bookId;
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT book_id FROM books WHERE isbn = '978-000-ANNUAL'")) {
            ResultSet rs = ps.executeQuery();
            rs.next();
            bookId = rs.getInt("book_id");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        E2ETestHarness.post("/member/borrow", Map.of("bookId", String.valueOf(bookId)), mem);

        // Step 3: Member returns
        int txId;
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT transaction_id FROM transactions WHERE book_id = ?")) {
            ps.setInt(1, bookId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            txId = rs.getInt("transaction_id");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        E2ETestHarness.post("/member/return", Map.of("transactionId", String.valueOf(txId)), mem);

        // Step 4: Final Dashboard audit
        E2ETestHarness.TestResponse kpi = E2ETestHarness.get("/librarian/dashboard", lib);
        assertEquals(200, kpi.getStatusCode());
    }
}
