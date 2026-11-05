package com.library.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Milestone 1 Adversarial Empirical Challenge Suite")
class AdversarialM1ChallengeTest {

    // =========================================================================
    // 1. Entity Invariant Enforcement: Book Stock Operations & Limits
    // =========================================================================

    @Test
    @DisplayName("Challenge 1.1: Borrowing when availableCopies == 0 must throw IllegalStateException")
    void testBorrowWhenAvailableCopiesIsZero() {
        // Book with totalCopies = 2, availableCopies = 0 (2 borrowed)
        Book book = new Book(1, "978-0134685991", "Effective Java", "Joshua Bloch", "CS", 2, 0);
        assertEquals(0, book.getAvailableCopies());
        assertFalse(book.isAvailable());

        IllegalStateException ex = assertThrows(IllegalStateException.class, book::borrowCopy);
        assertTrue(ex.getMessage().contains("no copies available"), 
                "Exception message must explain that no copies are available");
        // State must remain unmutated at 0
        assertEquals(0, book.getAvailableCopies());
    }

    @Test
    @DisplayName("Challenge 1.2: Returning when availableCopies == totalCopies must throw IllegalStateException")
    void testReturnWhenAvailableCopiesEqualsTotalCopies() {
        Book book = new Book(2, "978-0134685991", "Effective Java", "Joshua Bloch", "CS", 5, 5);
        assertEquals(5, book.getAvailableCopies());
        assertEquals(5, book.getTotalCopies());

        IllegalStateException ex = assertThrows(IllegalStateException.class, book::returnCopy);
        assertTrue(ex.getMessage().contains("available copies already equal total copies"),
                "Exception message must explain available equals total copies");
        // State must remain unmutated at 5
        assertEquals(5, book.getAvailableCopies());
    }

    @Test
    @DisplayName("Challenge 1.3: Instantiating Book with negative totalCopies or availableCopies must fail")
    void testNegativeCopiesInConstructor() {
        // Negative totalCopies
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                new Book("978-0134685991", "Title", "Author", "CS", -5));
        assertTrue(ex1.getMessage().contains("Total copies cannot be negative"));

