package com.library.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Transaction Domain Model Unit Tests")
class TransactionTest {

    @Test
    @DisplayName("Verify Transaction creation defaults (status=BORROWED, fine=0.00, dueDate=borrowDate+14)")
    void testTransactionCreationDefaults() {
        LocalDateTime now = LocalDateTime.now();
        Transaction tx = new Transaction(101, 201, now, null);

        assertEquals(101, tx.getBookId());
        assertEquals(201, tx.getUserId());
        assertNotNull(tx.getBorrowDate());
        assertEquals(now, tx.getBorrowDateLocalDateTime());
        assertEquals(now.plusDays(14), tx.getDueDateLocalDateTime());
        assertNull(tx.getReturnDate());
        assertEquals(TransactionStatus.BORROWED, tx.getStatus());
        assertEquals(new BigDecimal("0.00"), tx.getFineAmount());
        assertTrue(tx.isActive());
        assertFalse(tx.isReturned());
    }

    @Test
    @DisplayName("Verify isOverdue evaluation across various dates and status states")
    void testIsOverdue() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Due date in the future -> not overdue
        Transaction futureTx = new Transaction(1, 1, now, now.plusDays(7));
        assertFalse(futureTx.isOverdue());

        // 2. Due date in the past with status BORROWED -> overdue
        Transaction pastTx = new Transaction(1, 1, now.minusDays(20), now.minusDays(6));
        assertTrue(pastTx.isOverdue());

        // 3. Status explicitly OVERDUE -> overdue
        Transaction explicitOverdue = new Transaction(1, 1, now, now.plusDays(5));
        explicitOverdue.setStatus(TransactionStatus.OVERDUE);
        assertTrue(explicitOverdue.isOverdue());

        // 4. Returned transaction is not overdue even if dueDate is past
        Transaction returnedTx = new Transaction(1, 1, now.minusDays(20), now.minusDays(6));
        returnedTx.setStatus(TransactionStatus.RETURNED);
        returnedTx.setReturnDate(now.minusDays(6));
        assertFalse(returnedTx.isOverdue());
    }

    @Test
    @DisplayName("Verify getDaysOverdue accurately computes elapsed days past dueDate")
    void testDaysOverdueCalculation() {
        LocalDateTime now = LocalDateTime.now();

        // Overdue by 5 days with no returnDate yet
        Transaction tx = new Transaction(1, 1, now.minusDays(19), now.minusDays(5));
        assertTrue(tx.getDaysOverdue() >= 5);

        // Returned 3 days after due date
        Transaction returnedLate = new Transaction(1, 1, now.minusDays(20), now.minusDays(10));
        returnedLate.setReturnDate(now.minusDays(7));
        assertEquals(3, returnedLate.getDaysOverdue());

        // Returned before due date -> 0 days overdue
        Transaction returnedEarly = new Transaction(1, 1, now.minusDays(10), now.plusDays(4));
        returnedEarly.setReturnDate(now.minusDays(2));
        assertEquals(0, returnedEarly.getDaysOverdue());
    }

    @Test
    @DisplayName("Verify calculateFine multiplies overdue days by daily rate")
    void testFineCalculation() {
        LocalDateTime now = LocalDateTime.now();

        // Returned 4 days overdue with default daily rate ($1.00)
        Transaction tx = new Transaction(1, 1, now.minusDays(18), now.minusDays(4));
        tx.setReturnDate(now);
        BigDecimal defaultFine = tx.calculateFine((BigDecimal) null);
        assertEquals(new BigDecimal("4.00"), defaultFine);

        // With custom daily rate ($2.50) -> 4 * 2.50 = 10.00
        BigDecimal customFine = tx.calculateFine(new BigDecimal("2.50"));
        assertEquals(new BigDecimal("10.00"), customFine);

        // With primitive double rate
        assertEquals(10.00, tx.calculateFine(2.50));

        // Not overdue -> fine is 0.00
        Transaction notOverdue = new Transaction(1, 1, now, now.plusDays(14));
        assertEquals(new BigDecimal("0.00"), notOverdue.calculateFine(new BigDecimal("2.00")));
    }

    @Test
    @DisplayName("Verify TransactionStatus enum operations and active status check")
    void testTransactionStatusEnum() {
        assertEquals("Borrowed", TransactionStatus.BORROWED.getDisplayName());
        assertEquals("Returned", TransactionStatus.RETURNED.getDisplayName());
        assertEquals("Overdue", TransactionStatus.OVERDUE.getDisplayName());

        assertTrue(TransactionStatus.BORROWED.isActive());
        assertTrue(TransactionStatus.OVERDUE.isActive());
        assertFalse(TransactionStatus.RETURNED.isActive());

        assertEquals(TransactionStatus.BORROWED, TransactionStatus.fromString("BORROWED"));
        assertEquals(TransactionStatus.RETURNED, TransactionStatus.fromString("returned"));
        assertEquals(TransactionStatus.OVERDUE, TransactionStatus.fromString("Overdue"));
        assertNull(TransactionStatus.fromString(null));

        assertThrows(IllegalArgumentException.class, () -> TransactionStatus.fromString("UNKNOWN"));
    }

    @Test
    @DisplayName("Verify Transaction navigational references and setters")
    void testNavigationalReferencesAndSetters() {
        Transaction tx = new Transaction();
        Book book = new Book("978-0134685991", "Effective Java", "Joshua Bloch", "CS", 5);
        Member member = new Member("MEM-001", "Alice", "alice@example.com", "pass");

        tx.setBook(book);
        tx.setUser(member);
        tx.setFineAmount(new BigDecimal("7.50"));

        assertSame(book, tx.getBook());
        assertSame(member, tx.getUser());
        assertEquals(new BigDecimal("7.50"), tx.getFineAmount());

        String str = tx.toString();
        assertTrue(str.contains("fineAmount=7.50"));
    }
}
