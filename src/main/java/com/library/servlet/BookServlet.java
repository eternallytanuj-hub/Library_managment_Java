package com.library.servlet;

import com.library.exception.BookNotFoundException;
import com.library.exception.DuplicateEntityException;
import com.library.exception.LibraryException;
import com.library.exception.ValidationException;
import com.library.model.Book;
import com.library.model.User;
import com.library.service.BookService;
import com.library.service.impl.BookServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller servlet managing the book catalog, CRUD modifications, and catalog search.
 */
@WebServlet(name = "BookServlet", urlPatterns = {"/librarian/books", "/member/books", "/member/search"})
public class BookServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(BookServlet.class.getName());

    private final BookService bookService;

    public BookServlet() {
        this.bookService = new BookServiceImpl();
    }

    public BookServlet(BookService bookService) {
        this.bookService = bookService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        try {
            if ("/librarian/books".equals(path)) {
                List<Book> books = bookService.getAllBooks();
                List<String> genres = bookService.getAllGenres();
                request.setAttribute("books", books);
                request.setAttribute("genres", genres);
                request.getRequestDispatcher("/WEB-INF/views/librarian/books.jsp").forward(request, response);
            } else if ("/member/search".equals(path) || "/member/books".equals(path)) {
                handleMemberSearch(request, response);
            } else {
                response.sendRedirect(request.getContextPath() + "/index.jsp");
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error in BookServlet doGet: " + ex.getMessage(), ex);
            request.setAttribute("errorMessage", "Error loading books: " + ex.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/librarian/books.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        if ("/librarian/books".equals(path)) {
            handleLibrarianBookMutation(request, response);
        } else {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
        }
    }

    private void handleMemberSearch(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, Exception {
        String query = request.getParameter("q");
        String searchType = request.getParameter("type");
        if (searchType == null || searchType.isBlank()) {
            searchType = request.getParameter("searchType");
        }

        HttpSession session = request.getSession(false);
        User loggedUser = (session != null) ? (User) session.getAttribute(AuthServlet.LOGGED_USER_SESSION_KEY) : null;
        Integer userId = (loggedUser != null) ? loggedUser.getId() : null;

        List<Book> searchResults;
        if (query != null && !query.trim().isEmpty()) {
            if (userId != null) {
                searchResults = bookService.searchBooks(userId, query.trim(), searchType);
            } else {
                searchResults = bookService.searchBooks(query.trim(), searchType);
            }
        } else {
            searchResults = bookService.getAllBooks();
        }

        request.setAttribute("query", query);
        request.setAttribute("searchType", searchType);
        request.setAttribute("books", searchResults);
        request.setAttribute("genres", bookService.getAllGenres());

        request.getRequestDispatcher("/WEB-INF/views/member/search.jsp").forward(request, response);
    }

    private void handleLibrarianBookMutation(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String action = request.getParameter("action");
        String deleteParam = request.getParameter("delete");
        String bookIdStr = request.getParameter("bookId");

        // 1. Delete Book
        if ("delete".equalsIgnoreCase(action) || "true".equalsIgnoreCase(deleteParam)) {
            try {
                int bookId = Integer.parseInt(bookIdStr);
                bookService.deleteBook(bookId);
                response.sendRedirect(request.getContextPath() + "/librarian/books?deleted=true");
            } catch (LibraryException ex) {
                LOGGER.log(Level.WARNING, "Cannot delete book: " + ex.getMessage());
                response.sendRedirect(request.getContextPath() + "/librarian/books?error=active_loans");
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error deleting book", ex);
                response.sendRedirect(request.getContextPath() + "/librarian/books?error=delete_failed");
            }
            return;
        }

        // Common Fields for Add and Update
        String title = request.getParameter("title");
        String author = request.getParameter("author");
        String isbn = request.getParameter("isbn");
        String genre = request.getParameter("genre");
        String copiesStr = request.getParameter("totalCopies");

        int totalCopies = 1;
        try {
            if (copiesStr != null && !copiesStr.isBlank()) {
                totalCopies = Integer.parseInt(copiesStr.trim());
            }
        } catch (NumberFormatException ignored) {}

        // 2. Update Existing Book
        if ("update".equalsIgnoreCase(action) || "edit".equalsIgnoreCase(action) || (bookIdStr != null && !bookIdStr.isBlank() && Integer.parseInt(bookIdStr) > 0)) {
            try {
                int bookId = Integer.parseInt(bookIdStr);
                Book existingBook = bookService.getBookById(bookId).orElseThrow(() -> new BookNotFoundException("Book not found"));
                existingBook.setTitle(title);
                existingBook.setAuthor(author);
                existingBook.setIsbn(isbn);
                existingBook.setGenre(genre);
                existingBook.setTotalCopies(totalCopies);

                bookService.updateBook(existingBook);
                response.sendRedirect(request.getContextPath() + "/librarian/books?updated=true");
            } catch (DuplicateEntityException ex) {
                response.sendRedirect(request.getContextPath() + "/librarian/books?error=duplicate_isbn");
            } catch (ValidationException | BookNotFoundException ex) {
                response.sendRedirect(request.getContextPath() + "/librarian/books?error=validation_failed");
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error updating book", ex);
                response.sendRedirect(request.getContextPath() + "/librarian/books?error=server_error");
            }
            return;
        }

        // 3. Add New Book
        try {
            Book newBook = new Book(isbn, title, author, genre, totalCopies, totalCopies);
            bookService.addBook(newBook);
            response.sendRedirect(request.getContextPath() + "/librarian/books?added=true");
        } catch (DuplicateEntityException ex) {
            response.sendRedirect(request.getContextPath() + "/librarian/books?error=duplicate_isbn");
        } catch (ValidationException ex) {
            response.sendRedirect(request.getContextPath() + "/librarian/books?error=validation_failed");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error adding book", ex);
            response.sendRedirect(request.getContextPath() + "/librarian/books?error=server_error");
        }
    }
}
