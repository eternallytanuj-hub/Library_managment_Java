package com.library.service;

import com.library.exception.BookNotFoundException;
import com.library.exception.DatabaseOperationException;
import com.library.exception.DuplicateEntityException;
import com.library.exception.LibraryException;
import com.library.exception.ValidationException;
import com.library.model.Book;

import java.util.List;
import java.util.Optional;

/**
 * Service contract for library book catalog management, search queries,
 * inventory adjustments, and stock invariants.
 */
public interface BookService {

    Book addBook(Book book) throws DuplicateEntityException, ValidationException, DatabaseOperationException;

    Book updateBook(Book book) throws BookNotFoundException, DuplicateEntityException, ValidationException, DatabaseOperationException;

    boolean deleteBook(int bookId) throws LibraryException, DatabaseOperationException;

    Optional<Book> getBookById(int bookId) throws DatabaseOperationException;

    List<Book> searchBooks(String query, String searchType) throws DatabaseOperationException;

    List<Book> searchBooks(int userId, String query, String searchType) throws DatabaseOperationException;

    List<Book> getAllBooks() throws DatabaseOperationException;

    List<String> getAllGenres() throws DatabaseOperationException;
}
