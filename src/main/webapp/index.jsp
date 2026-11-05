<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.library.model.User" %>
<%
    User loggedUser = (User) session.getAttribute("loggedUser");
    if (loggedUser != null) {
        if (loggedUser.isLibrarian()) {
            response.sendRedirect(request.getContextPath() + "/librarian/dashboard");
            return;
        } else {
            response.sendRedirect(request.getContextPath() + "/member/dashboard");
            return;
        }
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Online Library Management System</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/style.css">
</head>
<body>
    <header class="navbar">
        <a href="<%= request.getContextPath() %>/" class="navbar-brand">Library System</a>
        <nav>
            <ul class="navbar-menu">
                <li><a href="<%= request.getContextPath() %>/member/search">Catalog Search</a></li>
                <li><a href="<%= request.getContextPath() %>/login">Sign In</a></li>
                <li><a href="<%= request.getContextPath() %>/register" class="btn btn-primary btn-sm">Join Library</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <div class="card" style="padding: 60px 40px; text-align: center; margin-top: 40px;">
            <span class="badge badge-primary" style="margin-bottom: 16px;">Enterprise Library Portal</span>
            <h1 style="font-size: 2.5rem; margin-bottom: 16px;">Online Library Management System</h1>
            <p style="font-size: 1.15rem; color: var(--text-muted); max-width: 680px; margin: 0 auto 32px;">
                Comprehensive library management facilitating staff catalog curation, member lifecycle operations,
                and real-time borrowing transactions with automated due-date monitoring.
            </p>
            <div style="display: flex; gap: 16px; justify-content: center; flex-wrap: wrap;">
                <a href="<%= request.getContextPath() %>/login" class="btn btn-primary" style="padding: 12px 28px; font-size: 1rem;">Librarian / Member Login</a>
                <a href="<%= request.getContextPath() %>/member/search" class="btn btn-secondary" style="padding: 12px 28px; font-size: 1rem;">Browse Catalog</a>
            </div>
        </div>

        <div class="metrics-grid" style="margin-top: 32px;">
            <div class="metric-card">
                <div class="metric-label">Librarian Suite</div>
                <p style="font-size: 0.9rem; color: var(--text-muted); margin: 0;">Catalog CRUD, member directory, loan processing, and analytics reports.</p>
            </div>
            <div class="metric-card">
                <div class="metric-label">Member Portal</div>
                <p style="font-size: 0.9rem; color: var(--text-muted); margin: 0;">Multi-criteria book discovery, 1-click borrowing, loan history, and alert inbox.</p>
            </div>
            <div class="metric-card">
                <div class="metric-label">ACID Transactions</div>
                <p style="font-size: 0.9rem; color: var(--text-muted); margin: 0;">Guaranteed inventory atomicity with fine-grained concurrency locking.</p>
            </div>
            <div class="metric-card">
                <div class="metric-label">Automated Sweeps</div>
                <p style="font-size: 0.9rem; color: var(--text-muted); margin: 0;">Scheduled background daemon for due date tracking and automated fine logging.</p>
            </div>
        </div>
    </main>

    <script src="<%= request.getContextPath() %>/js/app.js"></script>
</body>
</html>
