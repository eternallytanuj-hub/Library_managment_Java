package com.library.dao.impl;

import com.library.dao.BookDao;
import com.library.exception.DatabaseOperationException;
import com.library.exception.DuplicateEntityException;
import com.library.model.Book;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link BookDao} with parameterized queries and atomic inventory operations.
 */
public class BookDaoJdbcImpl extends BaseJdbcDao implements BookDao {

    @Override
    public Optional<Book> findById(Integer id) throws DatabaseOperationException {
        if (id == null) return Optional.empty();
        String sql = "SELECT * FROM books WHERE book_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToBook(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find book by ID: " + id, e);
        }
    }

    @Override
    public List<Book> findAll() throws DatabaseOperationException {
        String sql = "SELECT * FROM books ORDER BY title ASC";
        List<Book> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToBook(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to list all books", e);
        }
    }

    @Override
    public Book save(Book book) throws DatabaseOperationException, DuplicateEntityException {
        String sql = "INSERT INTO books (isbn, title, author, genre, total_copies, available_copies, added_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Timestamp now = book.getCreatedAt() != null ? book.getCreatedAt() : new Timestamp(System.currentTimeMillis());
            ps.setString(1, book.getIsbn());
            ps.setString(2, book.getTitle());
            ps.setString(3, book.getAuthor());
            ps.setString(4, book.getGenre());
            ps.setInt(5, book.getTotalCopies());
            ps.setInt(6, book.getAvailableCopies());
            ps.setTimestamp(7, now);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DatabaseOperationException("Creating book failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    book.setId(generatedKeys.getInt(1));
                } else {
                    throw new DatabaseOperationException("Creating book failed, no ID obtained.");
                }
            }
            book.setCreatedAt(now);
            book.setUpdatedAt(now);
            return book;
        } catch (SQLException e) {
            throw translateException("save book", e);
        }
    }

    @Override
    public boolean update(Book book) throws DatabaseOperationException, DuplicateEntityException {
        String sql = "UPDATE books SET isbn = ?, title = ?, author = ?, genre = ?, " +
                     "total_copies = ?, available_copies = ? WHERE book_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, book.getIsbn());
            ps.setString(2, book.getTitle());
            ps.setString(3, book.getAuthor());
            ps.setString(4, book.getGenre());
            ps.setInt(5, book.getTotalCopies());
            ps.setInt(6, book.getAvailableCopies());
            ps.setInt(7, book.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw translateException("update book", e);
        }
    }

    @Override
    public boolean deleteById(Integer id) throws DatabaseOperationException {
        if (id == null) return false;
        String sql = "DELETE FROM books WHERE book_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to delete book with ID: " + id, e);
        }
    }

    @Override
    public Optional<Book> findByIsbn(String isbn) throws DatabaseOperationException {
        if (isbn == null) return Optional.empty();
        String sql = "SELECT * FROM books WHERE isbn = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, isbn.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToBook(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find book by ISBN: " + isbn, e);
        }
    }

    @Override
    public List<Book> searchBooks(String query, String searchType) throws DatabaseOperationException {
        String sql;
        String param = "%" + (query != null ? query.trim().toLowerCase() : "") + "%";

        if (searchType != null && searchType.equalsIgnoreCase("TITLE")) {
            sql = "SELECT * FROM books WHERE LOWER(title) LIKE ? ORDER BY title ASC";
        } else if (searchType != null && searchType.equalsIgnoreCase("AUTHOR")) {
            sql = "SELECT * FROM books WHERE LOWER(author) LIKE ? ORDER BY title ASC";
        } else if (searchType != null && searchType.equalsIgnoreCase("GENRE")) {
            sql = "SELECT * FROM books WHERE LOWER(genre) LIKE ? ORDER BY title ASC";
        } else {
            sql = "SELECT * FROM books WHERE LOWER(title) LIKE ? OR LOWER(author) LIKE ? OR LOWER(genre) LIKE ? ORDER BY title ASC";
        }

        List<Book> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (searchType != null && (searchType.equalsIgnoreCase("TITLE") ||
                    searchType.equalsIgnoreCase("AUTHOR") || searchType.equalsIgnoreCase("GENRE"))) {
                ps.setString(1, param);
            } else {
                ps.setString(1, param);
                ps.setString(2, param);
                ps.setString(3, param);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToBook(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to search books with query: " + query, e);
        }
    }

    @Override
    public List<Book> findByGenre(String genre) throws DatabaseOperationException {
        String sql = "SELECT * FROM books WHERE LOWER(genre) = LOWER(?) ORDER BY title ASC";
        List<Book> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, genre);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToBook(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find books by genre: " + genre, e);
        }
    }

    @Override
    public List<String> findAllGenres() throws DatabaseOperationException {
        String sql = "SELECT DISTINCT genre FROM books ORDER BY genre ASC";
        List<String> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString("genre"));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to retrieve genres", e);
        }
    }

    @Override
    public int countTotalTitles() throws DatabaseOperationException {
        String sql = "SELECT COUNT(*) FROM books";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
            return 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to count total titles", e);
        }
    }

    @Override
    public int countTotalCopies() throws DatabaseOperationException {
        String sql = "SELECT COALESCE(SUM(total_copies), 0) FROM books";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
            return 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to count total copies", e);
        }
    }

    @Override
    public int countAvailableCopies() throws DatabaseOperationException {
        String sql = "SELECT COALESCE(SUM(available_copies), 0) FROM books";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
            return 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to count available copies", e);
        }
    }

    @Override
    public boolean decrementAvailableCopies(Connection conn, int bookId) throws SQLException {
        String sql = "UPDATE books SET available_copies = available_copies - 1 " +
                     "WHERE book_id = ? AND available_copies > 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public boolean incrementAvailableCopies(Connection conn, int bookId) throws SQLException {
        String sql = "UPDATE books SET available_copies = available_copies + 1 " +
                     "WHERE book_id = ? AND available_copies < total_copies";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            return ps.executeUpdate() == 1;
        }
    }

    protected Book mapResultSetToBook(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setId(rs.getInt("book_id"));
        book.setIsbn(rs.getString("isbn"));
        book.setTitle(rs.getString("title"));
        book.setAuthor(rs.getString("author"));
        book.setGenre(rs.getString("genre"));
        book.setTotalCopies(rs.getInt("total_copies"));
        book.setAvailableCopies(rs.getInt("available_copies"));
        book.setCreatedAt(rs.getTimestamp("added_at"));
        book.setUpdatedAt(rs.getTimestamp("added_at"));
        return book;
    }
}
