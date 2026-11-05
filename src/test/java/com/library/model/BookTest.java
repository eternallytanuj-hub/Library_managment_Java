package com.library.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Book Domain Model Unit Tests")
class BookTest {

    @Test
    @DisplayName("Verify valid Book creation with full constructor and business getters")
    void testBookCreationValid() {
        Book book = new Book(1, "978-0134685991", "Effective Java", "Joshua Bloch", "Computer Science", 10, 8);

        assertEquals(1, book.getId());
        assertEquals("978-0134685991", book.getIsbn());
        assertEquals("Effective Java", book.getTitle());
        assertEquals("Joshua Bloch", book.getAuthor());
        assertEquals("Computer Science", book.getGenre());
        assertEquals(10, book.getTotalCopies());
        assertEquals(8, book.getAvailableCopies());
        assertEquals(2, book.getBorrowedCopies());
        assertTrue(book.isAvailable());
        assertNotNull(book.getCreatedAt());
        assertNotNull(book.getUpdatedAt());
    }

    @Test
    @DisplayName("Verify copy invariants enforce 0 <= availableCopies <= totalCopies")
    void testCopyInvariants() {
        // Negative total copies
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                new Book(1, "978-0134685991", "Title", "Author", "Genre", -1, 0));
        assertTrue(ex1.getMessage().contains("Total copies cannot be negative"));

        // Negative available copies
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                new Book(1, "978-0134685991", "Title", "Author", "Genre", 5, -1));
        assertTrue(ex2.getMessage().contains("Available copies cannot be negative"));

        // Available copies exceeding total copies
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () ->
                new Book(1, "978-0134685991", "Title", "Author", "Genre", 5, 6));
        assertTrue(ex3.getMessage().contains("cannot exceed total copies"));
    }

    @Test
    @DisplayName("Verify isAvailable reports true only when availableCopies > 0")
    void testIsAvailable() {
        Book book = new Book("978-0134685991", "Effective Java", "Joshua Bloch", "Computer Science", 1);
        assertTrue(book.isAvailable());

        book.borrowCopy();
        assertEquals(0, book.getAvailableCopies());
        assertFalse(book.isAvailable());
    }

    @Test
    @DisplayName("Verify hasCopies accurately checks available inventory")
    void testHasCopies() {
        Book book = new Book(1, "978-0134685991", "Effective Java", "Joshua Bloch", "Computer Science", 5, 3);

        assertTrue(book.hasCopies(1));
        assertTrue(book.hasCopies(3));
        assertFalse(book.hasCopies(4));
        assertFalse(book.hasCopies(0));
        assertFalse(book.hasCopies(-2));
    }

    @Test
    @DisplayName("Verify borrowCopy decrements available count correctly")
    void testBorrowCopySuccess() {
        Book book = new Book(1, "978-0134685991", "Effective Java", "Joshua Bloch", "Computer Science", 5, 3);
        book.borrowCopy();

        assertEquals(2, book.getAvailableCopies());
        assertEquals(3, book.getBorrowedCopies());
    }

    @Test
    @DisplayName("Verify borrowCopy throws IllegalStateException when stock depleted")
    void testBorrowCopyDepletedThrowsException() {
        Book book = new Book(1, "978-0134685991", "Effective Java", "Joshua Bloch", "Computer Science", 1, 0);

        IllegalStateException ex = assertThrows(IllegalStateException.class, book::borrowCopy);
        assertTrue(ex.getMessage().contains("no copies available"));
    }

    @Test
    @DisplayName("Verify returnCopy increments available count correctly")
    void testReturnCopySuccess() {
        Book book = new Book(1, "978-0134685991", "Effective Java", "Joshua Bloch", "Computer Science", 5, 3);
        book.returnCopy();

        assertEquals(4, book.getAvailableCopies());
        assertEquals(1, book.getBorrowedCopies());
    }

    @Test
    @DisplayName("Verify returnCopy throws IllegalStateException when all copies already returned")
    void testReturnCopyExceedTotalThrowsException() {
        Book book = new Book(1, "978-0134685991", "Effective Java", "Joshua Bloch", "Computer Science", 5, 5);

        IllegalStateException ex = assertThrows(IllegalStateException.class, book::returnCopy);
        assertTrue(ex.getMessage().contains("available copies already equal total copies"));
    }

    @Test
    @DisplayName("Verify setTotalCopies protects against reducing total copies below active loans")
    void testUpdateTotalCopiesConstraint() {
        // Total = 5, Available = 3 -> 2 copies currently borrowed
        Book book = new Book(1, "978-0134685991", "Effective Java", "Joshua Bloch", "Computer Science", 5, 3);

        // Updating to 4 or 6 should succeed
        book.setTotalCopies(6);
        assertEquals(6, book.getTotalCopies());

        // Attempting to set total copies below 2 (borrowed count) must fail
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> book.setTotalCopies(1));
        assertTrue(ex.getMessage().contains("cannot be less than currently borrowed copies"));
    }

    @Test
    @DisplayName("Verify setTotalCopies rejects total copies less than available copies")
    void testSetTotalCopiesLessThanAvailableCopiesThrowsException() {
        Book book = new Book(1, "978-0134685991", "Effective Java", "Joshua Bloch", "Computer Science", 10, 8);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> book.setTotalCopies(5));
        assertTrue(ex.getMessage().contains("cannot be less than available copies"));
        assertEquals(10, book.getTotalCopies());
        assertEquals(8, book.getAvailableCopies());
        assertEquals(2, book.getBorrowedCopies());
    }

    @Test
    @DisplayName("Verify setAvailableCopies enforces copy invariants")
    void testSetAvailableCopiesValidation() {
        Book book = new Book(1, "978-0134685991", "Effective Java", "Joshua Bloch", "Computer Science", 5, 3);

        assertThrows(IllegalArgumentException.class, () -> book.setAvailableCopies(-1));
        assertThrows(IllegalArgumentException.class, () -> book.setAvailableCopies(6));

        book.setAvailableCopies(4);
        assertEquals(4, book.getAvailableCopies());
    }

    @Test
    @DisplayName("Verify Book mutators and toString output")
    void testToStringAndSetters() {
        Book book = new Book();
        book.setId(99);
        book.setIsbn("978-0201633610");
        book.setTitle("Design Patterns");
        book.setAuthor("Gang of Four");
        book.setGenre("Software Engineering");

        assertEquals(99, book.getId());
        assertEquals("978-0201633610", book.getIsbn());
        assertEquals("Design Patterns", book.getTitle());
        assertEquals("Gang of Four", book.getAuthor());
        assertEquals("Software Engineering", book.getGenre());

        String str = book.toString();
        assertTrue(str.contains("Design Patterns"));
        assertTrue(str.contains("978-0201633610"));
    }

    @Test
    @DisplayName("Verify Book identity equality based on primary key ID")
    void testBookIdentityEquality() {
        Book b1 = new Book(10, "ISBN-1", "Title 1", "Author 1", "Genre", 5, 5);
        Book b2 = new Book(10, "ISBN-2", "Title 2", "Author 2", "Genre", 3, 3);
        Book b3 = new Book(20, "ISBN-1", "Title 1", "Author 1", "Genre", 5, 5);

        assertEquals(b1, b2, "Entities with the same ID must be equal");
        assertEquals(b1.hashCode(), b2.hashCode(), "Entities with the same ID must have matching hash codes");
        assertNotEquals(b1, b3, "Entities with different IDs must not be equal");
        assertNotEquals(b1, null);
        assertNotEquals(b1, "someString");
    }
}
