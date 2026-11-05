package com.library.servlet;

import com.library.exception.LibraryException;
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
 * Controller servlet for member self-service profile management and credential updates.
 */
@WebServlet(name = "ProfileServlet", urlPatterns = {"/member/profile"})
public class ProfileServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ProfileServlet.class.getName());

    private final AuthService authService;

    public ProfileServlet() {
        this.authService = new AuthServiceImpl();
    }

    public ProfileServlet(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User loggedUser = (session != null) ? (User) session.getAttribute(AuthServlet.LOGGED_USER_SESSION_KEY) : null;

        if (loggedUser == null) {
            response.sendRedirect(request.getContextPath() + "/login?error=session_expired");
            return;
        }

        try {
            User latestUser = authService.getUserById(loggedUser.getId()).orElse(loggedUser);
            request.setAttribute("user", latestUser);
            request.getRequestDispatcher("/WEB-INF/views/member/profile.jsp").forward(request, response);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error fetching profile", ex);
            request.setAttribute("user", loggedUser);
            request.getRequestDispatcher("/WEB-INF/views/member/profile.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User loggedUser = (session != null) ? (User) session.getAttribute(AuthServlet.LOGGED_USER_SESSION_KEY) : null;

        if (loggedUser == null) {
            response.sendRedirect(request.getContextPath() + "/login?error=session_expired");
            return;
        }

        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");

        try {
            User updatedUser = authService.updateProfile(loggedUser.getId(), name, email, currentPassword, newPassword);
            session.setAttribute(AuthServlet.LOGGED_USER_SESSION_KEY, updatedUser);
            response.sendRedirect(request.getContextPath() + "/member/profile?updated=true");
        } catch (LibraryException ex) {
            LOGGER.log(Level.WARNING, "Failed to update profile: " + ex.getMessage());
            response.sendRedirect(request.getContextPath() + "/member/profile?error=" + ex.getMessage());
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating profile", ex);
            response.sendRedirect(request.getContextPath() + "/member/profile?error=server_error");
        }
    }
}
