package com.library.dao;

import com.library.config.DBConnectionFactory;
import com.library.dao.impl.BookDaoJdbcImpl;
import com.library.exception.DuplicateEntityException;
import com.library.model.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BookDaoJdbcImpl Integration Tests")
class BookDaoJdbcImplTest extends BaseDaoTest {

    private BookDao bookDao;

    @BeforeEach
    void setUp() {
        bookDao = new BookDaoJdbcImpl();
    }

    @Test
    @DisplayName("Should save a book and retrieve by ISBN")
    void testSaveAndFindByIsbn() throws Exception {
        Book book = new Book("978-0134685991", "Effective Java", "Joshua Bloch", "Computer Science", 5, 5);
        Book saved = bookDao.save(book);

        assertNotNull(saved.getId());
        assertTrue(saved.getId() > 0);

        Optional<Book> opt = bookDao.findByIsbn("978-0134685991");
        assertTrue(opt.isPresent());
        Book fetched = opt.get();
        assertEquals("Effective Java", fetched.getTitle());
        assertEquals("Joshua Bloch", fetched.getAuthor());
        assertEquals("Computer Science", fetched.getGenre());
        assertEquals(5, fetched.getTotalCopies());
        assertEquals(5, fetched.getAvailableCopies());
    }

    @Test
    @DisplayName("Should throw DuplicateEntityException on duplicate ISBN insertion")
    void testDuplicateIsbn_ThrowsDuplicateEntityException() throws Exception {
        Book b1 = new Book("978-1111", "Book One", "Author A", "Genre A", 3, 3);
        bookDao.save(b1);

        Book b2 = new Book("978-1111", "Book Two", "Author B", "Genre B", 2, 2);
        assertThrows(DuplicateEntityException.class, () -> bookDao.save(b2),
                "Inserting a book with existing ISBN must throw DuplicateEntityException");
    }

    @Test
    @DisplayName("Should search books by Title, Author, Genre, and All fields")
    void testSearchBooks_TitleAuthorGenreAll() throws Exception {
        bookDao.save(new Book("ISBN-1", "Clean Code", "Robert C. Martin", "Software", 4, 4));
        bookDao.save(new Book("ISBN-2", "Clean Architecture", "Robert C. Martin", "Software", 5, 5));
        bookDao.save(new Book("ISBN-3", "Design Patterns", "Gang of Four", "Computer Science", 3, 3));
        bookDao.save(new Book("ISBN-4", "1984", "George Orwell", "Fiction", 6, 6));

        // Search by Title
        List<Book> titleResults = bookDao.searchBooks("Clean", "TITLE");
        assertEquals(2, titleResults.size());

        // Search by Author
        List<Book> authorResults = bookDao.searchBooks("Robert", "AUTHOR");
        assertEquals(2, authorResults.size());

        // Search by Genre
        List<Book> genreResults = bookDao.searchBooks("Fiction", "GENRE");
        assertEquals(1, genreResults.size());
        assertEquals("1984", genreResults.get(0).getTitle());

        // Search ALL fields
        List<Book> allResults = bookDao.searchBooks("Four", "ALL");
        assertEquals(1, allResults.size());
        assertEquals("Design Patterns", allResults.get(0).getTitle());
    }

    @Test
    @DisplayName("Should atomically decrement available copies and prevent negative inventory")
    void testAtomicDecrementAvailableCopies() throws Exception {
        Book book = bookDao.save(new Book("ISBN-STOCK", "Stock Test Book", "Author", "Genre", 2, 2));
        int bookId = book.getId();

        try (Connection conn = DBConnectionFactory.getConnection()) {
            // First decrement: 2 -> 1
            boolean dec1 = bookDao.decrementAvailableCopies(conn, bookId);
            assertTrue(dec1);
            assertEquals(1, bookDao.findById(bookId).orElseThrow().getAvailableCopies());

            // Second decrement: 1 -> 0
            boolean dec2 = bookDao.decrementAvailableCopies(conn, bookId);
            assertTrue(dec2);
            assertEquals(0, bookDao.findById(bookId).orElseThrow().getAvailableCopies());

            // Third decrement when copies == 0: must return false and not go negative!
            boolean dec3 = bookDao.decrementAvailableCopies(conn, bookId);
            assertFalse(dec3, "Decrementing when copies == 0 must return false");
            assertEquals(0, bookDao.findById(bookId).orElseThrow().getAvailableCopies());
        }
    }

    @Test
    @DisplayName("Should atomically increment available copies and prevent exceeding total copies")
    void testAtomicIncrementAvailableCopies() throws Exception {
        Book book = bookDao.save(new Book("ISBN-INC", "Increment Test", "Author", "Genre", 3, 2));
        int bookId = book.getId();

        try (Connection conn = DBConnectionFactory.getConnection()) {
            // Increment: 2 -> 3 (total is 3)
            boolean inc1 = bookDao.incrementAvailableCopies(conn, bookId);
            assertTrue(inc1);
            assertEquals(3, bookDao.findById(bookId).orElseThrow().getAvailableCopies());

            // Increment when available == total: must return false
            boolean inc2 = bookDao.incrementAvailableCopies(conn, bookId);
            assertFalse(inc2, "Incrementing when available == total must return false");
            assertEquals(3, bookDao.findById(bookId).orElseThrow().getAvailableCopies());
        }
    }

    @Test
    @DisplayName("Should correctly calculate inventory KPI aggregations")
    void testKPIAggregations() throws Exception {
        bookDao.save(new Book("ISBN-K1", "Book 1", "A1", "G1", 5, 3));
        bookDao.save(new Book("ISBN-K2", "Book 2", "A2", "G2", 7, 6));

        assertEquals(2, bookDao.countTotalTitles());
        assertEquals(12, bookDao.countTotalCopies());
        assertEquals(9, bookDao.countAvailableCopies());
    }

    @Test
    @DisplayName("Should delete book by ID and verify removal")
    void testDeleteBook() throws Exception {
        Book book = bookDao.save(new Book("ISBN-DEL", "To Delete", "Author", "Genre", 2, 2));
        boolean deleted = bookDao.deleteById(book.getId());
        assertTrue(deleted);
        assertFalse(bookDao.findById(book.getId()).isPresent());
    }
}
