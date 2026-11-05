<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sign In - Online Library Management System</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/style.css">
</head>
<body>
    <div class="auth-wrapper">
        <div class="auth-card">
            <h1 class="auth-title">Welcome Back</h1>
            <p class="auth-subtitle">Sign in to your library account</p>

            <% String error = request.getParameter("error"); %>
            <% if ("invalid_credentials".equals(error)) { %>
                <div class="alert alert-danger">Invalid email or password. Please try again.</div>
            <% } else if ("session_expired".equals(error)) { %>
                <div class="alert alert-warning">Your session has expired. Please sign in to continue.</div>
            <% } else if ("unauthorized".equals(error)) { %>
                <div class="alert alert-danger">Access denied: Insufficient privileges for this section.</div>
            <% } else if (error != null) { %>
                <div class="alert alert-danger">Authentication failed. Please verify your credentials.</div>
            <% } %>

            <% if ("true".equals(request.getParameter("registered"))) { %>
                <div class="alert alert-success">Registration successful! Please sign in with your credentials.</div>
            <% } %>

            <form action="<%= request.getContextPath() %>/login" method="post">
                <div class="form-group">
                    <label class="form-label" for="email">Email Address</label>
                    <input type="email" id="email" name="email" class="form-control" placeholder="name@example.com" required autofocus>
                </div>

                <div class="form-group">
                    <label class="form-label" for="password">Password</label>
                    <input type="password" id="password" name="password" class="form-control" placeholder="••••••••" required>
                </div>

                <button type="submit" class="btn btn-primary" style="width: 100%; margin-top: 8px;">Sign In</button>
            </form>

            <div class="card" style="background-color: var(--bg-subtle); padding: 14px; margin-top: 24px; border-radius: var(--radius-sm);">
                <div style="font-size: 0.8rem; font-weight: 700; color: var(--text-muted); text-transform: uppercase; margin-bottom: 6px;">Evaluation Seed Accounts</div>
                <div style="font-size: 0.825rem; color: var(--text-main); margin-bottom: 4px;">
                    <strong>Librarian:</strong> admin@library.com &bull; admin123
                </div>
                <div style="font-size: 0.825rem; color: var(--text-main);">
                    <strong>Member:</strong> alice@example.com &bull; password123
                </div>
            </div>

            <div class="auth-footer">
                Don't have an account? <a href="<%= request.getContextPath() %>/register">Join Library</a>
            </div>
        </div>
    </div>
    <script src="<%= request.getContextPath() %>/js/app.js"></script>
</body>
</html>
