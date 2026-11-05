<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.library.model.User" %>
<%
    User user = (User) request.getAttribute("user");
    if (user == null) {
        user = (User) session.getAttribute("loggedUser");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Member Profile - Online Library System</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/style.css">
</head>
<body>
    <header class="navbar">
        <a href="<%= request.getContextPath() %>/member/dashboard" class="navbar-brand">Member Portal</a>
        <nav>
            <ul class="navbar-menu">
                <li><a href="<%= request.getContextPath() %>/member/dashboard">Dashboard</a></li>
                <li><a href="<%= request.getContextPath() %>/member/search">Catalog</a></li>
                <li><a href="<%= request.getContextPath() %>/member/history">Borrowing History</a></li>
                <li><a href="<%= request.getContextPath() %>/member/notifications">Notifications</a></li>
                <li><a href="<%= request.getContextPath() %>/member/profile" class="active">Profile</a></li>
                <li><a href="<%= request.getContextPath() %>/logout" class="btn btn-secondary btn-sm">Sign Out</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <div class="page-header">
            <div>
                <h1 class="page-title">Personal Profile</h1>
                <p class="page-subtitle">Update your personal account details and credentials</p>
            </div>
        </div>

        <% if ("true".equals(request.getParameter("updated"))) { %>
            <div class="alert alert-success">Profile information successfully updated!</div>
        <% } else if (request.getParameter("error") != null) { %>
            <div class="alert alert-danger">Update failed: <%= request.getParameter("error") %></div>
        <% } %>

        <div class="card" style="max-width: 640px;">
            <div class="form-group" style="margin-bottom: 24px;">
                <label class="form-label">Membership Identifier</label>
                <input type="text" class="form-control" value="<%= user != null ? user.getMembershipId() : "" %>" disabled style="background-color: var(--bg-hover); color: var(--text-muted); cursor: not-allowed;">
                <span style="font-size: 0.8rem; color: var(--text-muted);">Unique system-generated library identifier.</span>
            </div>

            <form action="<%= request.getContextPath() %>/member/profile" method="post">
                <div class="form-group">
                    <label class="form-label" for="name">Full Name</label>
                    <input type="text" id="name" name="name" class="form-control" value="<%= user != null ? user.getName() : "" %>" required>
                </div>

                <div class="form-group">
                    <label class="form-label" for="email">Email Address</label>
                    <input type="email" id="email" name="email" class="form-control" value="<%= user != null ? user.getEmail() : "" %>" required>
                </div>

                <div style="margin: 28px 0 16px; padding-top: 16px; border-top: 2px solid var(--bg-subtle);">
                    <h3 style="font-size: 1rem; margin-bottom: 4px;">Security & Password</h3>
                    <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 16px;">Leave blank if you do not wish to change your password.</p>
                </div>

                <div class="form-group">
                    <label class="form-label" for="currentPassword">Current Password</label>
                    <input type="password" id="currentPassword" name="currentPassword" class="form-control" placeholder="Required only if changing password">
                </div>

                <div class="form-group">
                    <label class="form-label" for="newPassword">New Password</label>
                    <input type="password" id="newPassword" name="newPassword" class="form-control" placeholder="Minimum 6 characters" minlength="6">
                </div>

                <button type="submit" class="btn btn-primary" style="margin-top: 8px;">Save Profile Changes</button>
            </form>
        </div>
    </main>

    <script src="<%= request.getContextPath() %>/js/app.js"></script>
</body>
</html>
