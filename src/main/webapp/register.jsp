<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Register - Online Library Management System</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/style.css">
</head>
<body>
    <div class="auth-wrapper">
        <div class="auth-card">
            <h1 class="auth-title">Join Online Library</h1>
            <p class="auth-subtitle">Create a new library member account</p>

            <% String error = request.getParameter("error"); %>
            <% if ("duplicate_email".equals(error)) { %>
                <div class="alert alert-danger">An account with this email address already exists.</div>
            <% } else if ("validation_failed".equals(error)) { %>
                <div class="alert alert-danger">Invalid registration details. Please verify all fields.</div>
            <% } else if (error != null) { %>
                <div class="alert alert-danger">Registration failed. Please try again.</div>
            <% } %>

            <form action="<%= request.getContextPath() %>/register" method="post">
                <div class="form-group">
                    <label class="form-label" for="name">Full Name</label>
                    <input type="text" id="name" name="name" class="form-control" placeholder="John Doe" required autofocus>
                </div>

                <div class="form-group">
                    <label class="form-label" for="email">Email Address</label>
                    <input type="email" id="email" name="email" class="form-control" placeholder="name@example.com" required>
                </div>

                <div class="form-group">
                    <label class="form-label" for="password">Password</label>
                    <input type="password" id="password" name="password" class="form-control" placeholder="Minimum 6 characters" minlength="6" required>
                </div>

                <button type="submit" class="btn btn-primary" style="width: 100%; margin-top: 8px;">Create Account</button>
            </form>

            <div class="auth-footer">
                Already registered? <a href="<%= request.getContextPath() %>/login">Sign In</a>
            </div>
        </div>
    </div>
    <script src="<%= request.getContextPath() %>/js/app.js"></script>
</body>
</html>
