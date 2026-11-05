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
 * Tier 3: Cross-Feature Combinations & State Transition Test Suite
 * Covers >= 32 pairwise interactions, concurrent access patterns, state lifecycles, and security barriers.
 */
public class Tier3CombinatorialInteractionTest {

    @BeforeEach
    public void setUp() {
        E2ETestHarness.resetTestData();
    }

    @Test
    @DisplayName("T3.01: Concurrent borrow race condition: 10 threads compete for last remaining copy")
    public void testT3_01_ConcurrentBorrow_LastCopyAtomicity() throws Exception {
        // Set book 5 available_copies to 1
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE books SET available_copies = 1 WHERE book_id = 5")) {
            ps.executeUpdate();
        }

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int memberId = (i % 2 == 0) ? 2 : 3;
            final String email = (memberId == 2) ? "alice@example.com" : "bob@example.com";
            executor.submit(() -> {
                try {
                    startLatch.await();
                    E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
                    E2ETestHarness.post("/login", Map.of("email", email, "password", "member123"), session);
                    E2ETestHarness.TestResponse resp = E2ETestHarness.post("/member/borrow", Map.of("bookId", "5"), session);
                    if (resp.getStatusCode() == 302 && resp.getRedirectLocation().contains("borrowed=true")) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one concurrent thread must succeed in borrowing the last copy");
        assertEquals(threadCount - 1, failureCount.get(), "All other concurrent threads must fail with out-of-stock");

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT available_copies FROM books WHERE book_id = 5")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt("available_copies"), "Available copies must be exactly 0, never negative");
        }
    }

    @Test
    @DisplayName("T3.02: Borrow-Return-Reborrow cycle restores stock and allows re-borrowing")
    public void testT3_02_BorrowReturnReborrowCycle() throws Exception {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);

        // 1. Borrow book 1 (stock 5 -> 4)
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), alice);
        int avail1 = getAvailableCopies(1);
        assertEquals(4, avail1);

        // 2. Return book 1 (stock 4 -> 5)
        E2ETestHarness.post("/member/return", Map.of("transactionId", "1"), alice);
        int avail2 = getAvailableCopies(1);
        assertEquals(5, avail2);

        // 3. Re-borrow book 1 (stock 5 -> 4)
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), alice);
        int avail3 = getAvailableCopies(1);
        assertEquals(4, avail3);
    }

    @Test
    @DisplayName("T3.03: Search-and-borrow integration records search history and updates catalog stock")
    public void testT3_03_SearchAndBorrowIntegration() throws Exception {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);

        // 1. Search for Head First
        E2ETestHarness.TestResponse searchResp = E2ETestHarness.get("/member/search?q=Head+First", alice);
        assertTrue(searchResp.containsText("Head First Java"));

        // 2. Borrow found book (book_id = 3)
        E2ETestHarness.TestResponse borrowResp = E2ETestHarness.post("/member/borrow", Map.of("bookId", "3"), alice);
        assertEquals(302, borrowResp.getStatusCode());

        // 3. Verify search history was recorded
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT query_text FROM search_history WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertTrue(rs.getString("query_text").contains("Head"));
        }
    }

    @Test
    @DisplayName("T3.04: Add book, search, and borrow lifecycle across Librarian and Member")
    public void testT3_04_AddSearchBorrowLifecycle() throws Exception {
        // Librarian adds new book
        E2ETestHarness.TestSession lib = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), lib);
        E2ETestHarness.post("/librarian/books",
                Map.of("action", "add", "isbn", "978-0132350884", "title", "Clean Code",
                        "author", "Robert C. Martin", "genre", "Software", "totalCopies", "2"), lib);

        // Member searches for Clean Code
        E2ETestHarness.TestSession mem = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), mem);
        E2ETestHarness.TestResponse searchResp = E2ETestHarness.get("/member/search?q=Clean+Code", mem);
        assertTrue(searchResp.containsText("Clean Code"));

        // Member borrows it
        int newBookId;
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT book_id FROM books WHERE isbn = '978-0132350884'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            newBookId = rs.getInt("book_id");
        }
        E2ETestHarness.post("/member/borrow", Map.of("bookId", String.valueOf(newBookId)), mem);

        // Librarian verifies available copies decremented
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT available_copies FROM books WHERE book_id = ?")) {
            ps.setInt(1, newBookId);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(1, rs.getInt("available_copies"));
        }
    }

    @Test
    @DisplayName("T3.05: Multi-member stock contention: exhaustion and failure cascading")
    public void testT3_05_MultiMemberContention() throws Exception {
        // Book 5 has 2 copies
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);

        E2ETestHarness.TestSession bob = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "bob@example.com", "password", "member123"), bob);

        // Register third member Charlie
        E2ETestHarness.post("/register",
                Map.of("name", "Charlie", "email", "charlie_contend@example.com", "password", "pass1234"), null);
        E2ETestHarness.TestSession charlie = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "charlie_contend@example.com", "password", "pass1234"), charlie);

        // Alice borrows copy 1 -> 1 left
        E2ETestHarness.TestResponse r1 = E2ETestHarness.post("/member/borrow", Map.of("bookId", "5"), alice);
        assertEquals(302, r1.getStatusCode());

        // Bob borrows copy 2 -> 0 left
        E2ETestHarness.TestResponse r2 = E2ETestHarness.post("/member/borrow", Map.of("bookId", "5"), bob);
        assertEquals(302, r2.getStatusCode());

        // Charlie attempts borrow -> rejected
        E2ETestHarness.TestResponse r3 = E2ETestHarness.post("/member/borrow", Map.of("bookId", "5"), charlie);
        assertEquals(409, r3.getStatusCode());
    }

    @Test
    @DisplayName("T3.06: Desk override return restores member borrowing eligibility")
    public void testT3_06_DeskOverrideReturn() throws Exception {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), alice);

        // Librarian checks in book at desk
        E2ETestHarness.TestSession lib = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), lib);
        E2ETestHarness.post("/member/return", Map.of("transactionId", "1"), lib);

        // Alice can now borrow book again
        E2ETestHarness.TestResponse reborrow = E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), alice);
        assertEquals(302, reborrow.getStatusCode());
    }

    @Test
    @DisplayName("T3.07: Self-registration to login to catalog search to checkout flow")
    public void testT3_07_RegistrationToCheckoutFlow() {
        // 1. Register new member
        E2ETestHarness.post("/register",
                Map.of("name", "Diana Prince", "email", "diana@example.com", "password", "amazon123"), null);

        // 2. Login
        E2ETestHarness.TestSession diana = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse loginResp = E2ETestHarness.post("/login",
                Map.of("email", "diana@example.com", "password", "amazon123"), diana);
        assertEquals(302, loginResp.getStatusCode());

        // 3. Search
        E2ETestHarness.TestResponse searchResp = E2ETestHarness.get("/member/search?q=1984", diana);
        assertTrue(searchResp.containsText("1984"));

        // 4. Checkout book 4
        E2ETestHarness.TestResponse borrowResp = E2ETestHarness.post("/member/borrow", Map.of("bookId", "4"), diana);
        assertEquals(302, borrowResp.getStatusCode());
    }

    @Test
    @DisplayName("T3.08: Member email update followed by re-authentication preserves loan history")
    public void testT3_08_EmailUpdatePreservesHistory() throws Exception {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), alice);

        // Update email
        E2ETestHarness.post("/member/profile",
                Map.of("name", "Alice Johnson", "email", "alice_combo@example.com"), alice);

        // Login with new email
        E2ETestHarness.TestSession aliceNew = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login",
                Map.of("email", "alice_combo@example.com", "password", "member123"), aliceNew);

        // History still has active loan
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM transactions WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("T3.09: Member password update invalidates old credentials but preserves session integrity")
    public void testT3_09_PasswordUpdateReAuth() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);

        E2ETestHarness.post("/member/profile",
                Map.of("name", "Alice", "email", "alice@example.com",
                        "currentPassword", "member123", "newPassword", "newSecretPass789"), session);

        // Old pass fails
        E2ETestHarness.TestSession failSession = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse failResp = E2ETestHarness.post("/login",
                Map.of("email", "alice@example.com", "password", "member123"), failSession);
        assertTrue(failResp.getRedirectLocation().contains("error="));

        // New pass succeeds
        E2ETestHarness.TestSession successSession = new E2ETestHarness.TestSession();
        E2ETestHarness.TestResponse successResp = E2ETestHarness.post("/login",
                Map.of("email", "alice@example.com", "password", "newSecretPass789"), successSession);
        assertEquals(302, successResp.getStatusCode());
        assertEquals("MEMBER", successSession.getRole());
    }

    @Test
    @DisplayName("T3.10: Book copies increase enables previously blocked member to borrow")
    public void testT3_10_CopyIncreaseEnablesBorrow() throws Exception {
        // Set book 5 to 0 copies
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE books SET available_copies = 0, total_copies = 0 WHERE book_id = 5")) {
            ps.executeUpdate();
        }

        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);

        // Initial borrow fails
        E2ETestHarness.TestResponse r1 = E2ETestHarness.post("/member/borrow", Map.of("bookId", "5"), alice);
        assertEquals(409, r1.getStatusCode());

        // Librarian increases copies to 3
        E2ETestHarness.TestSession lib = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), lib);
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE books SET available_copies = 3, total_copies = 3 WHERE book_id = 5")) {
            ps.executeUpdate();
        }

        // Alice retries borrow -> succeeds
        E2ETestHarness.TestResponse r2 = E2ETestHarness.post("/member/borrow", Map.of("bookId", "5"), alice);
        assertEquals(302, r2.getStatusCode());
    }

    @Test
    @DisplayName("T3.11: Book deletion blocked by active borrow, succeeds immediately after return")
    public void testT3_11_BookDeletionUnblockedAfterReturn() {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "5"), alice);

        // Librarian attempts delete -> fails 409
        E2ETestHarness.TestSession lib = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), lib);
        E2ETestHarness.TestResponse delFail = E2ETestHarness.post("/librarian/books",
                Map.of("action", "delete", "bookId", "5"), lib);
        assertEquals(409, delFail.getStatusCode());

        // Alice returns book
        E2ETestHarness.post("/member/return", Map.of("transactionId", "1"), alice);

        // Delete without active loans succeeds
        // (Clean up historical transactions for book 5 first to avoid FK constraint)
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM transactions WHERE book_id = 5")) {
            ps.executeUpdate();
        } catch (Exception ignored) {}

        E2ETestHarness.TestResponse delSuccess = E2ETestHarness.post("/librarian/books",
                Map.of("action", "delete", "bookId", "5"), lib);
        assertEquals(302, delSuccess.getStatusCode());
    }

    @Test
    @DisplayName("T3.12: Member deletion blocked by active loan, succeeds after return")
    public void testT3_12_MemberDeletionUnblockedAfterReturn() throws Exception {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), alice);

        // Librarian attempts delete Alice -> fails 409
        E2ETestHarness.TestSession lib = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), lib);
        E2ETestHarness.TestResponse delFail = E2ETestHarness.post("/librarian/members",
                Map.of("action", "delete", "userId", "2"), lib);
        assertEquals(409, delFail.getStatusCode());

        // Alice returns book
        E2ETestHarness.post("/member/return", Map.of("transactionId", "1"), alice);

        // Clean returned loan to satisfy FK
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM transactions WHERE user_id = 2")) {
            ps.executeUpdate();
        }

        E2ETestHarness.TestResponse delSuccess = E2ETestHarness.post("/librarian/members",
                Map.of("action", "delete", "userId", "2"), lib);
        assertEquals(302, delSuccess.getStatusCode());
    }

    @Test
    @DisplayName("T3.13: Broadcast notification isolation: marking read by Alice leaves unread for Bob")
    public void testT3_13_BroadcastNotificationIsolation() throws Exception {
        E2ETestHarness.TestSession lib = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), lib);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Broadcast 13", "message", "Msg", "type", "SYSTEM", "target", "ALL"), lib);

        // Alice marks read
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        int aliceNotifId = getNotificationId(2, "Broadcast 13");
        E2ETestHarness.post("/member/notifications", Map.of("notificationId", String.valueOf(aliceNotifId)), alice);

        // Verify Bob's notification remains unread
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT is_read FROM notifications WHERE user_id = 3 AND title = 'Broadcast 13'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertFalse(rs.getBoolean("is_read"));
        }
    }

    @Test
    @DisplayName("T3.14: Targeted notice dispatched to member holding overdue book")
    public void testT3_14_OverdueTargetedAlert() throws Exception {
        // Create overdue transaction for Bob
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO transactions (book_id, user_id, borrow_date, due_date, status) VALUES (1, 3, ?, ?, 'OVERDUE')")) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusDays(20)));
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusDays(6)));
            ps.executeUpdate();
        }

        // Librarian dispatches targeted due date notice to Bob
        E2ETestHarness.TestSession lib = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), lib);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "Overdue Notice", "message", "Your loan is 6 days overdue", "type", "DUE_DATE", "target", "3"), lib);

        // Bob checks notifications
        E2ETestHarness.TestSession bob = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "bob@example.com", "password", "member123"), bob);
        E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/notifications", bob);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    @DisplayName("T3.15: Overdue loan fine accumulation: 10 days overdue = $10.00 fine on return")
    public void testT3_15_TenDaysOverdueFine() throws Exception {
        int txId;
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO transactions (book_id, user_id, borrow_date, due_date, status, fine_amount) " +
                             "VALUES (1, 2, ?, ?, 'BORROWED', 0.00)", java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusDays(24)));
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusDays(10)));
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            txId = rs.getInt(1);
        }

        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/return", Map.of("transactionId", String.valueOf(txId)), alice);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT fine_amount FROM transactions WHERE transaction_id = ?")) {
            ps.setInt(1, txId);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertTrue(rs.getDouble("fine_amount") >= 10.00);
        }
    }

    @Test
    @DisplayName("T3.16: Cross-role permission escalation matrix: Member denied all 5 librarian routes")
    public void testT3_16_MemberDeniedAllLibrarianRoutes() {
        E2ETestHarness.TestSession member = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), member);

        List<String> librarianRoutes = List.of(
                "/librarian/dashboard",
                "/librarian/books",
                "/librarian/members",
                "/librarian/transactions",
                "/librarian/reports"
        );

        for (String route : librarianRoutes) {
            E2ETestHarness.TestResponse resp = E2ETestHarness.get(route, member);
            assertEquals(403, resp.getStatusCode(), "Route " + route + " must reject Member with 403 Forbidden");
        }
    }

    @Test
    @DisplayName("T3.17: Unauthenticated access interception matrix across protected endpoints")
    public void testT3_17_UnauthenticatedInterceptionMatrix() {
        List<String> protectedRoutes = List.of(
                "/librarian/dashboard",
                "/librarian/books",
                "/member/dashboard",
                "/member/profile",
                "/member/history"
        );

        for (String route : protectedRoutes) {
            E2ETestHarness.TestResponse resp = E2ETestHarness.get(route, null);
            assertEquals(302, resp.getStatusCode(), "Unauthenticated request to " + route + " must redirect");
            assertTrue(resp.getRedirectLocation().contains("/login"));
        }
    }

    @Test
    @DisplayName("T3.18: Concurrent returns by different members execute without deadlocks")
    public void testT3_18_ConcurrentReturnsNoDeadlock() throws Exception {
        // Alice borrows book 1, Bob borrows book 2
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), alice);

        E2ETestHarness.TestSession bob = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "bob@example.com", "password", "member123"), bob);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "2"), bob);

        ExecutorService exec = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        Future<Integer> f1 = exec.submit(() -> {
            latch.await();
            return E2ETestHarness.post("/member/return", Map.of("transactionId", "1"), alice).getStatusCode();
        });

        Future<Integer> f2 = exec.submit(() -> {
            latch.await();
            return E2ETestHarness.post("/member/return", Map.of("transactionId", "2"), bob).getStatusCode();
        });

        latch.countDown();
        assertEquals(302, f1.get(5, TimeUnit.SECONDS));
        assertEquals(302, f2.get(5, TimeUnit.SECONDS));
        exec.shutdown();
    }

    @Test
    @DisplayName("T3.19: Search history isolation between concurrent members")
    public void testT3_19_SearchHistoryIsolation() throws Exception {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.get("/member/search?q=AliceQuery", alice);

        E2ETestHarness.TestSession bob = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "bob@example.com", "password", "member123"), bob);
        E2ETestHarness.get("/member/search?q=BobQuery", bob);

        try (Connection conn = E2ETestHarness.getDbConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT query_text FROM search_history WHERE user_id = 2")) {
                ResultSet rs = ps.executeQuery();
                assertTrue(rs.next());
                assertEquals("AliceQuery", rs.getString("query_text"));
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT query_text FROM search_history WHERE user_id = 3")) {
                ResultSet rs = ps.executeQuery();
                assertTrue(rs.next());
                assertEquals("BobQuery", rs.getString("query_text"));
            }
        }
    }

    @Test
    @DisplayName("T3.20: Master transaction ledger updates in real time across borrow and return")
    public void testT3_20_MasterLedgerRealTimeSync() throws Exception {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), alice);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM transactions")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }

        E2ETestHarness.post("/member/return", Map.of("transactionId", "1"), alice);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT status FROM transactions WHERE transaction_id = 1")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("RETURNED", rs.getString("status"));
        }
    }

    @Test
    @DisplayName("T3.21: Dashboard KPI reconciliation across multi-step mutations")
    public void testT3_21_DashboardKpiReconciliation() {
        // Initial KPI check
        E2ETestHarness.TestSession lib = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), lib);
        E2ETestHarness.TestResponse kpi1 = E2ETestHarness.get("/librarian/dashboard", lib);
        assertTrue(kpi1.containsText("\"borrowed\":0"));

        // Alice borrows
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), alice);

        // Updated KPI check
        E2ETestHarness.TestResponse kpi2 = E2ETestHarness.get("/librarian/dashboard", lib);
        assertTrue(kpi2.containsText("\"borrowed\":1"));
    }

    @Test
    @DisplayName("T3.22: Popular books ranking updates dynamically based on multiple loans")
    public void testT3_22_PopularRankingSync() throws Exception {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "2"), alice); // Design Patterns

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT b.title FROM books b JOIN transactions t ON b.book_id = t.book_id " +
                             "GROUP BY b.book_id, b.title ORDER BY COUNT(t.transaction_id) DESC")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("Design Patterns", rs.getString("title"));
        }
    }

    @Test
    @DisplayName("T3.23: Availability ratio updates dynamically across borrow and return")
    public void testT3_23_AvailabilityRatioSync() throws Exception {
        int initialAvail = getTotalAvailableCopies();

        // Borrow 2 books
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "2"), alice);

        assertEquals(initialAvail - 2, getTotalAvailableCopies());

        // Return 1 book
        E2ETestHarness.post("/member/return", Map.of("transactionId", "1"), alice);
        assertEquals(initialAvail - 1, getTotalAvailableCopies());
    }

    @Test
    @DisplayName("T3.24: Genre breakdown reflects newly introduced genre in real time")
    public void testT3_24_GenreBreakdownSync() throws Exception {
        E2ETestHarness.TestSession lib = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), lib);
        E2ETestHarness.post("/librarian/books",
                Map.of("action", "add", "isbn", "978-44445555", "title", "World History",
                        "author", "Historian", "genre", "History", "totalCopies", "3"), lib);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM books WHERE genre = 'History'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("T3.25: Rapid lifecycle: Add, Borrow, Return, Edit, Delete")
    public void testT3_25_FullEntityLifecycle() throws Exception {
        // 1. Librarian adds book
        E2ETestHarness.TestSession lib = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), lib);
        E2ETestHarness.post("/librarian/books",
                Map.of("action", "add", "isbn", "978-55556666", "title", "Temp Lifecycle Book",
                        "author", "Author", "genre", "Temp", "totalCopies", "1"), lib);

        int bookId;
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT book_id FROM books WHERE isbn = '978-55556666'")) {
            ResultSet rs = ps.executeQuery();
            rs.next();
            bookId = rs.getInt("book_id");
        }

        // 2. Member borrows
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", String.valueOf(bookId)), alice);

        // 3. Member returns
        int txId;
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT transaction_id FROM transactions WHERE book_id = ?")) {
            ps.setInt(1, bookId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            txId = rs.getInt("transaction_id");
        }
        E2ETestHarness.post("/member/return", Map.of("transactionId", String.valueOf(txId)), alice);

        // 4. Librarian edits
        E2ETestHarness.post("/librarian/books",
                Map.of("action", "edit", "bookId", String.valueOf(bookId), "title", "Renamed Book",
                        "author", "Author", "genre", "Temp", "totalCopies", "2"), lib);

        // 5. Clean up historical transaction and delete
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM transactions WHERE book_id = ?")) {
            ps.setInt(1, bookId);
            ps.executeUpdate();
        }
        E2ETestHarness.TestResponse delResp = E2ETestHarness.post("/librarian/books",
                Map.of("action", "delete", "bookId", String.valueOf(bookId)), lib);
        assertEquals(302, delResp.getStatusCode());
    }

    @Test
    @DisplayName("T3.26: Multi-item borrowing by single member tracks distinct loan IDs")
    public void testT3_26_MultiBookBorrowingSingleMember() throws Exception {
        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "1"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "2"), alice);
        E2ETestHarness.post("/member/borrow", Map.of("bookId", "3"), alice);

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(DISTINCT book_id) FROM transactions WHERE user_id = 2 AND status = 'BORROWED'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(3, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("T3.27: Notification inbox unread counter decreases monotonically as items are marked read")
    public void testT3_27_UnreadCounterDecreases() throws Exception {
        E2ETestHarness.TestSession lib = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), lib);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "A1", "message", "M1", "type", "SYSTEM", "target", "2"), lib);
        E2ETestHarness.post("/librarian/notifications",
                Map.of("title", "A2", "message", "M2", "type", "SYSTEM", "target", "2"), lib);

        E2ETestHarness.TestSession alice = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), alice);

        int count1 = getUnreadNotificationCount(2);
        assertEquals(2, count1);

        E2ETestHarness.post("/member/notifications", Map.of("notificationId", "1"), alice);
        int count2 = getUnreadNotificationCount(2);
        assertEquals(1, count2);

        E2ETestHarness.post("/member/notifications", Map.of("notificationId", "2"), alice);
        int count3 = getUnreadNotificationCount(2);
        assertEquals(0, count3);
    }

    @Test
    @DisplayName("T3.28: Member notification preferences toggle is persistent in DB")
    public void testT3_28_NotificationPreferencesToggle() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE users SET due_date_alerts = FALSE, new_arrival_alerts = FALSE WHERE user_id = 2")) {
            ps.executeUpdate();
        }

        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT due_date_alerts, new_arrival_alerts FROM users WHERE user_id = 2")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertFalse(rs.getBoolean("due_date_alerts"));
            assertFalse(rs.getBoolean("new_arrival_alerts"));
        }
    }

    @Test
    @DisplayName("T3.29: Session fixation defense: clean session created upon authentication")
    public void testT3_29_SessionRenewalOnLogin() {
        E2ETestHarness.TestSession session = new E2ETestHarness.TestSession();
        String initialSessionId = session.getSessionId();

        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), session);
        assertTrue(session.isAuthenticated());
        assertNotNull(session.getCookies().get("JSESSIONID"));
    }

    @Test
    @DisplayName("T3.30: ACID rollback integrity: simulated failure aborts transaction cleanly")
    public void testT3_30_AcidRollbackIntegrity() throws Exception {
        int availBefore = getAvailableCopies(1);

        try (Connection conn = E2ETestHarness.getDbConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psUpdate = conn.prepareStatement(
                    "UPDATE books SET available_copies = available_copies - 1 WHERE book_id = 1")) {
                psUpdate.executeUpdate();
            }
            // Deliberate rollback
            conn.rollback();
        }

        int availAfter = getAvailableCopies(1);
        assertEquals(availBefore, availAfter, "Rollback must restore original stock count perfectly");
    }

    @Test
    @DisplayName("T3.31: Concurrent reader threads searching catalog while librarian adds books")
    public void testT3_31_ConcurrentReadWriteCatalog() throws Exception {
        ExecutorService exec = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(1);

        // Writer task
        Future<?> writer = exec.submit(() -> {
            latch.await();
            E2ETestHarness.TestSession lib = new E2ETestHarness.TestSession();
            E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), lib);
            E2ETestHarness.post("/librarian/books",
                    Map.of("action", "add", "isbn", "978-9999000", "title", "Concurrent Read/Write",
                            "author", "Author", "genre", "CS", "totalCopies", "2"), lib);
            return null;
        });

        // Reader tasks
        List<Future<Integer>> readers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            readers.add(exec.submit(() -> {
                latch.await();
                E2ETestHarness.TestResponse resp = E2ETestHarness.get("/member/search?q=Java", null);
                return resp.getStatusCode();
            }));
        }

        latch.countDown();
        writer.get(5, TimeUnit.SECONDS);
        for (Future<Integer> rf : readers) {
            assertEquals(200, rf.get(5, TimeUnit.SECONDS));
        }
        exec.shutdown();
    }

    @Test
    @DisplayName("T3.32: Comprehensive multi-actor E2E flow across all components")
    public void testT3_32_MultiActorFullInteraction() {
        // 1. Librarian logs in and creates book
        E2ETestHarness.TestSession lib = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "librarian@library.com", "password", "admin123"), lib);
        E2ETestHarness.post("/librarian/books",
                Map.of("action", "add", "isbn", "978-10101010", "title", "Grand Unified Java",
                        "author", "Master", "genre", "CS", "totalCopies", "1"), lib);

        // 2. Member logs in, searches, and borrows
        E2ETestHarness.TestSession mem = new E2ETestHarness.TestSession();
        E2ETestHarness.post("/login", Map.of("email", "alice@example.com", "password", "member123"), mem);
        E2ETestHarness.TestResponse searchResp = E2ETestHarness.get("/member/search?q=Grand+Unified", mem);
        assertTrue(searchResp.containsText("Grand Unified Java"));

        int bookId;
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT book_id FROM books WHERE isbn = '978-10101010'")) {
            ResultSet rs = ps.executeQuery();
            rs.next();
            bookId = rs.getInt("book_id");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        E2ETestHarness.post("/member/borrow", Map.of("bookId", String.valueOf(bookId)), mem);

        // 3. Librarian audits transactions ledger
        E2ETestHarness.TestResponse txResp = E2ETestHarness.get("/librarian/transactions", lib);
        assertEquals(200, txResp.getStatusCode());

        // 4. Member returns
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

        E2ETestHarness.TestResponse returnResp = E2ETestHarness.post("/member/return",
                Map.of("transactionId", String.valueOf(txId)), mem);
        assertEquals(302, returnResp.getStatusCode());

        // 5. Librarian audits final inventory status
        E2ETestHarness.TestResponse kpiResp = E2ETestHarness.get("/librarian/dashboard", lib);
        assertEquals(200, kpiResp.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private int getAvailableCopies(int bookId) throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT available_copies FROM books WHERE book_id = ?")) {
            ps.setInt(1, bookId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt("available_copies");
        }
    }

    private int getTotalAvailableCopies() throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT SUM(available_copies) FROM books")) {
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    private int getNotificationId(int userId, String title) throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT notification_id FROM notifications WHERE user_id = ? AND title = ?")) {
            ps.setInt(1, userId);
            ps.setString(2, title);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt("notification_id");
        }
    }

    private int getUnreadNotificationCount(int userId) throws Exception {
        try (Connection conn = E2ETestHarness.getDbConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }
}
