<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.library.model.Transaction, java.util.List" %>
<%
    List<Transaction> history = (List<Transaction>) request.getAttribute("history");
    List<Transaction> activeLoans = (List<Transaction>) request.getAttribute("activeLoans");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Borrowing History - Online Library System</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/style.css">
</head>
<body>
    <header class="navbar">
        <a href="<%= request.getContextPath() %>/member/dashboard" class="navbar-brand">Member Portal</a>
        <nav>
            <ul class="navbar-menu">
                <li><a href="<%= request.getContextPath() %>/member/dashboard">Dashboard</a></li>
                <li><a href="<%= request.getContextPath() %>/member/search">Catalog</a></li>
                <li><a href="<%= request.getContextPath() %>/member/history" class="active">Borrowing History</a></li>
                <li><a href="<%= request.getContextPath() %>/member/notifications">Notifications</a></li>
                <li><a href="<%= request.getContextPath() %>/member/profile">Profile</a></li>
                <li><a href="<%= request.getContextPath() %>/logout" class="btn btn-secondary btn-sm">Sign Out</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <div class="page-header">
            <div>
                <h1 class="page-title">Personal Borrowing History</h1>
                <p class="page-subtitle">Complete ledger of your active and returned library book loans</p>
            </div>
            <a href="<%= request.getContextPath() %>/member/search" class="btn btn-primary btn-sm">+ Borrow Book</a>
        </div>

        <% if ("true".equals(request.getParameter("returned"))) { %>
            <div class="alert alert-success">Book successfully returned! Thank you for keeping our catalog in circulation.</div>
        <% } %>

        <div class="card">
            <div class="card-header">
                <h2 class="card-title">All Loan Transactions (<%= history != null ? history.size() : 0 %>)</h2>
            </div>

            <div class="table-responsive">
                <table class="table">
                    <thead>
                        <tr>
                            <th>Tx ID</th>
                            <th>Book Title</th>
                            <th>Borrow Date</th>
                            <th>Due Date</th>
                            <th>Return Date</th>
                            <th>Status</th>
                            <th>Fine</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (history != null && !history.isEmpty()) { 
                            for (Transaction tx : history) {
                        %>
                        <tr>
                            <td>#<%= tx.getId() %></td>
                            <td><strong><%= tx.getBook() != null ? tx.getBook().getTitle() : "Book #" + tx.getBookId() %></strong></td>
                            <td><%= tx.getBorrowDate() != null ? tx.getBorrowDate().toString().substring(0, 10) : "-" %></td>
                            <td><%= tx.getDueDate() != null ? tx.getDueDate().toString().substring(0, 10) : "-" %></td>
                            <td><%= tx.getReturnDate() != null ? tx.getReturnDate().toString().substring(0, 10) : "-" %></td>
                            <td>
                                <% if ("RETURNED".equalsIgnoreCase(String.valueOf(tx.getStatus()))) { %>
                                    <span class="badge badge-success">Returned</span>
                                <% } else if ("OVERDUE".equalsIgnoreCase(String.valueOf(tx.getStatus()))) { %>
                                    <span class="badge badge-danger">Overdue</span>
                                <% } else { %>
                                    <span class="badge badge-warning">Borrowed</span>
                                <% } %>
                            </td>
                            <td>
                                <% if (tx.getFineAmountDouble() > 0) { %>
                                    <span style="color: var(--danger); font-weight: 700;">$<%= String.format("%.2f", tx.getFineAmountDouble()) %></span>
                                <% } else { %>
                                    <span>$0.00</span>
                                <% } %>
                            </td>
                            <td>
                                <% if (!"RETURNED".equalsIgnoreCase(String.valueOf(tx.getStatus()))) { %>
                                <form action="<%= request.getContextPath() %>/member/return" method="post" style="display:inline;">
                                    <input type="hidden" name="transactionId" value="<%= tx.getId() %>">
                                    <button type="submit" class="btn btn-success btn-sm">Return Book</button>
                                </form>
                                <% } else { %>
                                    <span style="color: var(--text-muted); font-size: 0.8rem;">Returned</span>
                                <% } %>
                            </td>
                        </tr>
                        <%  } 
                           } else { %>
                        <tr>
                            <td colspan="8" style="text-align:center; color: var(--text-muted); padding: 32px;">No borrowing records found.</td>
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
