package com.library.servlet;

import com.library.dao.dto.ReportSummaryDTO;
import com.library.service.ReportService;
import com.library.service.impl.ReportServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller servlet serving inventory metrics, borrowing trends, and analytics data.
 */
@WebServlet(name = "ReportServlet", urlPatterns = {"/librarian/reports"})
public class ReportServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ReportServlet.class.getName());

    private final ReportService reportService;

    public ReportServlet() {
        this.reportService = new ReportServiceImpl();
    }

    public ReportServlet(ReportService reportService) {
        this.reportService = reportService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String format = request.getParameter("format");
        String acceptHeader = request.getHeader("Accept");

        try {
            ReportSummaryDTO summary = reportService.getReportSummary();
            Map<String, Integer> metrics = reportService.getSummaryMetrics();
            Map<String, Integer> monthlyTrends = reportService.getMonthlyBorrowTrends();
            Map<String, Integer> genreDistribution = reportService.getGenreDistribution();
            Map<String, Integer> availabilityRatio = reportService.getAvailabilityRatio();
            List<Map<String, Object>> topBooks = reportService.getTopBorrowedBooks(5);

            if ("json".equalsIgnoreCase(format) || (acceptHeader != null && acceptHeader.contains("application/json"))) {
                response.setContentType("application/json;charset=UTF-8");
                PrintWriter out = response.getWriter();
                out.print(buildJsonReport(summary, monthlyTrends, genreDistribution, availabilityRatio));
                out.flush();
                return;
            }

            request.setAttribute("summary", summary);
            request.setAttribute("metrics", metrics);
            request.setAttribute("monthlyTrends", monthlyTrends);
            request.setAttribute("genreDistribution", genreDistribution);
            request.setAttribute("availabilityRatio", availabilityRatio);
            request.setAttribute("topBooks", topBooks);

            request.getRequestDispatcher("/WEB-INF/views/librarian/reports.jsp").forward(request, response);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error generating inventory reports", ex);
            request.setAttribute("errorMessage", "Error compiling report: " + ex.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/librarian/reports.jsp").forward(request, response);
        }
    }

    private String buildJsonReport(ReportSummaryDTO summary, Map<String, Integer> trends,
                                   Map<String, Integer> genres, Map<String, Integer> availability) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"totalBooks\":").append(summary.getTotalBooks()).append(",");
        sb.append("\"totalCopies\":").append(summary.getTotalCopies()).append(",");
        sb.append("\"availableCopies\":").append(summary.getAvailableCopies()).append(",");
        sb.append("\"borrowedCopies\":").append(summary.getBorrowedCopies()).append(",");
        sb.append("\"totalMembers\":").append(summary.getTotalMembers()).append(",");
        sb.append("\"overdueLoans\":").append(summary.getOverdueLoans()).append(",");

        // Trends
        sb.append("\"monthlyTrends\":{");
        int count = 0;
        for (Map.Entry<String, Integer> e : trends.entrySet()) {
            if (count++ > 0) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":").append(e.getValue());
        }
        sb.append("},");

        // Genres
        sb.append("\"genres\":{");
        count = 0;
        for (Map.Entry<String, Integer> e : genres.entrySet()) {
            if (count++ > 0) sb.append(",");
            sb.append("\"").append(e.getKey().replace("\"", "\\\"")).append("\":").append(e.getValue());
        }
        sb.append("},");

        // Availability
        sb.append("\"availability\":{");
        count = 0;
        for (Map.Entry<String, Integer> e : availability.entrySet()) {
            if (count++ > 0) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":").append(e.getValue());
        }
        sb.append("}}");

        return sb.toString();
    }
}
