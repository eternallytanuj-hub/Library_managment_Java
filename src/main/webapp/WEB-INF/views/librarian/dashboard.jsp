<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.library.model.User, com.library.model.Book, com.library.model.Member, com.library.model.Transaction, com.library.dao.dto.ReportSummaryDTO, java.util.List" %>
<%
    User loggedUser = (User) session.getAttribute("loggedUser");
    ReportSummaryDTO summary = (ReportSummaryDTO) request.getAttribute("summary");
    List<Transaction> transactions = (List<Transaction>) request.getAttribute("transactions");
    List<Book> books = (List<Book>) request.getAttribute("books");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Librarian Dashboard - Online Library Management System</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/style.css">
</head>
<body>
    <header class="navbar">
        <a href="<%= request.getContextPath() %>/librarian/dashboard" class="navbar-brand">Library Staff Portal</a>
        <nav>
            <ul class="navbar-menu">
                <li><a href="<%= request.getContextPath() %>/librarian/dashboard" class="active">Dashboard</a></li>
                <li><a href="<%= request.getContextPath() %>/librarian/books">Books</a></li>
                <li><a href="<%= request.getContextPath() %>/librarian/members">Members</a></li>
                <li><a href="<%= request.getContextPath() %>/librarian/transactions">Transactions</a></li>
                <li><a href="<%= request.getContextPath() %>/librarian/notifications">Notifications</a></li>
                <li><a href="<%= request.getContextPath() %>/librarian/reports">Reports</a></li>
                <li><a href="<%= request.getContextPath() %>/logout" class="btn btn-secondary btn-sm">Sign Out</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <div class="page-header">
            <div>
                <h1 class="page-title">Staff Overview</h1>
                <p class="page-subtitle">Welcome, <%= loggedUser != null ? loggedUser.getName() : "Staff" %> &bull; System Health & Active Catalog</p>
            </div>
            <div>
                <a href="<%= request.getContextPath() %>/librarian/books" class="btn btn-primary btn-sm">+ Add New Book</a>
                <a href="<%= request.getContextPath() %>/librarian/notifications" class="btn btn-secondary btn-sm">Broadcast Alert</a>
            </div>
        </div>

        <% if (summary != null) { %>
        <div class="metrics-grid">
            <div class="metric-card">
                <div class="metric-label">Total Titles</div>
                <div class="metric-value primary"><%= summary.getTotalBooks() %></div>
            </div>
            <div class="metric-card">
                <div class="metric-label">Total Copies</div>
                <div class="metric-value"><%= summary.getTotalCopies() %></div>
            </div>
            <div class="metric-card">
                <div class="metric-label">Available Copies</div>
                <div class="metric-value success"><%= summary.getAvailableCopies() %></div>
            </div>
            <div class="metric-card">
                <div class="metric-label">Borrowed Copies</div>
                <div class="metric-value warning"><%= summary.getBorrowedCopies() %></div>
            </div>
            <div class="metric-card">
                <div class="metric-label">Overdue Loans</div>
                <div class="metric-value danger"><%= summary.getOverdueLoans() %></div>
            </div>
            <div class="metric-card">
                <div class="metric-label">Active Members</div>
                <div class="metric-value"><%= summary.getTotalMembers() %></div>
            </div>
        </div>
        <% } %>

        <div class="card">
            <div class="card-header">
                <h2 class="card-title">Recent Transactions Ledger</h2>
                <a href="<%= request.getContextPath() %>/librarian/transactions" class="btn btn-secondary btn-sm">View All Transactions</a>
            </div>

            <div class="table-responsive">
                <table class="table">
                    <thead>
                        <tr>
                            <th>Tx ID</th>
                            <th>Book Title</th>
                            <th>Member</th>
                            <th>Borrow Date</th>
                            <th>Due Date</th>
                            <th>Status</th>
                            <th>Fine</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (transactions != null && !transactions.isEmpty()) { 
                            int limit = Math.min(transactions.size(), 8);
                            for (int i = 0; i < limit; i++) {
                                Transaction tx = transactions.get(i);
                        %>
                        <tr>
                            <td>#<%= tx.getId() %></td>
                            <td><strong><%= tx.getBook() != null ? tx.getBook().getTitle() : "Book #" + tx.getBookId() %></strong></td>
                            <td><%= tx.getUser() != null ? tx.getUser().getName() : "Member #" + tx.getUserId() %></td>
                            <td><%= tx.getBorrowDate() != null ? tx.getBorrowDate().toString().substring(0, 10) : "-" %></td>
                            <td><%= tx.getDueDate() != null ? tx.getDueDate().toString().substring(0, 10) : "-" %></td>
                            <td>
                                <% if ("RETURNED".equalsIgnoreCase(String.valueOf(tx.getStatus()))) { %>
                                    <span class="badge badge-success">Returned</span>
                                <% } else if ("OVERDUE".equalsIgnoreCase(String.valueOf(tx.getStatus()))) { %>
                                    <span class="badge badge-danger">Overdue</span>
                                <% } else { %>
                                    <span class="badge badge-warning">Borrowed</span>
                                <% } %>
                            </td>
                            <td>$<%= String.format("%.2f", tx.getFineAmountDouble()) %></td>
                            <td>
                                <% if (!"RETURNED".equalsIgnoreCase(String.valueOf(tx.getStatus()))) { %>
                                <form action="<%= request.getContextPath() %>/librarian/transactions" method="post" style="display:inline;">
                                    <input type="hidden" name="transactionId" value="<%= tx.getId() %>">
                                    <button type="submit" class="btn btn-success btn-sm">Return</button>
                                </form>
                                <% } else { %>
                                    <span style="color: var(--text-muted); font-size: 0.8rem;">Complete</span>
                                <% } %>
                            </td>
                        </tr>
                        <%  } 
                           } else { %>
                        <tr>
                            <td colspan="8" style="text-align:center; color: var(--text-muted); padding: 32px;">No active transactions found.</td>
                        </tr>
                        <% } %>
                    </tbody>
                </table>
            </div>
        </div>
    </main>

    <script src="<%= request.getContextPath() %>/js/app.js"></script>
</body>
</html>
