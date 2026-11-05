<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.library.model.Transaction, java.util.List" %>
<%
    List<Transaction> transactions = (List<Transaction>) request.getAttribute("transactions");
    String currentStatus = (String) request.getAttribute("currentStatus");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Transaction Ledger - Online Library System</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/style.css">
</head>
<body>
    <header class="navbar">
        <a href="<%= request.getContextPath() %>/librarian/dashboard" class="navbar-brand">Library Staff Portal</a>
        <nav>
            <ul class="navbar-menu">
                <li><a href="<%= request.getContextPath() %>/librarian/dashboard">Dashboard</a></li>
                <li><a href="<%= request.getContextPath() %>/librarian/books">Books</a></li>
                <li><a href="<%= request.getContextPath() %>/librarian/members">Members</a></li>
                <li><a href="<%= request.getContextPath() %>/librarian/transactions" class="active">Transactions</a></li>
                <li><a href="<%= request.getContextPath() %>/librarian/notifications">Notifications</a></li>
                <li><a href="<%= request.getContextPath() %>/librarian/reports">Reports</a></li>
                <li><a href="<%= request.getContextPath() %>/logout" class="btn btn-secondary btn-sm">Sign Out</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <div class="page-header">
            <div>
                <h1 class="page-title">Transaction Ledger</h1>
                <p class="page-subtitle">Track historical and active book borrowings, returns, and overdue fines</p>
            </div>
            <div style="display: flex; gap: 8px;">
                <a href="<%= request.getContextPath() %>/librarian/transactions" class="btn btn-sm <%= currentStatus == null || currentStatus.isBlank() ? "btn-primary" : "btn-secondary" %>">All</a>
                <a href="<%= request.getContextPath() %>/librarian/transactions?status=BORROWED" class="btn btn-sm <%= "BORROWED".equalsIgnoreCase(currentStatus) ? "btn-primary" : "btn-secondary" %>">Active Loans</a>
                <a href="<%= request.getContextPath() %>/librarian/transactions?status=OVERDUE" class="btn btn-sm <%= "OVERDUE".equalsIgnoreCase(currentStatus) ? "btn-primary" : "btn-secondary" %>">Overdue</a>
                <a href="<%= request.getContextPath() %>/librarian/transactions?status=RETURNED" class="btn btn-sm <%= "RETURNED".equalsIgnoreCase(currentStatus) ? "btn-primary" : "btn-secondary" %>">Returned</a>
            </div>
        </div>

        <% if ("true".equals(request.getParameter("returned"))) { %>
            <div class="alert alert-success">Book successfully recorded as returned! Stock inventory incremented.</div>
        <% } %>

        <div class="card">
            <div class="table-responsive">
                <table class="table">
                    <thead>
                        <tr>
                            <th>Tx ID</th>
                            <th>Book Title</th>
                            <th>Member</th>
                            <th>Borrow Date</th>
                            <th>Due Date</th>
                            <th>Return Date</th>
                            <th>Status</th>
                            <th>Fine</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (transactions != null && !transactions.isEmpty()) { 
                            for (Transaction tx : transactions) {
                        %>
                        <tr>
                            <td>#<%= tx.getId() %></td>
                            <td><strong><%= tx.getBook() != null ? tx.getBook().getTitle() : "Book #" + tx.getBookId() %></strong></td>
                            <td><%= tx.getUser() != null ? tx.getUser().getName() : "Member #" + tx.getUserId() %></td>
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
                                <form action="<%= request.getContextPath() %>/librarian/transactions" method="post" style="display:inline;">
                                    <input type="hidden" name="transactionId" value="<%= tx.getId() %>">
                                    <button type="submit" class="btn btn-success btn-sm">Mark Returned</button>
                                </form>
                                <% } else { %>
                                    <span style="color: var(--text-muted); font-size: 0.8rem;">Returned</span>
                                <% } %>
                            </td>
                        </tr>
                        <%  } 
                           } else { %>
                        <tr>
                            <td colspan="9" style="text-align:center; color: var(--text-muted); padding: 32px;">No transactions matching the selected criteria.</td>
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
