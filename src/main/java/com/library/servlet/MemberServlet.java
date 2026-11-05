package com.library.servlet;

import com.library.exception.DuplicateEntityException;
import com.library.exception.LibraryException;
import com.library.exception.ValidationException;
import com.library.model.Member;
import com.library.service.MemberService;
import com.library.service.impl.MemberServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller servlet for Librarian administration of library members.
 */
@WebServlet(name = "MemberServlet", urlPatterns = {"/librarian/members"})
public class MemberServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(MemberServlet.class.getName());

    private final MemberService memberService;

    public MemberServlet() {
        this.memberService = new MemberServiceImpl();
    }

    public MemberServlet(MemberService memberService) {
        this.memberService = memberService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            List<Member> members = memberService.getAllMembers();
            request.setAttribute("members", members);
            request.getRequestDispatcher("/WEB-INF/views/librarian/members.jsp").forward(request, response);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error loading members", ex);
            request.setAttribute("errorMessage", "Unable to load members: " + ex.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/librarian/members.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        String deleteParam = request.getParameter("delete");
        String userIdStr = request.getParameter("userId");
        if (userIdStr == null || userIdStr.isBlank()) {
            userIdStr = request.getParameter("memberId");
        }

        // 1. Delete Member
        if ("delete".equalsIgnoreCase(action) || "true".equalsIgnoreCase(deleteParam)) {
            try {
                int userId = Integer.parseInt(userIdStr);
                memberService.deleteMember(userId);
                response.sendRedirect(request.getContextPath() + "/librarian/members?deleted=true");
            } catch (LibraryException ex) {
                LOGGER.log(Level.WARNING, "Cannot delete member: " + ex.getMessage());
                response.sendRedirect(request.getContextPath() + "/librarian/members?error=active_loans");
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error deleting member", ex);
                response.sendRedirect(request.getContextPath() + "/librarian/members?error=delete_failed");
            }
            return;
        }

        // 2. Update Member Profile
        if ("update".equalsIgnoreCase(action) && userIdStr != null && !userIdStr.isBlank()) {
            try {
                int userId = Integer.parseInt(userIdStr);
                String name = request.getParameter("name");
                String email = request.getParameter("email");
                String currentPassword = request.getParameter("currentPassword");
                String newPassword = request.getParameter("newPassword");

                memberService.updateProfile(userId, name, email, currentPassword, newPassword);
                response.sendRedirect(request.getContextPath() + "/librarian/members?updated=true");
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error updating member", ex);
                response.sendRedirect(request.getContextPath() + "/librarian/members?error=update_failed");
            }
            return;
        }

        // 3. Create Member
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        if (password == null || password.isBlank()) {
            password = "password123";
        }

        try {
            memberService.registerMember(name, email, password);
            response.sendRedirect(request.getContextPath() + "/librarian/members?created=true");
        } catch (DuplicateEntityException ex) {
            response.sendRedirect(request.getContextPath() + "/librarian/members?error=duplicate_email");
        } catch (ValidationException ex) {
            response.sendRedirect(request.getContextPath() + "/librarian/members?error=validation_failed");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error creating member", ex);
            response.sendRedirect(request.getContextPath() + "/librarian/members?error=server_error");
        }
    }
}
