<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.library.model.User, com.library.model.Transaction, com.library.model.Notification, com.library.model.Book, java.util.List" %>
<%
    User loggedUser = (User) session.getAttribute("loggedUser");
    List<Transaction> activeLoans = (List<Transaction>) request.getAttribute("activeLoans");
    List<Notification> notifications = (List<Notification>) request.getAttribute("notifications");
    int unreadCount = request.getAttribute("unreadCount") != null ? (Integer) request.getAttribute("unreadCount") : 0;
    List<Book> books = (List<Book>) request.getAttribute("books");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Member Dashboard - Online Library System</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/style.css">
</head>
<body>
    <header class="navbar">
        <a href="<%= request.getContextPath() %>/member/dashboard" class="navbar-brand">Member Portal</a>
        <nav>
            <ul class="navbar-menu">
                <li><a href="<%= request.getContextPath() %>/member/dashboard" class="active">Dashboard</a></li>
                <li><a href="<%= request.getContextPath() %>/member/search">Catalog</a></li>
                <li><a href="<%= request.getContextPath() %>/member/history">Borrowing History</a></li>
                <li>
                    <a href="<%= request.getContextPath() %>/member/notifications">
                        Notifications
                        <% if (unreadCount > 0) { %>
                            <span class="badge badge-primary"><%= unreadCount %></span>
                        <% } %>
                    </a>
                </li>
                <li><a href="<%= request.getContextPath() %>/member/profile">Profile</a></li>
                <li><a href="<%= request.getContextPath() %>/logout" class="btn btn-secondary btn-sm">Sign Out</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <div class="page-header">
            <div>
                <h1 class="page-title">Welcome, <%= loggedUser != null ? loggedUser.getName() : "Member" %></h1>
                <p class="page-subtitle">Membership ID: <code><%= loggedUser != null ? loggedUser.getMembershipId() : "N/A" %></code> &bull; Standard 14-day borrowing duration</p>
            </div>
            <div>
                <a href="<%= request.getContextPath() %>/member/search" class="btn btn-primary btn-sm">Search Catalog</a>
            </div>
        </div>

        <% if ("true".equals(request.getParameter("borrowed"))) { %>
            <div class="alert alert-success">Book successfully borrowed! Please return on or before your due date.</div>
        <% } %>

        <div class="card">
            <div class="card-header">
                <h2 class="card-title">Currently Borrowed Books (<%= activeLoans != null ? activeLoans.size() : 0 %>)</h2>
                <a href="<%= request.getContextPath() %>/member/search" class="btn btn-secondary btn-sm">+ Borrow Another</a>
            </div>

            <div class="table-responsive">
                <table class="table">
                    <thead>
                        <tr>
                            <th>Book Title</th>
                            <th>Author</th>
                            <th>Borrowed On</th>
                            <th>Due Date</th>
                            <th>Status</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (activeLoans != null && !activeLoans.isEmpty()) { 
                            for (Transaction tx : activeLoans) {
                        %>
                        <tr>
                            <td><strong><%= tx.getBook() != null ? tx.getBook().getTitle() : "Book #" + tx.getBookId() %></strong></td>
                            <td><%= tx.getBook() != null ? tx.getBook().getAuthor() : "-" %></td>
                            <td><%= tx.getBorrowDate() != null ? tx.getBorrowDate().toString().substring(0, 10) : "-" %></td>
                            <td><%= tx.getDueDate() != null ? tx.getDueDate().toString().substring(0, 10) : "-" %></td>
                            <td>
                                <% if ("OVERDUE".equalsIgnoreCase(String.valueOf(tx.getStatus()))) { %>
                                    <span class="badge badge-danger">Overdue ($<%= String.format("%.2f", tx.getFineAmountDouble()) %>)</span>
                                <% } else { %>
                                    <span class="badge badge-warning">Active Loan</span>
                                <% } %>
                            </td>
                            <td>
                                <form action="<%= request.getContextPath() %>/member/return" method="post" style="display:inline;">
                                    <input type="hidden" name="transactionId" value="<%= tx.getId() %>">
                                    <button type="submit" class="btn btn-success btn-sm">Return Book</button>
                                </form>
                            </td>
                        </tr>
                        <%  } 
                           } else { %>
                        <tr>
                            <td colspan="6" style="text-align:center; color: var(--text-muted); padding: 32px;">You do not currently have any borrowed books. Browse the catalog to borrow one!</td>
                        </tr>
                        <% } %>
                    </tbody>
                </table>
            </div>
        </div>

        <div class="charts-grid">
            <div class="card" style="margin-bottom: 0;">
                <div class="card-header">
                    <h3 class="card-title">Recent Alerts & Updates</h3>
                    <a href="<%= request.getContextPath() %>/member/notifications" class="btn btn-secondary btn-sm">All Alerts</a>
                </div>
                <% if (notifications != null && !notifications.isEmpty()) { 
                    int previewLimit = Math.min(notifications.size(), 3);
                    for (int i = 0; i < previewLimit; i++) {
                        Notification n = notifications.get(i);
                %>
                <div class="notification-item <%= !n.isRead() ? "unread" : "" %>">
                    <div>
                        <div class="notification-title"><%= n.getTitle() %></div>
                        <div style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 4px;"><%= n.getMessage() %></div>
                        <div class="notification-time"><%= n.getCreatedAt() != null ? n.getCreatedAt().toString().substring(0, 16) : "" %></div>
                    </div>
                </div>
                <%  } 
                   } else { %>
                    <p style="color: var(--text-muted); text-align: center; padding: 24px;">No new alerts.</p>
                <% } %>
            </div>

            <div class="card" style="margin-bottom: 0;">
                <h3 class="card-title" style="margin-bottom: 16px;">Search Library Catalog</h3>
                <form action="<%= request.getContextPath() %>/member/search" method="get">
                    <div class="form-group">
                        <label class="form-label">Search Query</label>
                        <input type="text" name="q" class="form-control" placeholder="Search by title, author, or keyword...">
                    </div>
                    <div class="form-group">
                        <label class="form-label">Search By</label>
                        <select name="type" class="form-control">
                            <option value="TITLE">Book Title</option>
                            <option value="AUTHOR">Author</option>
                            <option value="GENRE">Genre</option>
                        </select>
                    </div>
                    <button type="submit" class="btn btn-primary" style="width: 100%;">Search Catalog</button>
                </form>
            </div>
        </div>
    </main>

    <script src="<%= request.getContextPath() %>/js/app.js"></script>
</body>
</html>
