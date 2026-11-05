package com.library.service.impl;

import com.library.dao.BookDao;
import com.library.dao.SearchHistoryDao;
import com.library.dao.impl.BookDaoJdbcImpl;
import com.library.dao.impl.SearchHistoryDaoJdbcImpl;
import com.library.exception.BookNotFoundException;
import com.library.exception.DatabaseOperationException;
import com.library.exception.DuplicateEntityException;
import com.library.exception.LibraryException;
import com.library.exception.ValidationException;
import com.library.model.Book;
import com.library.model.SearchHistory;
import com.library.service.BookService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Concrete implementation of {@link BookService} governing catalog operations,
 * stock copy invariants, duplicate ISBN protection, and search tracking.
 */
public class BookServiceImpl implements BookService {

    private static final Logger LOGGER = Logger.getLogger(BookServiceImpl.class.getName());

    private final BookDao bookDao;
    private final SearchHistoryDao searchHistoryDao;

    public BookServiceImpl() {
        this(new BookDaoJdbcImpl(), new SearchHistoryDaoJdbcImpl());
    }

    public BookServiceImpl(BookDao bookDao) {
        this(bookDao, new SearchHistoryDaoJdbcImpl());
    }

    public BookServiceImpl(BookDao bookDao, SearchHistoryDao searchHistoryDao) {
        this.bookDao = Objects.requireNonNull(bookDao, "BookDao must not be null");
        this.searchHistoryDao = searchHistoryDao;
    }

    @Override
    public Book addBook(Book book) throws DuplicateEntityException, ValidationException, DatabaseOperationException {
        if (book == null) {
            throw new ValidationException("Book entity cannot be null");
        }
        validateBookFields(book);

        if (book.getTotalCopies() <= 0) {
            throw new ValidationException("totalCopies", "Total copies must be at least 1");
        }

        // Initialize availableCopies if not set or zero
        if (book.getAvailableCopies() <= 0) {
            book.setAvailableCopies(book.getTotalCopies());
        } else if (book.getAvailableCopies() > book.getTotalCopies()) {
            throw new ValidationException("availableCopies", "Available copies cannot exceed total copies");
        }

        // Verify duplicate ISBN
        Optional<Book> existing = bookDao.findByIsbn(book.getIsbn().trim());
        if (existing.isPresent()) {
            throw new DuplicateEntityException("Book", "ISBN", book.getIsbn().trim());
        }

        return bookDao.save(book);
    }

    @Override
    public Book updateBook(Book book) throws BookNotFoundException, DuplicateEntityException, ValidationException, DatabaseOperationException {
        if (book == null || book.getId() == null) {
            throw new ValidationException("Book and Book ID are required for update");
        }
        validateBookFields(book);

        Optional<Book> existingOpt = bookDao.findById(book.getId());
        if (existingOpt.isEmpty()) {
            throw new BookNotFoundException(book.getId());
        }

        Book existing = existingOpt.get();

        // Check duplicate ISBN if ISBN changed
        if (!existing.getIsbn().equalsIgnoreCase(book.getIsbn().trim())) {
            Optional<Book> other = bookDao.findByIsbn(book.getIsbn().trim());
            if (other.isPresent() && !other.get().getId().equals(book.getId())) {
                throw new DuplicateEntityException("Book", "ISBN", book.getIsbn().trim());
            }
        }

        // Adjust copy counts preserving currently borrowed copies invariant
        int borrowedCopies = existing.getBorrowedCopies();
        if (book.getTotalCopies() < borrowedCopies) {
            throw new ValidationException("totalCopies", "Total copies (" + book.getTotalCopies() + 
                    ") cannot be less than currently borrowed copies (" + borrowedCopies + ")");
        }

        int newAvailable = book.getTotalCopies() - borrowedCopies;
        book.setAvailableCopies(newAvailable);

        boolean updated = bookDao.update(book);
        if (!updated) {
            throw new DatabaseOperationException("Failed to update book: " + book.getId());
        }
        return book;
    }

    @Override
    public boolean deleteBook(int bookId) throws LibraryException, DatabaseOperationException {
        Optional<Book> bookOpt = bookDao.findById(bookId);
        if (bookOpt.isEmpty()) {
            throw new BookNotFoundException(bookId);
        }

        Book book = bookOpt.get();
        int borrowed = book.getBorrowedCopies();
        if (borrowed > 0) {
            throw new LibraryException("Cannot delete book '" + book.getTitle() + "': " + 
                    borrowed + " copies are currently borrowed.");
        }

        return bookDao.deleteById(bookId);
    }

    @Override
    public Optional<Book> getBookById(int bookId) throws DatabaseOperationException {
        return bookDao.findById(bookId);
    }

    @Override
    public List<Book> searchBooks(String query, String searchType) throws DatabaseOperationException {
        return searchBooks(0, query, searchType);
    }

    @Override
    public List<Book> searchBooks(int userId, String query, String searchType) throws DatabaseOperationException {
        List<Book> results = bookDao.searchBooks(query, searchType);

        if (userId > 0 && query != null && !query.trim().isEmpty() && searchHistoryDao != null) {
            try {
                searchHistoryDao.save(new SearchHistory(userId, query.trim(), searchType));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to log search history for user " + userId, e);
            }
        }

        return results;
    }

    @Override
    public List<Book> getAllBooks() throws DatabaseOperationException {
        return bookDao.findAll();
    }

    @Override
    public List<String> getAllGenres() throws DatabaseOperationException {
        return bookDao.findAllGenres();
    }

    private void validateBookFields(Book book) throws ValidationException {
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            throw new ValidationException("title", "Title cannot be empty");
        }
        if (book.getAuthor() == null || book.getAuthor().trim().isEmpty()) {
            throw new ValidationException("author", "Author cannot be empty");
        }
        if (book.getIsbn() == null || book.getIsbn().trim().isEmpty()) {
            throw new ValidationException("isbn", "ISBN cannot be empty");
        }
        if (book.getGenre() == null || book.getGenre().trim().isEmpty()) {
            throw new ValidationException("genre", "Genre cannot be empty");
        }
    }
}
