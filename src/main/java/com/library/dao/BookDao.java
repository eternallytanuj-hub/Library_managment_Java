package com.library.dao;

import com.library.model.Book;
import com.library.exception.DatabaseOperationException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for {@link Book} catalog entities.
 */
public interface BookDao extends GenericDao<Book, Integer> {

    Optional<Book> findByIsbn(String isbn) throws DatabaseOperationException;

    List<Book> searchBooks(String query, String searchType) throws DatabaseOperationException;

    List<Book> findByGenre(String genre) throws DatabaseOperationException;

    List<String> findAllGenres() throws DatabaseOperationException;

    int countTotalTitles() throws DatabaseOperationException;

    int countTotalCopies() throws DatabaseOperationException;

    int countAvailableCopies() throws DatabaseOperationException;

    boolean decrementAvailableCopies(Connection conn, int bookId) throws SQLException;

    boolean incrementAvailableCopies(Connection conn, int bookId) throws SQLException;
}
