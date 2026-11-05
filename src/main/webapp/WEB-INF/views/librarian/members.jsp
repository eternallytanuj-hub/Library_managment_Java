<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.library.model.Member, java.util.List" %>
<%
    List<Member> members = (List<Member>) request.getAttribute("members");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Member Management - Online Library System</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/style.css">
</head>
<body>
    <header class="navbar">
        <a href="<%= request.getContextPath() %>/librarian/dashboard" class="navbar-brand">Library Staff Portal</a>
        <nav>
            <ul class="navbar-menu">
                <li><a href="<%= request.getContextPath() %>/librarian/dashboard">Dashboard</a></li>
                <li><a href="<%= request.getContextPath() %>/librarian/books">Books</a></li>
                <li><a href="<%= request.getContextPath() %>/librarian/members" class="active">Members</a></li>
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
                <h1 class="page-title">Member Management</h1>
                <p class="page-subtitle">Register and oversee library patron accounts and loan quotas</p>
            </div>
        </div>

        <% if ("true".equals(request.getParameter("created"))) { %>
            <div class="alert alert-success">Member account successfully created!</div>
        <% } else if ("true".equals(request.getParameter("updated"))) { %>
            <div class="alert alert-success">Member profile successfully updated!</div>
        <% } else if ("true".equals(request.getParameter("deleted"))) { %>
            <div class="alert alert-success">Member account successfully deactivated.</div>
        <% } else if ("duplicate_email".equals(request.getParameter("error"))) { %>
            <div class="alert alert-danger">Error: A user account with this email address already exists.</div>
        <% } else if ("active_loans".equals(request.getParameter("error"))) { %>
            <div class="alert alert-danger">Error: Cannot delete member with active book loans.</div>
        <% } else if (request.getParameter("error") != null) { %>
            <div class="alert alert-danger">Operation failed. Please verify member data and try again.</div>
        <% } %>

        <div class="card">
            <h2 class="card-title" style="margin-bottom: 16px;">Create New Member Account</h2>
            <form action="<%= request.getContextPath() %>/librarian/members" method="post">
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label">Full Name</label>
                        <input type="text" name="name" class="form-control" placeholder="e.g. Eleanor Vance" required>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Email Address</label>
                        <input type="email" name="email" class="form-control" placeholder="e.g. eleanor@example.com" required>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Initial Password</label>
                        <input type="password" name="password" class="form-control" placeholder="Minimum 6 chars" value="password123" required>
                    </div>
                </div>

                <button type="submit" class="btn btn-primary">+ Create Member Account</button>
            </form>
        </div>

        <div class="card">
            <div class="card-header">
                <h2 class="card-title">Registered Members (<%= members != null ? members.size() : 0 %>)</h2>
            </div>

            <div class="table-responsive">
                <table class="table">
                    <thead>
                        <tr>
                            <th>User ID</th>
                            <th>Membership ID</th>
                            <th>Name</th>
                            <th>Email</th>
                            <th>Active Loans</th>
                            <th>Status</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (members != null && !members.isEmpty()) { 
                            for (Member m : members) {
                        %>
                        <tr>
                            <td>#<%= m.getId() %></td>
                            <td><code><%= m.getMembershipId() %></code></td>
                            <td><strong><%= m.getName() %></strong></td>
                            <td><%= m.getEmail() %></td>
                            <td>
                                <span class="badge <%= m.getActiveBorrowCount() > 0 ? "badge-warning" : "badge-secondary" %>">
                                    <%= m.getActiveBorrowCount() %> / <%= m.getMaxBorrowLimit() %>
                                </span>
                            </td>
                            <td>
                                <span class="badge badge-success">Active</span>
                            </td>
                            <td>
                                <form action="<%= request.getContextPath() %>/librarian/members" method="post" onsubmit="return confirm('Deactivate member <%= m.getName() %>?');">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="userId" value="<%= m.getId() %>">
                                    <button type="submit" class="btn btn-danger btn-sm">Deactivate</button>
                                </form>
                            </td>
                        </tr>
                        <%  } 
                           } else { %>
                        <tr>
                            <td colspan="7" style="text-align:center; color: var(--text-muted); padding: 32px;">No members registered yet.</td>
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
