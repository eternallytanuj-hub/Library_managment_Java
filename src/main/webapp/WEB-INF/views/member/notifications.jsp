<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.library.model.Notification, com.library.model.User, java.util.List" %>
<%
    User user = (User) request.getAttribute("user");
    if (user == null) {
        user = (User) session.getAttribute("loggedUser");
    }
    List<Notification> notifications = (List<Notification>) request.getAttribute("notifications");
    int unreadCount = request.getAttribute("unreadCount") != null ? (Integer) request.getAttribute("unreadCount") : 0;
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Notification Alerts - Online Library System</title>
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
                <li><a href="<%= request.getContextPath() %>/member/notifications" class="active">Notifications</a></li>
                <li><a href="<%= request.getContextPath() %>/member/profile">Profile</a></li>
                <li><a href="<%= request.getContextPath() %>/logout" class="btn btn-secondary btn-sm">Sign Out</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <div class="page-header">
            <div>
                <h1 class="page-title">Notification Alerts & Preferences</h1>
                <p class="page-subtitle">Personal due-date notifications, new arrivals, and alert delivery settings</p>
            </div>
            <% if (unreadCount > 0) { %>
            <form action="<%= request.getContextPath() %>/member/notifications" method="post" style="display:inline;">
                <input type="hidden" name="action" value="mark_all_read">
                <button type="submit" class="btn btn-secondary btn-sm">Mark All as Read (<%= unreadCount %>)</button>
            </form>
            <% } %>
        </div>

        <% if ("true".equals(request.getParameter("marked"))) { %>
            <div class="alert alert-success">Notification marked as read.</div>
        <% } else if ("true".equals(request.getParameter("preferences_saved"))) { %>
            <div class="alert alert-success">Notification preferences successfully updated!</div>
        <% } %>

        <div class="charts-grid">
            <div class="card" style="margin-bottom: 0;">
                <div class="card-header">
                    <h2 class="card-title">Alert Inbox (<%= notifications != null ? notifications.size() : 0 %>)</h2>
                </div>

                <% if (notifications != null && !notifications.isEmpty()) { 
                    for (Notification n : notifications) {
                %>
                <div class="notification-item <%= !n.isRead() ? "unread" : "" %>">
                    <div style="flex: 1;">
                        <div class="notification-title"><%= n.getTitle() %></div>
                        <div style="font-size: 0.9rem; color: var(--text-muted); margin-bottom: 6px;"><%= n.getMessage() %></div>
                        <div class="notification-time"><%= n.getCreatedAt() != null ? n.getCreatedAt().toString().substring(0, 16) : "" %></div>
                    </div>
                    <% if (!n.isRead()) { %>
                    <form action="<%= request.getContextPath() %>/member/notifications" method="post" style="margin-left: 12px;">
                        <input type="hidden" name="notificationId" value="<%= n.getId() %>">
                        <button type="submit" class="btn btn-secondary btn-sm">Mark Read</button>
                    </form>
                    <% } %>
                </div>
                <%  } 
                   } else { %>
                    <p style="color: var(--text-muted); text-align: center; padding: 40px 0;">No notifications in your inbox.</p>
                <% } %>
            </div>

            <div class="card" style="margin-bottom: 0; height: fit-content;">
                <h2 class="card-title" style="margin-bottom: 16px;">Notification Preferences</h2>
                <p style="font-size: 0.875rem; color: var(--text-muted); margin-bottom: 20px;">
                    Select which types of notifications you want to receive from the library.
                </p>

                <form action="<%= request.getContextPath() %>/member/notifications" method="post">
                    <input type="hidden" name="action" value="preferences">

                    <div class="form-group">
                        <label class="checkbox-label">
                            <input type="checkbox" name="dueDateAlerts" <%= (user != null && user.isDueDateAlerts()) ? "checked" : "" %>>
                            <span><strong>Due Date Reminders</strong><br><small style="color: var(--text-muted);">Receive alerts 48 hours before loan expiration</small></span>
                        </label>
                    </div>

                    <div class="form-group">
                        <label class="checkbox-label">
                            <input type="checkbox" name="newArrivalAlerts" <%= (user != null && user.isNewArrivalAlerts()) ? "checked" : "" %>>
                            <span><strong>New Arrivals</strong><br><small style="color: var(--text-muted);">Receive alerts when newly cataloged books arrive</small></span>
                        </label>
                    </div>

                    <div class="form-group">
                        <label class="checkbox-label">
                            <input type="checkbox" name="emailNotifications" <%= (user != null && user.isEmailNotifications()) ? "checked" : "" %>>
                            <span><strong>Email Notifications</strong><br><small style="color: var(--text-muted);">Mirror urgent notifications to your email address</small></span>
                        </label>
                    </div>

                    <button type="submit" class="btn btn-primary" style="width: 100%; margin-top: 12px;">Save Preferences</button>
                </form>
            </div>
        </div>
    </main>

    <script src="<%= request.getContextPath() %>/js/app.js"></script>
</body>
</html>
