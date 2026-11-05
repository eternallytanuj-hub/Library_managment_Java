package com.library.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Custom Exception Hierarchy Unit Tests")
class ExceptionHierarchyTest {

    @Test
    @DisplayName("Verify all custom exceptions inherit from checked LibraryException")
    void testInheritanceHierarchy() {
        assertTrue(Exception.class.isAssignableFrom(LibraryException.class), "LibraryException must extend Exception");
        assertFalse(RuntimeException.class.isAssignableFrom(LibraryException.class), "LibraryException must NOT be a RuntimeException (must be checked)");

        assertTrue(LibraryException.class.isAssignableFrom(BookNotFoundException.class));
        assertTrue(LibraryException.class.isAssignableFrom(BookUnavailableException.class));
        assertTrue(LibraryException.class.isAssignableFrom(MemberNotFoundException.class));
        assertTrue(LibraryException.class.isAssignableFrom(DuplicateEntityException.class));
        assertTrue(LibraryException.class.isAssignableFrom(ValidationException.class));
        assertTrue(LibraryException.class.isAssignableFrom(UnauthorizedAccessException.class));
        assertTrue(LibraryException.class.isAssignableFrom(DatabaseOperationException.class));
    }

    @Test
    @DisplayName("Verify LibraryException constructors and message propagation")
    void testLibraryExceptionConstructors() {
        LibraryException ex1 = new LibraryException();
        assertNotNull(ex1.getMessage());

        LibraryException ex2 = new LibraryException("Custom library message");
        assertEquals("Custom library message", ex2.getMessage());

        Throwable cause = new IllegalArgumentException("Root cause");
        LibraryException ex3 = new LibraryException("Message with cause", cause);
        assertEquals("Message with cause", ex3.getMessage());
        assertSame(cause, ex3.getCause());

        LibraryException ex4 = new LibraryException(cause);
        assertEquals("Root cause", ex4.getMessage());
        assertSame(cause, ex4.getCause());
    }

    @Test
    @DisplayName("Verify BookNotFoundException properties and constructors")
    void testBookNotFoundException() {
        BookNotFoundException ex1 = new BookNotFoundException(42);
        assertEquals(42, ex1.getBookId());
        assertTrue(ex1.getMessage().contains("42"));

        BookNotFoundException ex2 = new BookNotFoundException("978-0134685991", true);
        assertEquals("978-0134685991", ex2.getIsbn());
        assertTrue(ex2.getMessage().contains("978-0134685991"));

        Throwable cause = new RuntimeException("DB miss");
        BookNotFoundException ex3 = new BookNotFoundException("Book error", cause);
        assertSame(cause, ex3.getCause());
    }

    @Test
    @DisplayName("Verify BookUnavailableException properties and message")
    void testBookUnavailableException() {
        BookUnavailableException ex1 = new BookUnavailableException(101);
        assertEquals(101, ex1.getBookId());
        assertTrue(ex1.getMessage().contains("101"));

        BookUnavailableException ex2 = new BookUnavailableException(102, "Clean Architecture");
        assertEquals(102, ex2.getBookId());
        assertEquals("Clean Architecture", ex2.getTitle());
        assertTrue(ex2.getMessage().contains("Clean Architecture"));
    }

    @Test
    @DisplayName("Verify MemberNotFoundException properties and constructors")
    void testMemberNotFoundException() {
        MemberNotFoundException ex1 = new MemberNotFoundException(205);
        assertEquals(205, ex1.getMemberId());
        assertTrue(ex1.getMessage().contains("205"));

        MemberNotFoundException ex2 = new MemberNotFoundException("LIB-M-00999");
        assertEquals("LIB-M-00999", ex2.getIdentifier());
        assertTrue(ex2.getMessage().contains("LIB-M-00999"));
    }

    @Test
    @DisplayName("Verify DuplicateEntityException properties and collision details")
    void testDuplicateEntityException() {
        DuplicateEntityException ex = new DuplicateEntityException("Book", "ISBN", "978-0134685991");
        assertEquals("Book", ex.getEntityName());
        assertEquals("ISBN", ex.getFieldName());
        assertEquals("978-0134685991", ex.getConflictingValue());
        assertTrue(ex.getMessage().contains("Book with ISBN '978-0134685991' already exists"));
    }

    @Test
    @DisplayName("Verify ValidationException field attribution")
    void testValidationException() {
        ValidationException ex = new ValidationException("totalCopies", "Total copies cannot be negative");
        assertEquals("totalCopies", ex.getFieldName());
        assertTrue(ex.getMessage().contains("totalCopies"));
        assertTrue(ex.getMessage().contains("cannot be negative"));
    }

    @Test
    @DisplayName("Verify UnauthorizedAccessException role and resource tracking")
    void testUnauthorizedAccessException() {
        UnauthorizedAccessException ex = new UnauthorizedAccessException("/librarian/reports", "LIBRARIAN");
        assertEquals("/librarian/reports", ex.getAttemptedResource());
        assertEquals("LIBRARIAN", ex.getRequiredRole());
        assertTrue(ex.getMessage().contains("/librarian/reports"));
        assertTrue(ex.getMessage().contains("LIBRARIAN"));
    }

    @Test
    @DisplayName("Verify DatabaseOperationException wraps SQLException and preserves SQL state and error codes")
    void testDatabaseOperationExceptionCauseChaining() {
        SQLException sqlException = new SQLException("Deadlock detected during lock acquisition", "40001", 1213);
        DatabaseOperationException dbEx = new DatabaseOperationException("Failed to decrement inventory", sqlException);

        assertSame(sqlException, dbEx.getCause(), "Root cause must be preserved");
        assertEquals(Integer.valueOf(1213), dbEx.getErrorCode(), "Vendor error code must be exposed");
        assertEquals("40001", dbEx.getSqlState(), "SQLState code must be exposed");
        assertTrue(dbEx.getMessage().contains("Failed to decrement inventory"));

        // Test constructor taking only cause
        DatabaseOperationException dbExOnlyCause = new DatabaseOperationException(sqlException);
        assertSame(sqlException, dbExOnlyCause.getCause());
        assertEquals("Deadlock detected during lock acquisition", dbExOnlyCause.getMessage());
    }

    @Test
    @DisplayName("Verify polymorphic catch capability under LibraryException")
    void testPolymorphicCatchBlock() {
        boolean caughtAsLibraryException = false;
        try {
            throw new BookUnavailableException(77, "Effective Java");
        } catch (LibraryException e) {
            caughtAsLibraryException = true;
            assertInstanceOf(BookUnavailableException.class, e);
        }
        assertTrue(caughtAsLibraryException, "Subclasses must be catchable polymorphically as LibraryException");
    }
}
