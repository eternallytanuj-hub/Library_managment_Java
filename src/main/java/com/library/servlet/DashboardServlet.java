package com.library.servlet;

import com.library.dao.dto.ReportSummaryDTO;
import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Notification;
import com.library.model.Transaction;
import com.library.model.User;
import com.library.service.BookService;
import com.library.service.MemberService;
import com.library.service.NotificationService;
import com.library.service.ReportService;
import com.library.service.TransactionService;
import com.library.service.impl.BookServiceImpl;
import com.library.service.impl.MemberServiceImpl;
import com.library.service.impl.NotificationServiceImpl;
import com.library.service.impl.ReportServiceImpl;
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
 * Controller servlet serving the role-tailored Librarian and Member Dashboards.
 */
@WebServlet(name = "DashboardServlet", urlPatterns = {"/librarian/dashboard", "/member/dashboard"})
public class DashboardServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DashboardServlet.class.getName());

    private final BookService bookService;
    private final MemberService memberService;
    private final TransactionService transactionService;
    private final NotificationService notificationService;
    private final ReportService reportService;

    public DashboardServlet() {
        this.bookService = new BookServiceImpl();
        this.memberService = new MemberServiceImpl();
        this.transactionService = new TransactionServiceImpl();
        this.notificationService = new NotificationServiceImpl();
        this.reportService = new ReportServiceImpl();
    }

    public DashboardServlet(BookService bookService, MemberService memberService,
                            TransactionService transactionService, NotificationService notificationService,
                            ReportService reportService) {
        this.bookService = bookService;
        this.memberService = memberService;
        this.transactionService = transactionService;
        this.notificationService = notificationService;
        this.reportService = reportService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        try {
            if ("/librarian/dashboard".equals(path)) {
                renderLibrarianDashboard(request, response);
            } else if ("/member/dashboard".equals(path)) {
                renderMemberDashboard(request, response);
            } else {
                response.sendRedirect(request.getContextPath() + "/index.jsp");
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error loading dashboard for path: " + path, ex);
            request.setAttribute("errorMessage", "Unable to load dashboard data: " + ex.getMessage());
            if (path.startsWith("/librarian")) {
                request.getRequestDispatcher("/WEB-INF/views/librarian/dashboard.jsp").forward(request, response);
            } else {
                request.getRequestDispatcher("/WEB-INF/views/member/dashboard.jsp").forward(request, response);
            }
        }
    }

    private void renderLibrarianDashboard(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, Exception {
        ReportSummaryDTO summary = reportService.getReportSummary();
        List<Book> books = bookService.getAllBooks();
        List<Member> members = memberService.getAllMembers();
        List<Transaction> transactions = transactionService.getAllTransactions(null);

        request.setAttribute("summary", summary);
        request.setAttribute("books", books);
        request.setAttribute("members", members);
        request.setAttribute("transactions", transactions);

        request.getRequestDispatcher("/WEB-INF/views/librarian/dashboard.jsp").forward(request, response);
    }

    private void renderMemberDashboard(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, Exception {
        HttpSession session = request.getSession(false);
        User loggedUser = (session != null) ? (User) session.getAttribute(AuthServlet.LOGGED_USER_SESSION_KEY) : null;

        if (loggedUser == null) {
            response.sendRedirect(request.getContextPath() + "/login?error=session_expired");
            return;
        }

        List<Transaction> activeLoans = transactionService.getActiveLoansByUserId(loggedUser.getId());
        List<Notification> notifications = notificationService.getNotificationsForUser(loggedUser.getId());
        int unreadCount = notificationService.getUnreadNotificationCount(loggedUser.getId());
        List<Book> allBooks = bookService.getAllBooks();

        request.setAttribute("activeLoans", activeLoans);
        request.setAttribute("notifications", notifications);
        request.setAttribute("unreadCount", unreadCount);
        request.setAttribute("books", allBooks);

        request.getRequestDispatcher("/WEB-INF/views/member/dashboard.jsp").forward(request, response);
    }
}
