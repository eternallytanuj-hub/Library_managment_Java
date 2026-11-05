package com.library.servlet;

import com.library.model.Notification;
import com.library.model.NotificationType;
import com.library.model.User;
import com.library.service.MemberService;
import com.library.service.NotificationService;
import com.library.service.impl.MemberServiceImpl;
import com.library.service.impl.NotificationServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller servlet for broadcasting announcements and managing member alert inboxes/preferences.
 */
@WebServlet(name = "NotificationServlet", urlPatterns = {"/librarian/notifications", "/member/notifications"})
public class NotificationServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(NotificationServlet.class.getName());

    private final NotificationService notificationService;
    private final MemberService memberService;

    public NotificationServlet() {
        this.notificationService = new NotificationServiceImpl();
        this.memberService = new MemberServiceImpl();
    }

    public NotificationServlet(NotificationService notificationService, MemberService memberService) {
        this.notificationService = notificationService;
        this.memberService = memberService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        try {
            if ("/librarian/notifications".equals(path)) {
                request.getRequestDispatcher("/WEB-INF/views/librarian/notifications.jsp").forward(request, response);
            } else if ("/member/notifications".equals(path)) {
                HttpSession session = request.getSession(false);
                User loggedUser = (session != null) ? (User) session.getAttribute(AuthServlet.LOGGED_USER_SESSION_KEY) : null;
                if (loggedUser == null) {
                    response.sendRedirect(request.getContextPath() + "/login?error=session_expired");
                    return;
                }

                List<Notification> notifications = notificationService.getNotificationsForUser(loggedUser.getId());
                int unreadCount = notificationService.getUnreadNotificationCount(loggedUser.getId());
                User freshUser = memberService.getMemberById(loggedUser.getId()).orElse(null);

                request.setAttribute("notifications", notifications);
                request.setAttribute("unreadCount", unreadCount);
                request.setAttribute("user", (freshUser != null) ? freshUser : loggedUser);
                request.getRequestDispatcher("/WEB-INF/views/member/notifications.jsp").forward(request, response);
            } else {
                response.sendRedirect(request.getContextPath() + "/index.jsp");
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error in NotificationServlet doGet", ex);
            response.sendRedirect(request.getContextPath() + "/index.jsp?error=server_error");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        HttpSession session = request.getSession(false);
        User loggedUser = (session != null) ? (User) session.getAttribute(AuthServlet.LOGGED_USER_SESSION_KEY) : null;

        if (loggedUser == null) {
            response.sendRedirect(request.getContextPath() + "/login?error=session_expired");
            return;
        }

        try {
            if ("/librarian/notifications".equals(path)) {
                handleLibrarianBroadcast(request, response);
            } else if ("/member/notifications".equals(path)) {
                handleMemberNotificationAction(request, response, loggedUser);
            } else {
                response.sendRedirect(request.getContextPath() + "/index.jsp");
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error in NotificationServlet doPost", ex);
            response.sendRedirect(request.getContextPath() + path + "?error=operation_failed");
        }
    }

    private void handleLibrarianBroadcast(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String title = request.getParameter("title");
        String message = request.getParameter("message");
        String typeStr = request.getParameter("type");
        String targetUserIdStr = request.getParameter("userId");

        NotificationType type = NotificationType.SYSTEM;
        if (typeStr != null && !typeStr.isBlank()) {
            try {
                type = NotificationType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        try {
            if (targetUserIdStr != null && !targetUserIdStr.isBlank()) {
                int targetUserId = Integer.parseInt(targetUserIdStr);
                notificationService.sendNotification(targetUserId, title, message, type);
            } else {
                notificationService.broadcastNotification(title, message, type);
            }
            response.sendRedirect(request.getContextPath() + "/librarian/notifications?sent=true");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to broadcast notification", ex);
            response.sendRedirect(request.getContextPath() + "/librarian/notifications?error=send_failed");
        }
    }

    private void handleMemberNotificationAction(HttpServletRequest request, HttpServletResponse response, User loggedUser)
            throws IOException {
        String action = request.getParameter("action");
        String notifIdStr = request.getParameter("notificationId");

        // 1. Mark single notification as read
        if (notifIdStr != null && !notifIdStr.isBlank()) {
            try {
                int notifId = Integer.parseInt(notifIdStr);
                notificationService.markAsRead(notifId, loggedUser.getId());
                response.sendRedirect(request.getContextPath() + "/member/notifications?marked=true");
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to mark notification read", ex);
                response.sendRedirect(request.getContextPath() + "/member/notifications?error=action_failed");
            }
            return;
        }

        // 2. Mark all as read
        if ("mark_all_read".equalsIgnoreCase(action)) {
            try {
                notificationService.markAllAsRead(loggedUser.getId());
                response.sendRedirect(request.getContextPath() + "/member/notifications?marked=true");
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to mark all notifications read", ex);
                response.sendRedirect(request.getContextPath() + "/member/notifications?error=action_failed");
            }
            return;
        }

        // 3. Update alert preferences
        boolean emailNotifications = "on".equalsIgnoreCase(request.getParameter("emailNotifications"))
                || "true".equalsIgnoreCase(request.getParameter("emailNotifications"));
        boolean dueDateAlerts = "on".equalsIgnoreCase(request.getParameter("dueDateAlerts"))
                || "true".equalsIgnoreCase(request.getParameter("dueDateAlerts"));
        boolean newArrivalAlerts = "on".equalsIgnoreCase(request.getParameter("newArrivalAlerts"))
                || "true".equalsIgnoreCase(request.getParameter("newArrivalAlerts"));

        try {
            memberService.updateNotificationPreferences(loggedUser.getId(), emailNotifications, dueDateAlerts, newArrivalAlerts);
            loggedUser.setEmailNotifications(emailNotifications);
            loggedUser.setDueDateAlerts(dueDateAlerts);
            loggedUser.setNewArrivalAlerts(newArrivalAlerts);
            response.sendRedirect(request.getContextPath() + "/member/notifications?preferences_saved=true");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to update notification preferences", ex);
            response.sendRedirect(request.getContextPath() + "/member/notifications?error=preferences_failed");
        }
    }
}
