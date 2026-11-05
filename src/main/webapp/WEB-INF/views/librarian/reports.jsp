<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.library.dao.dto.ReportSummaryDTO, java.util.Map, java.util.List" %>
<%
    ReportSummaryDTO summary = (ReportSummaryDTO) request.getAttribute("summary");
    List<Map<String, Object>> topBooks = (List<Map<String, Object>>) request.getAttribute("topBooks");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Inventory & Borrowing Analytics - Online Library System</title>
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
                <li><a href="<%= request.getContextPath() %>/librarian/transactions">Transactions</a></li>
                <li><a href="<%= request.getContextPath() %>/librarian/notifications">Notifications</a></li>
                <li><a href="<%= request.getContextPath() %>/librarian/reports" class="active">Reports</a></li>
                <li><a href="<%= request.getContextPath() %>/logout" class="btn btn-secondary btn-sm">Sign Out</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <div class="page-header">
            <div>
                <h1 class="page-title">Inventory Reports & Analytics</h1>
                <p class="page-subtitle">Real-time circulation metrics, genre distribution, and borrowing volume trends</p>
            </div>
            <a href="<%= request.getContextPath() %>/librarian/reports?format=json" target="_blank" class="btn btn-secondary btn-sm">Export JSON</a>
        </div>

        <% if (summary != null) { %>
        <div class="metrics-grid">
            <div class="metric-card">
                <div class="metric-label">Total Titles</div>
                <div class="metric-value primary"><%= summary.getTotalBooks() %></div>
            </div>
            <div class="metric-card">
                <div class="metric-label">Total Physical Copies</div>
                <div class="metric-value"><%= summary.getTotalCopies() %></div>
            </div>
            <div class="metric-card">
                <div class="metric-label">Available Copies</div>
                <div class="metric-value success"><%= summary.getAvailableCopies() %></div>
            </div>
            <div class="metric-card">
                <div class="metric-label">Currently Borrowed</div>
                <div class="metric-value warning"><%= summary.getBorrowedCopies() %></div>
            </div>
            <div class="metric-card">
                <div class="metric-label">Availability Ratio</div>
                <div class="metric-value"><%= String.format("%.1f%%", summary.getAvailabilityPercentage()) %></div>
            </div>
            <div class="metric-card">
                <div class="metric-label">Overdue Loans</div>
                <div class="metric-value danger"><%= summary.getOverdueLoans() %></div>
            </div>
        </div>
        <% } %>

        <div class="charts-grid">
            <div class="chart-card">
                <h3 class="card-title" style="margin-bottom: 16px;">Monthly Loan Volume Trends</h3>
                <div class="chart-container">
                    <canvas id="trendChart"></canvas>
                </div>
            </div>

            <div class="chart-card">
                <h3 class="card-title" style="margin-bottom: 16px;">Collection by Genre Distribution</h3>
                <div class="chart-container">
                    <canvas id="genreChart"></canvas>
                </div>
            </div>
        </div>

        <div class="charts-grid">
            <div class="chart-card">
                <h3 class="card-title" style="margin-bottom: 16px;">Overall Stock Availability Ratio</h3>
                <div class="chart-container">
                    <canvas id="availChart"></canvas>
                </div>
            </div>

            <div class="card" style="margin-bottom: 0;">
                <h3 class="card-title" style="margin-bottom: 16px;">Top Borrowed Titles</h3>
                <div class="table-responsive">
                    <table class="table">
                        <thead>
                            <tr>
                                <th>Rank</th>
                                <th>Title</th>
                                <th>Author</th>
                                <th>Times Borrowed</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% if (topBooks != null && !topBooks.isEmpty()) { 
                                int rank = 1;
                                for (Map<String, Object> tb : topBooks) {
                            %>
                            <tr>
                                <td><strong>#<%= rank++ %></strong></td>
                                <td><strong><%= tb.get("title") %></strong></td>
                                <td><%= tb.get("author") %></td>
                                <td><span class="badge badge-primary"><%= tb.get("borrowCount") %> borrows</span></td>
                            </tr>
                            <%  } 
                               } else { %>
                            <tr>
                                <td colspan="4" style="text-align:center; color: var(--text-muted); padding: 24px;">No circulation history recorded yet.</td>
                            </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </main>

    <script src="<%= request.getContextPath() %>/js/app.js"></script>
</body>
</html>