        // Negative availableCopies
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                new Book(1, "978-0134685991", "Title", "Author", "CS", 10, -1));
        assertTrue(ex2.getMessage().contains("Available copies cannot be negative"));

        // availableCopies > totalCopies
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () ->
                new Book(1, "978-0134685991", "Title", "Author", "CS", 5, 6));
        assertTrue(ex3.getMessage().contains("cannot exceed total copies"));
    }

    @Test
    @DisplayName("Challenge 1.4: Mutating Book with negative totalCopies or availableCopies must fail")
    void testNegativeCopiesInMutators() {
        Book book = new Book(1, "978-0134685991", "Title", "Author", "CS", 10, 8);

        // Mutating totalCopies to negative
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> book.setTotalCopies(-1));
        assertTrue(ex1.getMessage().contains("Total copies cannot be negative"));

        // Mutating availableCopies to negative
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> book.setAvailableCopies(-1));
        assertTrue(ex2.getMessage().contains("Available copies cannot be negative"));

        // Mutating availableCopies to exceed totalCopies
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () -> book.setAvailableCopies(11));
        assertTrue(ex3.getMessage().contains("cannot exceed total copies"));
    }

    @Test
    @DisplayName("Challenge 1.5: Verify setTotalCopies rejects reducing total copies below available copies")
    void testSetTotalCopiesInvariantBreachVulnerability() {
        // Given book with total = 10, available = 8 (borrowed = 2)
        Book book = new Book(1, "978-0134685991", "Title", "Author", "CS", 10, 8);
        assertEquals(2, book.getBorrowedCopies());

        // When totalCopies is reduced to 5 (< availableCopies 8), must throw IllegalArgumentException
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> book.setTotalCopies(5));
        assertTrue(ex.getMessage().contains("cannot be less than available copies"));

        // Entity state remains unchanged and consistent
        assertEquals(10, book.getTotalCopies());
        assertEquals(8, book.getAvailableCopies());
        assertEquals(2, book.getBorrowedCopies());
    }

    @Test
    @DisplayName("Challenge 1.6: Book.hasCopies(-1) and Book.hasCopies(0) must return false")
    void testHasCopiesEdgeCases() {
        Book book = new Book(1, "978-0134685991", "Title", "Author", "CS", 10, 5);

        assertFalse(book.hasCopies(-1), "hasCopies(-1) must return false");
        assertFalse(book.hasCopies(-999), "hasCopies(-999) must return false");
        assertFalse(book.hasCopies(0), "hasCopies(0) must return false");
        assertFalse(book.hasCopies(Integer.MIN_VALUE), "hasCopies(Integer.MIN_VALUE) must return false");
        assertTrue(book.hasCopies(1), "hasCopies(1) must return true when available is 5");
        assertTrue(book.hasCopies(5), "hasCopies(5) must return true when available is 5");
        assertFalse(book.hasCopies(6), "hasCopies(6) must return false when available is 5");
        assertFalse(book.hasCopies(Integer.MAX_VALUE), "hasCopies(Integer.MAX_VALUE) must return false");
    }

    // =========================================================================
    // 2. Identity Contract: equals and hashCode across Transient vs Persisted
    // =========================================================================

    @Test
    @DisplayName("Challenge 2.1: Symmetry and transitivity for transient entities (id == null)")
    void testTransientEntityEqualsAndHashCode() {
        Book t1 = new Book("ISBN-1", "Title 1", "Author 1", "CS", 5);
        Book t2 = new Book("ISBN-1", "Title 1", "Author 1", "CS", 5);
        Book t3 = new Book("ISBN-2", "Title 2", "Author 2", "Math", 3);

        assertNull(t1.getId());
        assertNull(t2.getId());

        // Reflexivity
        assertEquals(t1, t1, "Reflexive: transient entity equals itself");

        // Transient non-identity: two distinct transient objects have id == null
        // Under BaseEntity: return id != null && Objects.equals(id, that.id)
        assertNotEquals(t1, t2, "Distinct transient instances must NOT equal each other");
        assertNotEquals(t2, t1, "Symmetry holds: neither transient instance equals the other");

        // Hash code consistency for transient entities
        assertEquals(t1.hashCode(), t2.hashCode(), "Transient entities of same class share class-based hashCode");
        assertEquals(t1.hashCode(), t3.hashCode(), "Transient entities of same class share class-based hashCode");
    }

    @Test
    @DisplayName("Challenge 2.2: Symmetry and transitivity across transient vs persisted entities")
    void testTransientVsPersistedEqualsAndHashCode() {
        Book transientBook = new Book("ISBN-1", "Title 1", "Author 1", "CS", 5);
        Book persistedBook = new Book(100, "ISBN-1", "Title 1", "Author 1", "CS", 5, 5);

        // Symmetry between transient and persisted
        assertNotEquals(transientBook, persistedBook, "Transient must not equal persisted");
        assertNotEquals(persistedBook, transientBook, "Symmetry: Persisted must not equal transient");

        // Transitivity across persisted entities
        Book p1 = new Book(100, "ISBN-A", "Title A", "Author A", "CS", 5, 5);
        Book p2 = new Book(100, "ISBN-B", "Title B", "Author B", "CS", 5, 5);
        Book p3 = new Book(100, "ISBN-C", "Title C", "Author C", "CS", 5, 5);

        assertEquals(p1, p2, "p1 equals p2 (same ID 100)");
        assertEquals(p2, p3, "p2 equals p3 (same ID 100)");
        assertEquals(p1, p3, "Transitivity: p1 equals p3 (same ID 100)");
        assertEquals(p1.hashCode(), p2.hashCode(), "Hash codes match");
        assertEquals(p2.hashCode(), p3.hashCode(), "Hash codes match");
    }

    @Test
    @DisplayName("Challenge 2.3: Entity ID assignment preserves HashSet containment with constant hashCode")
    void testEntityIdMutationInSetHarness() {
        Set<Book> bookSet = new HashSet<>();
        Book book = new Book("978-0134685991", "Effective Java", "Joshua Bloch", "CS", 5);
        assertNull(book.getId());

        bookSet.add(book);
        assertTrue(bookSet.contains(book), "Set contains transient book");

        // Now simulate persistence: ID is assigned post-insert
        book.setId(42);

        // Constant hashCode() guarantees containment across ID mutations
        boolean containsAfterIdAssigned = bookSet.contains(book);
        assertTrue(containsAfterIdAssigned, "Entity with constant hashCode remains retrievable in hashed sets after ID is assigned");

        // Restoring ID to null still preserves lookup
        book.setId(null);
        assertTrue(bookSet.contains(book), "Restoring ID to null restores HashSet lookup");
    }

    // =========================================================================
    // 3. Polymorphic Bounds: Member Quota and Librarian Restrictions
    // =========================================================================

    @Test
    @DisplayName("Challenge 3.1: Member correctly enforces max borrowing limit")
    void testMemberMaxBorrowLimitEnforcement() {
        Member member = new Member(10, "MEM-001", "Bob Smith", "bob@example.com", "hash");
        assertEquals(5, member.getMaxBorrowLimit());
        assertEquals(0, member.getActiveBorrowCount());
        assertTrue(member.canBorrow());

        // Increment up to limit
        for (int i = 0; i < 5; i++) {
            assertTrue(member.canBorrow(), "Member with " + i + " active loans should be allowed to borrow");
            member.incrementActiveBorrowCount();
        }

        // Limit reached: 5 loans
        assertEquals(5, member.getActiveBorrowCount());
        assertFalse(member.canBorrow(), "Member with 5 loans must NOT be allowed to borrow");

        // Exceeding limit throws IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, member::incrementActiveBorrowCount);
        assertTrue(ex.getMessage().contains("reached maximum borrow limit of 5"));

        // Decrement brings member back under limit
        member.decrementActiveBorrowCount();
        assertEquals(4, member.getActiveBorrowCount());
        assertTrue(member.canBorrow(), "Member with 4 loans can borrow again");
    }

    @Test
    @DisplayName("Challenge 3.2: Member decrementActiveBorrowCount does not go below zero")
    void testMemberDecrementDoesNotUnderflow() {
        Member member = new Member(10, "MEM-001", "Bob Smith", "bob@example.com", "hash");
        assertEquals(0, member.getActiveBorrowCount());

        // Decrementing when already 0 should stay 0
        member.decrementActiveBorrowCount();
        assertEquals(0, member.getActiveBorrowCount());
    }

    @Test
    @DisplayName("Challenge 3.3: Member setActiveBorrowCount rejects negative values")
    void testMemberSetActiveBorrowCountNegativeRejected() {
        Member member = new Member();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> member.setActiveBorrowCount(-1));
        assertTrue(ex.getMessage().contains("cannot be negative"));
    }

    @Test
    @DisplayName("Challenge 3.4: Librarian cannot borrow, has 0 borrow limit, and cannot be cast/mutated to Member")
    void testLibrarianPolymorphicBoundsAndCasting() {
        Librarian librarian = new Librarian(1, "LIB-001", "Alice Admin", "alice@library.com", "hash");

        // Borrowing privileges
        assertFalse(librarian.canBorrow(), "Librarian must never have borrowing privilege");
        assertEquals(0, librarian.getMaxBorrowLimit(), "Librarian max borrow limit must be 0");

        // Cannot mutate role to MEMBER
        IllegalArgumentException exRole = assertThrows(IllegalArgumentException.class, () -> librarian.setRole(Role.MEMBER));
        assertTrue(exRole.getMessage().contains("Cannot change role of Librarian"));
        assertEquals(Role.LIBRARIAN, librarian.getRole());

        // Cannot cast Librarian to Member (compile-time separate hierarchies, runtime ClassCastException via User)
        User userRef = librarian;
        assertThrows(ClassCastException.class, () -> {
            Member memberRef = (Member) userRef;
            assertNotNull(memberRef);
        }, "Casting a Librarian instance to Member must throw ClassCastException");

        // Sibling equality check
        Member member = new Member(1, "LIB-001", "Alice Admin", "alice@library.com", "hash");
        assertNotEquals(librarian, member, "Librarian and Member with identical ID and properties must NOT equal each other");
        assertNotEquals(member, librarian, "Symmetric: Member must NOT equal Librarian");
    }

    // =========================================================================
    // 4. Additional Domain Invariant Stress Tests (Transaction & Member)
    // =========================================================================

    @Test
    @DisplayName("Challenge 4.1: Member setActiveBorrowCount above maxBorrowLimit allows state drift")
    void testMemberSetActiveBorrowCountExceedingMaxLimit() {
        Member member = new Member(1, "MEM-001", "Alice", "alice@example.com", "hash");
        assertEquals(5, member.getMaxBorrowLimit());

        // setActiveBorrowCount does not validate against maxBorrowLimit:
        member.setActiveBorrowCount(10);
        assertEquals(10, member.getActiveBorrowCount(), "Active borrow count set to 10");
        assertFalse(member.canBorrow(), "Member cannot borrow when active loans exceed limit");

        // incrementActiveBorrowCount correctly throws when already over limit
        assertThrows(IllegalStateException.class, member::incrementActiveBorrowCount);
    }

    @Test
    @DisplayName("Challenge 4.2: Transaction overdue calculations and negative daily fine rates")
    void testTransactionFineCalculationsAdversarial() {
        // Transaction returned before due date
        Timestamp borrowTime = Timestamp.valueOf("2026-01-01 10:00:00");
        Timestamp dueTime = Timestamp.valueOf("2026-01-15 10:00:00");
        Timestamp returnEarly = Timestamp.valueOf("2026-01-10 10:00:00");
        Timestamp returnLate = Timestamp.valueOf("2026-01-20 10:00:00"); // 5 days late

        Transaction txEarly = new Transaction(1, 10, borrowTime, dueTime, returnEarly, TransactionStatus.RETURNED, 0.0);
        assertEquals(0, txEarly.getDaysOverdue(), "Early return must have 0 overdue days");
        assertEquals(new BigDecimal("0.00"), txEarly.calculateFine(new BigDecimal("1.50")));
        assertTrue(txEarly.isReturned());
        assertFalse(txEarly.isCurrentlyActive());

        Transaction txLate = new Transaction(1, 10, borrowTime, dueTime, returnLate, TransactionStatus.RETURNED, 0.0);
        assertEquals(5, txLate.getDaysOverdue(), "5 days overdue");
        assertEquals(new BigDecimal("7.50"), txLate.calculateFine(new BigDecimal("1.50")));

        // Transaction with null dueDate
        Transaction txNullDue = new Transaction();
        txNullDue.setDueDate((Timestamp) null);
        assertEquals(0, txNullDue.getDaysOverdue(), "Null due date returns 0 days overdue without NullPointerException");
        assertEquals(new BigDecimal("0.00"), txNullDue.calculateFine(new BigDecimal("2.00")));
    }

    @Test
    @DisplayName("Challenge 4.3: Transaction status fromString handles adversarial inputs")
    void testTransactionStatusFromStringAdversarial() {
        assertNull(TransactionStatus.fromString(null));
        assertEquals(TransactionStatus.BORROWED, TransactionStatus.fromString("borrowed"));
        assertEquals(TransactionStatus.BORROWED, TransactionStatus.fromString("  BORROWED  "));
        assertEquals(TransactionStatus.RETURNED, TransactionStatus.fromString("Returned"));
        assertEquals(TransactionStatus.OVERDUE, TransactionStatus.fromString("OVERDUE"));

        assertThrows(IllegalArgumentException.class, () -> TransactionStatus.fromString("UNKNOWN_STATUS"));
        assertThrows(IllegalArgumentException.class, () -> TransactionStatus.fromString(""));
    }

    @Test
    @DisplayName("Challenge 4.4: Role fromString handles empty and invalid strings")
    void testRoleFromStringAdversarial() {
        assertNull(Role.fromString(null));
        assertNull(Role.fromString(""));
        assertNull(Role.fromString("   "));
        assertEquals(Role.LIBRARIAN, Role.fromString("librarian"));
        assertEquals(Role.MEMBER, Role.fromString("  MEMBER  "));
        assertThrows(IllegalArgumentException.class, () -> Role.fromString("SUPERUSER"));
    }
}
