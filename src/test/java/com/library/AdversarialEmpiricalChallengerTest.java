package com.library;

import com.library.dao.*;
import com.library.dao.impl.*;
import com.library.exception.*;
import com.library.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Empirical Adversarial Challenger Test Suite - Milestone 1 Iteration 2")
class AdversarialEmpiricalChallengerTest {

    // =========================================================================
    // 1. Build Portability & Runtime Verifications
    // =========================================================================

    @Test
    @DisplayName("Verify Java runtime meets or exceeds Java 21 LTS baseline")
    void testJavaRuntimeVersion() {
        String javaVersion = System.getProperty("java.version");
        assertNotNull(javaVersion);
        int majorVersion = Runtime.version().feature();
        assertTrue(majorVersion >= 21, "Runtime Java major version must be >= 21. Actual: " + majorVersion);
    }

    @Test
    @DisplayName("Verify Main bootstrap entry point executes cleanly without exceptions")
    void testMainBootstrapExecution() {
        assertDoesNotThrow(() -> Main.main(new String[0]), "Main.main() must execute without throwing any exception");
        assertEquals(8080, Main.DEFAULT_PORT);
    }

    // =========================================================================
    // 2. LibraryException Hierarchy & Polymorphism Verification
    // =========================================================================

    @Test
    @DisplayName("Verify every custom exception extends checked LibraryException and is not RuntimeException")
    void testAllExceptionsAreCheckedAndExtendLibraryException() {
        List<Class<? extends Throwable>> exceptionClasses = List.of(
                LibraryException.class,
                BookNotFoundException.class,
                BookUnavailableException.class,
                MemberNotFoundException.class,
                DuplicateEntityException.class,
                ValidationException.class,
                UnauthorizedAccessException.class,
                AuthenticationException.class,
                DatabaseOperationException.class,
                EntityNotFoundException.class
        );

        for (Class<? extends Throwable> clazz : exceptionClasses) {
            assertTrue(Exception.class.isAssignableFrom(clazz), clazz.getSimpleName() + " must extend Exception");
            assertFalse(RuntimeException.class.isAssignableFrom(clazz), clazz.getSimpleName() + " must NOT extend RuntimeException");
            if (clazz != LibraryException.class) {
                assertTrue(LibraryException.class.isAssignableFrom(clazz), clazz.getSimpleName() + " must inherit from LibraryException");
            }
        }
    }

    @Test
    @DisplayName("Verify polymorphic catch catches all custom exceptions under LibraryException")
    void testPolymorphicCatchAllCustomExceptions() {
        List<LibraryException> exceptions = List.of(
                new LibraryException("base error"),
                new BookNotFoundException(99),
                new BookNotFoundException("978-0134685991", true),
                new BookUnavailableException(101, "Test Book"),
                new MemberNotFoundException(201),
                new MemberNotFoundException("MEM-001"),
                new DuplicateEntityException("Book", "ISBN", "978-0134685991"),
                new ValidationException("email", "invalid email format"),
                new UnauthorizedAccessException("/admin", "LIBRARIAN"),
                new AuthenticationException("Bad credentials"),
                new DatabaseOperationException("SQL query failed", new SQLException("Connection refused", "08001", 100)),
                new EntityNotFoundException("Entity not found")
        );

        for (LibraryException ex : exceptions) {
            boolean caught = false;
            try {
                throw ex;
            } catch (LibraryException e) {
                caught = true;
                assertNotNull(e.getMessage());
                assertSame(ex, e);
            }
            assertTrue(caught, "Failed to catch " + ex.getClass().getSimpleName() + " polymorphically as LibraryException");
        }
    }

    @Test
    @DisplayName("Verify DatabaseOperationException cause chaining and SQL error details")
    void testDatabaseOperationExceptionCauseAndDetails() {
        SQLException sqlEx = new SQLException("Integrity constraint violation: foreign key", "23503", 23503);
        DatabaseOperationException dbEx = new DatabaseOperationException("Failed to delete member", sqlEx);

        assertEquals("23503", dbEx.getSqlState());
        assertEquals(Integer.valueOf(23503), dbEx.getErrorCode());
        assertSame(sqlEx, dbEx.getCause());
        assertTrue(dbEx.getMessage().contains("Failed to delete member"));

        // When cause is null or non-SQLException
        DatabaseOperationException simpleDbEx = new DatabaseOperationException("Simple error");
        assertNull(simpleDbEx.getSqlState());
        assertNull(simpleDbEx.getErrorCode());
    }

