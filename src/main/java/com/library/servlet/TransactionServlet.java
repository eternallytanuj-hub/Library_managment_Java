package com.library.servlet;

import com.library.exception.BookUnavailableException;
import com.library.exception.LibraryException;
import com.library.model.Transaction;
import com.library.model.User;
import com.library.service.TransactionService;
import com.library.service.impl.TransactionServiceImpl;
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
 * Controller servlet managing borrowing, returns, transaction ledgers, and history.
 */
@WebServlet(name = "TransactionServlet", urlPatterns = {
        "/librarian/transactions",
        "/member/borrow",
        "/member/return",
        "/member/history"
})
public class TransactionServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(TransactionServlet.class.getName());

    private final TransactionService transactionService;

    public TransactionServlet() {
        this.transactionService = new TransactionServiceImpl();
    }

    public TransactionServlet(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        try {
            if ("/librarian/transactions".equals(path)) {
                String status = request.getParameter("status");
                List<Transaction> transactions = transactionService.getAllTransactions(status);
                request.setAttribute("transactions", transactions);
                request.setAttribute("currentStatus", status);
                request.getRequestDispatcher("/WEB-INF/views/librarian/transactions.jsp").forward(request, response);
            } else if ("/member/history".equals(path)) {
                HttpSession session = request.getSession(false);
                User loggedUser = (session != null) ? (User) session.getAttribute(AuthServlet.LOGGED_USER_SESSION_KEY) : null;
                if (loggedUser == null) {
                    response.sendRedirect(request.getContextPath() + "/login?error=session_expired");
                    return;
                }
                List<Transaction> history = transactionService.getBorrowHistoryByUserId(loggedUser.getId());
                List<Transaction> activeLoans = transactionService.getActiveLoansByUserId(loggedUser.getId());
                request.setAttribute("history", history);
                request.setAttribute("activeLoans", activeLoans);
                request.getRequestDispatcher("/WEB-INF/views/member/history.jsp").forward(request, response);
            } else {
                response.sendRedirect(request.getContextPath() + "/index.jsp");
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error in TransactionServlet doGet: " + ex.getMessage(), ex);
            request.setAttribute("errorMessage", "Error loading transactions: " + ex.getMessage());
            if (path.startsWith("/librarian")) {
                request.getRequestDispatcher("/WEB-INF/views/librarian/transactions.jsp").forward(request, response);
            } else {
                request.getRequestDispatcher("/WEB-INF/views/member/history.jsp").forward(request, response);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        HttpSession session = request.getSession(false);
        User loggedUser = (session != null) ? (User) session.getAttribute(AuthServlet.LOGGED_USER_SESSION_KEY) : null;

        if (loggedUser == null) {
            response.sendRedirect(request.getContextPath() + "/login?error=session_expired");
            return;
        }

        try {
            switch (path) {
                case "/member/borrow" -> handleMemberBorrow(request, response, loggedUser);
                case "/member/return" -> handleMemberReturn(request, response, loggedUser);
                case "/librarian/transactions" -> handleLibrarianReturn(request, response, loggedUser);
                default -> response.sendRedirect(request.getContextPath() + "/index.jsp");
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unexpected error in TransactionServlet doPost", ex);
            response.sendRedirect(request.getContextPath() + "/index.jsp?error=server_error");
        }
    }

    private void handleMemberBorrow(HttpServletRequest request, HttpServletResponse response, User loggedUser)
            throws IOException {
        String bookIdStr = request.getParameter("bookId");
        if (bookIdStr == null || bookIdStr.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/member/search?error=missing_book_id");
            return;
        }

        try {
            int bookId = Integer.parseInt(bookIdStr);
            transactionService.borrowBook(loggedUser.getId(), bookId);
            response.sendRedirect(request.getContextPath() + "/member/dashboard?borrowed=true");
        } catch (BookUnavailableException ex) {
            LOGGER.log(Level.WARNING, "Borrowing unavailable book: " + ex.getMessage());
            response.sendRedirect(request.getContextPath() + "/member/search?error=book_unavailable");
        } catch (LibraryException ex) {
            LOGGER.log(Level.WARNING, "Borrow error: " + ex.getMessage());
            response.sendRedirect(request.getContextPath() + "/member/search?error=" + ex.getMessage());
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Borrow failed", ex);
            response.sendRedirect(request.getContextPath() + "/member/search?error=borrow_failed");
        }
    }

    private void handleMemberReturn(HttpServletRequest request, HttpServletResponse response, User loggedUser)
            throws IOException {
        String txIdStr = request.getParameter("transactionId");
        if (txIdStr == null || txIdStr.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/member/history?error=missing_id");
            return;
        }

        try {
            int txId = Integer.parseInt(txIdStr);
            transactionService.returnBook(txId, loggedUser.getId(), false);
            response.sendRedirect(request.getContextPath() + "/member/history?returned=true");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Return failed for txId: " + txIdStr, ex);
            response.sendRedirect(request.getContextPath() + "/member/history?error=return_failed");
        }
    }

    private void handleLibrarianReturn(HttpServletRequest request, HttpServletResponse response, User loggedUser)
            throws IOException {
        String txIdStr = request.getParameter("transactionId");
        if (txIdStr == null || txIdStr.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/librarian/transactions?error=missing_id");
            return;
        }

        try {
            int txId = Integer.parseInt(txIdStr);
            transactionService.returnBook(txId, loggedUser.getId(), true);
            response.sendRedirect(request.getContextPath() + "/librarian/transactions?returned=true");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Librarian return failed for txId: " + txIdStr, ex);
            response.sendRedirect(request.getContextPath() + "/librarian/transactions?error=return_failed");
        }
    }
}
