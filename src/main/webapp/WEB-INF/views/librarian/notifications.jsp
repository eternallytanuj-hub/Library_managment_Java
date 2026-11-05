<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Notification Management - Online Library System</title>
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
                <li><a href="<%= request.getContextPath() %>/librarian/notifications" class="active">Notifications</a></li>
                <li><a href="<%= request.getContextPath() %>/librarian/reports">Reports</a></li>
                <li><a href="<%= request.getContextPath() %>/logout" class="btn btn-secondary btn-sm">Sign Out</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <div class="page-header">
            <div>
                <h1 class="page-title">Notification Dispatcher</h1>
                <p class="page-subtitle">Send targeted due-date reminders, arrival notices, and system bulletins</p>
            </div>
        </div>

        <% if ("true".equals(request.getParameter("sent"))) { %>
            <div class="alert alert-success">Notification broadcast dispatched successfully!</div>
        <% } else if (request.getParameter("error") != null) { %>
            <div class="alert alert-danger">Failed to send notification. Please check all fields and try again.</div>
        <% } %>

        <div class="card" style="max-width: 720px;">
            <h2 class="card-title" style="margin-bottom: 20px;">Compose Library Announcement</h2>
            <form action="<%= request.getContextPath() %>/librarian/notifications" method="post">
                <div class="form-group">
                    <label class="form-label">Alert Category</label>
                    <select name="type" class="form-control" required>
                        <option value="NEW_ARRIVAL">New Book Arrival Notice</option>
                        <option value="DUE_DATE">Due Date Reminder</option>
                        <option value="SYSTEM">General System Announcement</option>
                    </select>
                </div>

                <div class="form-group">
                    <label class="form-label">Subject Title</label>
                    <input type="text" name="title" class="form-control" placeholder="e.g. New Arrivals: 2026 Computing Collection" required>
                </div>

                <div class="form-group">
                    <label class="form-label">Recipient Mode</label>
                    <input type="text" name="userId" class="form-control" placeholder="Leave blank to broadcast to ALL library members">
                </div>

                <div class="form-group">
                    <label class="form-label">Message Content</label>
                    <textarea name="message" class="form-control" placeholder="Detailed message for library patrons..." required></textarea>
                </div>

                <button type="submit" class="btn btn-primary">Dispatch Notification</button>
            </form>
        </div>
    </main>

    <script src="<%= request.getContextPath() %>/js/app.js"></script>
</body>
</html>
