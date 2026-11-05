package com.library.filter;

import com.library.model.User;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Security filter enforcing role-based access control for Librarian and Member routes.
 */
@WebFilter(filterName = "AuthenticationFilter", urlPatterns = {"/librarian/*", "/member/*"})
public class AuthenticationFilter implements Filter {

    public static final String LOGGED_USER_SESSION_KEY = "loggedUser";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestUri = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String path = requestUri.substring(contextPath.length());

        HttpSession session = httpRequest.getSession(false);
        User loggedUser = (session != null) ? (User) session.getAttribute(LOGGED_USER_SESSION_KEY) : null;

        // 1. Librarian Protected Routes
        if (path.startsWith("/librarian")) {
            if (loggedUser == null) {
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/login?error=session_expired");
                return;
            }
            if (!loggedUser.isLibrarian()) {
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/login?error=unauthorized");
                return;
            }
        }

        // 2. Member Protected Routes (Public catalog search allowed without session)
        if (path.startsWith("/member") && !path.startsWith("/member/search")) {
            if (loggedUser == null) {
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/login?error=session_expired");
                return;
            }
            if (!loggedUser.isMember()) {
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/login?error=unauthorized");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
