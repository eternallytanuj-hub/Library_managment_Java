package com.library.service;

import com.library.dao.BaseDaoTest;
import com.library.dao.BookDao;
import com.library.dao.SearchHistoryDao;
import com.library.dao.UserDao;
import com.library.dao.impl.BookDaoJdbcImpl;
import com.library.dao.impl.SearchHistoryDaoJdbcImpl;
import com.library.dao.impl.UserDaoJdbcImpl;
import com.library.exception.BookNotFoundException;
import com.library.exception.DuplicateEntityException;
import com.library.exception.LibraryException;
import com.library.exception.ValidationException;
import com.library.model.Book;
import com.library.model.Member;
import com.library.model.SearchHistory;
import com.library.service.impl.BookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BookService Integration Tests")
class BookServiceTest extends BaseDaoTest {

    private BookDao bookDao;
    private SearchHistoryDao searchHistoryDao;
    private UserDao userDao;
    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookDao = new BookDaoJdbcImpl();
        searchHistoryDao = new SearchHistoryDaoJdbcImpl();
        userDao = new UserDaoJdbcImpl();
        bookService = new BookServiceImpl(bookDao, searchHistoryDao);
    }

    @Test
    @DisplayName("Should successfully add a book and default available copies to total copies")
    void testAddBook_Success() throws Exception {
        Book book = new Book("978-0134685991", "Effective Java", "Joshua Bloch", "Computer Science", 5);
        Book saved = bookService.addBook(book);

        assertNotNull(saved.getId());
        assertEquals(5, saved.getTotalCopies());
        assertEquals(5, saved.getAvailableCopies());

        Optional<Book> fetched = bookService.getBookById(saved.getId());
        assertTrue(fetched.isPresent());
        assertEquals("Effective Java", fetched.get().getTitle());
    }

    @Test
    @DisplayName("Should reject duplicate ISBN when adding a book")
    void testAddBook_DuplicateIsbn() throws Exception {
        Book b1 = new Book("978-0134685991", "Effective Java", "Joshua Bloch", "Computer Science", 5);
        bookService.addBook(b1);

        Book b2 = new Book("978-0134685991", "Another Java Book", "Author", "Computer Science", 3);
        assertThrows(DuplicateEntityException.class, () -> bookService.addBook(b2));
    }

    @Test
    @DisplayName("Should validate required book fields")
    void testAddBook_ValidationErrors() {
        assertThrows(ValidationException.class, () -> bookService.addBook(null));
        assertThrows(ValidationException.class, () ->
                bookService.addBook(new Book("978-1111", "", "Author", "Genre", 5)));
        assertThrows(ValidationException.class, () ->
                bookService.addBook(new Book("978-1111", "Title", "", "Genre", 5)));
        assertThrows(ValidationException.class, () ->
                bookService.addBook(new Book("", "Title", "Author", "Genre", 5)));
        assertThrows(ValidationException.class, () ->
                bookService.addBook(new Book("978-1111", "Title", "Author", "", 5)));
        assertThrows(ValidationException.class, () ->
                bookService.addBook(new Book("978-1111", "Title", "Author", "Genre", 0)));
    }

    @Test
    @DisplayName("Should update book details and adjust stock copy invariants correctly")
    void testUpdateBook_Success() throws Exception {
        Book book = bookService.addBook(new Book("978-0001", "Initial Title", "Author", "Tech", 4, 3));
        // 1 copy is borrowed (4 total - 3 available = 1 borrowed)

        book.setTitle("Updated Title");
        book.setTotalCopies(6); // Increasing total copies to 6 -> new available copies should be 6 - 1 = 5

        Book updated = bookService.updateBook(book);
        assertEquals("Updated Title", updated.getTitle());
        assertEquals(6, updated.getTotalCopies());
        assertEquals(5, updated.getAvailableCopies());
    }

    @Test
    @DisplayName("Should reject reducing total copies below active borrowed count")
    void testUpdateBook_TotalCopiesBelowBorrowedCount() throws Exception {
        Book book = bookService.addBook(new Book("978-0002", "Contended Book", "Author", "Tech", 5, 2));
        // 3 copies borrowed (5 - 2 = 3)

        Book updateAttempt = new Book(book.getId(), "978-0002", "Contended Book", "Author", "Tech", 2, 2);
        assertThrows(ValidationException.class, () -> bookService.updateBook(updateAttempt));
    }

    @Test
    @DisplayName("Should reject duplicate ISBN during book update")
    void testUpdateBook_DuplicateIsbnCollision() throws Exception {
        Book b1 = bookService.addBook(new Book("978-1111", "Book 1", "Author 1", "Tech", 3));
        Book b2 = bookService.addBook(new Book("978-2222", "Book 2", "Author 2", "Tech", 3));

        b2.setIsbn("978-1111");
        assertThrows(DuplicateEntityException.class, () -> bookService.updateBook(b2));
    }

    @Test
    @DisplayName("Should throw BookNotFoundException when updating non-existent book")
    void testUpdateBook_NotFound() {
        Book phantom = new Book(9999, "978-9999", "Ghost Title", "Ghost Author", "Tech", 3, 3);
        assertThrows(BookNotFoundException.class, () -> bookService.updateBook(phantom));
    }

    @Test
    @DisplayName("Should delete book when no copies are borrowed")
    void testDeleteBook_Success() throws Exception {
        Book book = bookService.addBook(new Book("978-0003", "Disposable Book", "Author", "Fiction", 2, 2));

        boolean deleted = bookService.deleteBook(book.getId());
        assertTrue(deleted);
        assertTrue(bookService.getBookById(book.getId()).isEmpty());
    }

    @Test
    @DisplayName("Should prevent deleting book when copies are currently borrowed")
    void testDeleteBook_BlockedWhenBorrowed() throws Exception {
        Book book = bookService.addBook(new Book("978-0004", "Active Loan Book", "Author", "Fiction", 4, 3));
        // 1 borrowed

        assertThrows(LibraryException.class, () -> bookService.deleteBook(book.getId()));
    }

    @Test
    @DisplayName("Should search books and record search history for authenticated user")
    void testSearchBooks_WithSearchHistoryLogging() throws Exception {
        bookService.addBook(new Book("978-0005", "Core Java Volume I", "Cay Horstmann", "Tech", 5));
        bookService.addBook(new Book("978-0006", "Java Concurrency in Practice", "Brian Goetz", "Tech", 3));
        bookService.addBook(new Book("978-0007", "Clean Architecture", "Robert Martin", "Tech", 4));

        Member member = new Member("MEM-2001", "Searcher", "searcher@test.com", "hash");
        userDao.save(member);

        List<Book> results = bookService.searchBooks(member.getId(), "Java", "TITLE");
        assertEquals(2, results.size());

        List<SearchHistory> history = searchHistoryDao.findRecentByUserId(member.getId(), 5);
        assertEquals(1, history.size());
        assertEquals("Java", history.get(0).getQueryText());
        assertEquals("TITLE", history.get(0).getSearchType());
    }

    @Test
    @DisplayName("Should retrieve all books and unique genres")
    void testGetAllBooksAndGenres() throws Exception {
        bookService.addBook(new Book("978-0008", "Book A", "Author A", "Science", 2));
        bookService.addBook(new Book("978-0009", "Book B", "Author B", "History", 3));

        List<Book> allBooks = bookService.getAllBooks();
        assertEquals(2, allBooks.size());

        List<String> genres = bookService.getAllGenres();
        assertTrue(genres.contains("Science"));
        assertTrue(genres.contains("History"));
    }
}