    @Test
    @DisplayName("Verify DuplicateEntityException field accessors and collision formatting")
    void testDuplicateEntityExceptionFormatting() {
        DuplicateEntityException ex = new DuplicateEntityException("User", "email", "john@example.com");
        assertEquals("User", ex.getEntityName());
        assertEquals("email", ex.getFieldName());
        assertEquals("john@example.com", ex.getConflictingValue());
        assertEquals("john@example.com", ex.getFieldValue());
        assertTrue(ex.getMessage().contains("User with email 'john@example.com' already exists"));
    }

    // =========================================================================
    // 3. Generic DAO Contract & Polymorphic Dispatch Verification
    // =========================================================================

    @Test
    @DisplayName("Verify GenericDao defines canonical CRUD operations")
    void testGenericDaoInterfaceContract() {
        Method[] methods = GenericDao.class.getMethods();
        Set<String> methodNames = new HashSet<>();
        for (Method m : methods) {
            methodNames.add(m.getName());
        }

        assertTrue(methodNames.contains("findById"), "GenericDao must declare findById");
        assertTrue(methodNames.contains("findAll"), "GenericDao must declare findAll");
        assertTrue(methodNames.contains("save"), "GenericDao must declare save");
        assertTrue(methodNames.contains("update"), "GenericDao must declare update");
        assertTrue(methodNames.contains("deleteById"), "GenericDao must declare deleteById");
    }

    @Test
    @DisplayName("Verify all domain DAOs inherit from GenericDao")
    void testDomainDaosInheritGenericDao() {
        assertTrue(GenericDao.class.isAssignableFrom(BookDao.class));
        assertTrue(GenericDao.class.isAssignableFrom(UserDao.class));
        assertTrue(GenericDao.class.isAssignableFrom(MemberDao.class));
        assertTrue(GenericDao.class.isAssignableFrom(TransactionDao.class));
        assertTrue(GenericDao.class.isAssignableFrom(NotificationDao.class));
        assertTrue(GenericDao.class.isAssignableFrom(SearchHistoryDao.class));
    }

    @Test
    @DisplayName("Verify DAO implementations implement GenericDao and BaseJdbcDao")
    void testDaoImplementations() {
        assertTrue(GenericDao.class.isAssignableFrom(BookDaoJdbcImpl.class));
        assertTrue(GenericDao.class.isAssignableFrom(UserDaoJdbcImpl.class));
        assertTrue(GenericDao.class.isAssignableFrom(TransactionDaoJdbcImpl.class));
        assertTrue(GenericDao.class.isAssignableFrom(NotificationDaoJdbcImpl.class));
        assertTrue(GenericDao.class.isAssignableFrom(SearchHistoryDaoJdbcImpl.class));

        assertTrue(BaseJdbcDao.class.isAssignableFrom(BookDaoJdbcImpl.class));
        assertTrue(BaseJdbcDao.class.isAssignableFrom(UserDaoJdbcImpl.class));
        assertTrue(BaseJdbcDao.class.isAssignableFrom(TransactionDaoJdbcImpl.class));
        assertTrue(BaseJdbcDao.class.isAssignableFrom(NotificationDaoJdbcImpl.class));
        assertTrue(BaseJdbcDao.class.isAssignableFrom(SearchHistoryDaoJdbcImpl.class));
    }

    // =========================================================================
    // 4. Model Invariants & Worker Iteration 2 Hardening Stress Tests
    // =========================================================================

    @Test
    @DisplayName("Stress-test Book.setTotalCopies invariant precedence and boundary conditions")
    void testBookSetTotalCopiesInvariantsStress() {
        // Book with total=10, available=8 (borrowed=2)
        Book book = new Book(1, "978-0134685991", "Title", "Author", "CS", 10, 8);
        assertEquals(2, book.getBorrowedCopies());

        // 1. Negative total copies
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> book.setTotalCopies(-5));
        assertTrue(ex1.getMessage().contains("Total copies cannot be negative"));

