package com.library.servlet;

import com.library.exception.AuthenticationException;
import com.library.exception.DuplicateEntityException;
import com.library.exception.ValidationException;
import com.library.model.User;
import com.library.service.AuthService;
import com.library.service.impl.AuthServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller servlet managing user authentication, registration, and logout flows.
 */
@WebServlet(name = "AuthServlet", urlPatterns = {"/login", "/register", "/logout"})
public class AuthServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AuthServlet.class.getName());
    public static final String LOGGED_USER_SESSION_KEY = "loggedUser";

    private final AuthService authService;

    public AuthServlet() {
        this.authService = new AuthServiceImpl();
    }

    public AuthServlet(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        switch (path) {
            case "/logout" -> handleLogout(request, response);
            case "/register" -> request.getRequestDispatcher("/register.jsp").forward(request, response);
            case "/login" -> {
                // If already logged in, redirect to respective dashboard
                HttpSession session = request.getSession(false);
                if (session != null && session.getAttribute(LOGGED_USER_SESSION_KEY) != null) {
                    User user = (User) session.getAttribute(LOGGED_USER_SESSION_KEY);
                    if (user.isLibrarian()) {
                        response.sendRedirect(request.getContextPath() + "/librarian/dashboard");
                        return;
                    } else {
                        response.sendRedirect(request.getContextPath() + "/member/dashboard");
                        return;
                    }
                }
                request.getRequestDispatcher("/login.jsp").forward(request, response);
            }
            default -> response.sendRedirect(request.getContextPath() + "/index.jsp");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        switch (path) {
            case "/login" -> handleLogin(request, response);
            case "/register" -> handleRegister(request, response);
            case "/logout" -> handleLogout(request, response);
            default -> response.sendRedirect(request.getContextPath() + "/index.jsp");
        }
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try {
            User authenticatedUser = authService.login(email, password);
            HttpSession session = request.getSession(true);
            session.setAttribute(LOGGED_USER_SESSION_KEY, authenticatedUser);

            if (authenticatedUser.isLibrarian()) {
                response.sendRedirect(request.getContextPath() + "/librarian/dashboard");
            } else {
                response.sendRedirect(request.getContextPath() + "/member/dashboard");
            }
        } catch (AuthenticationException | ValidationException ex) {
            LOGGER.log(Level.WARNING, "Login failed for email: " + email + " - " + ex.getMessage());
            response.sendRedirect(request.getContextPath() + "/login?error=invalid_credentials");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unexpected error during login for: " + email, ex);
            response.sendRedirect(request.getContextPath() + "/login?error=server_error");
        }
    }

    private void handleRegister(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try {
            authService.register(name, email, password);
            response.sendRedirect(request.getContextPath() + "/login?registered=true");
        } catch (DuplicateEntityException ex) {
            LOGGER.log(Level.WARNING, "Registration duplicate email: " + email);
            response.sendRedirect(request.getContextPath() + "/register?error=duplicate_email");
        } catch (ValidationException ex) {
            LOGGER.log(Level.WARNING, "Registration validation error: " + ex.getMessage());
            response.sendRedirect(request.getContextPath() + "/register?error=validation_failed");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unexpected error during registration for: " + email, ex);
            response.sendRedirect(request.getContextPath() + "/register?error=server_error");
        }
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(LOGGED_USER_SESSION_KEY);
            session.invalidate();
        }
        response.sendRedirect(request.getContextPath() + "/index.jsp");
    }
}