        // 2. Total copies < borrowed copies (borrowed=2, try 1)
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> book.setTotalCopies(1));
        assertTrue(ex2.getMessage().contains("cannot be less than currently borrowed copies"));

        // 3. Total copies < available copies (available=8, try 5)
        // Here borrowed=2, total=5 > borrowed(2), but total(5) < available(8)
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () -> book.setTotalCopies(5));
        assertTrue(ex3.getMessage().contains("cannot be less than available copies"));

        // 4. Exact boundary: totalCopies == availableCopies (when available=8, try 8) -> valid!
        assertDoesNotThrow(() -> book.setTotalCopies(8));
        assertEquals(8, book.getTotalCopies());
        assertEquals(8, book.getAvailableCopies());
        assertEquals(0, book.getBorrowedCopies()); // now borrowed is 8 - 8 = 0

        // 5. Exact boundary: totalCopies == borrowedCopies when available == 0
        Book fullyBorrowed = new Book(2, "978-0134685991", "Title", "Author", "CS", 5, 0);
        assertEquals(5, fullyBorrowed.getBorrowedCopies());
        assertDoesNotThrow(() -> fullyBorrowed.setTotalCopies(5));
        assertEquals(5, fullyBorrowed.getTotalCopies());

        // Try setting totalCopies = 4 (< borrowed 5)
        IllegalArgumentException ex4 = assertThrows(IllegalArgumentException.class, () -> fullyBorrowed.setTotalCopies(4));
        assertTrue(ex4.getMessage().contains("cannot be less than currently borrowed copies"));
    }

    @Test
    @DisplayName("Stress-test BaseEntity hashCode stability across HashSet and HashMap lifecycle")
    void testBaseEntityHashCodeLifecycleStability() {
        Book book = new Book("978-0134685991", "Effective Java", "Joshua Bloch", "CS", 5);
        assertNull(book.getId());

        int initialHash = book.hashCode();
        assertEquals(Book.class.hashCode(), initialHash);

        // Put in HashSet
        Set<Book> set = new HashSet<>();
        set.add(book);
        assertTrue(set.contains(book));

        // Put in HashMap as key
        Map<Book, String> map = new HashMap<>();
        map.put(book, "transient-value");
        assertEquals("transient-value", map.get(book));

        // Mutate ID (simulating DB insert returning generated ID)
        book.setId(101);

        // Hash code must NOT have changed
        assertEquals(initialHash, book.hashCode());
        assertEquals(Book.class.hashCode(), book.hashCode());

        // HashSet containment MUST still hold!
        assertTrue(set.contains(book), "Entity must remain retrievable from HashSet after ID assignment");

        // HashMap retrieval MUST still hold!
        assertEquals("transient-value", map.get(book), "Entity must remain retrievable from HashMap after ID assignment");

        // Two persisted books with same class and ID must be equal and have identical hashCode
        Book persistedDuplicate = new Book(101, "978-0134685991", "Effective Java", "Joshua Bloch", "CS", 5, 5);
        assertEquals(book, persistedDuplicate);
        assertEquals(book.hashCode(), persistedDuplicate.hashCode());
        assertTrue(set.contains(persistedDuplicate), "Persisted instance with same ID must be found in set");

        // Sibling classes (Member vs Librarian) must have different hashCodes
        Member member = new Member(101, "M-101", "Alice", "alice@test.com", "hash");
        Librarian librarian = new Librarian(101, "M-101", "Alice", "alice@test.com", "hash");
        assertNotEquals(member, librarian);
        assertNotEquals(member.hashCode(), librarian.hashCode());
    }

    @Test
    @DisplayName("Stress-test Member and User maxBooksAllowed rubric accessors and limits")
    void testMaxBooksAllowedRubricAliasesStress() {
        assertEquals(5, Member.DEFAULT_MAX_BORROW_LIMIT);
        assertEquals(5, Member.DEFAULT_MAX_BOOKS_ALLOWED);

        Member member = new Member();
        assertEquals(5, member.getMaxBooksAllowed());
        assertEquals(5, member.getMaxBorrowLimit());

        member.setMaxBooksAllowed(10);
        assertEquals(10, member.getMaxBooksAllowed());
        assertEquals(10, member.getMaxBorrowLimit());

        member.setMaxBorrowLimit(8);
        assertEquals(8, member.getMaxBooksAllowed());
        assertEquals(8, member.getMaxBorrowLimit());

        // Negative values must be rejected
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> member.setMaxBooksAllowed(-1));
        assertTrue(ex1.getMessage().contains("cannot be negative"));

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> member.setMaxBorrowLimit(-1));
        assertTrue(ex2.getMessage().contains("cannot be negative"));

        // Polymorphic access through User reference
        User userMember = member;
        assertEquals(8, userMember.getMaxBooksAllowed());
        assertEquals(8, userMember.getMaxBorrowLimit());

        User userLibrarian = new Librarian();
        assertEquals(0, userLibrarian.getMaxBooksAllowed());
        assertEquals(0, userLibrarian.getMaxBorrowLimit());
    }
}
