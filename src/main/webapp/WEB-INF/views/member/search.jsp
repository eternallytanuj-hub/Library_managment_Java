<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.library.model.Book, com.library.model.User, java.util.List" %>
<%
    User loggedUser = (User) session.getAttribute("loggedUser");
    List<Book> books = (List<Book>) request.getAttribute("books");
    List<String> genres = (List<String>) request.getAttribute("genres");
    String query = (String) request.getAttribute("query");
    String searchType = (String) request.getAttribute("searchType");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Book Catalog Search - Online Library System</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/style.css">
</head>
<body>
    <header class="navbar">
        <a href="<%= request.getContextPath() %>/member/dashboard" class="navbar-brand">Member Portal</a>
        <nav>
            <ul class="navbar-menu">
                <li><a href="<%= request.getContextPath() %>/member/dashboard">Dashboard</a></li>
                <li><a href="<%= request.getContextPath() %>/member/search" class="active">Catalog</a></li>
                <li><a href="<%= request.getContextPath() %>/member/history">Borrowing History</a></li>
                <li><a href="<%= request.getContextPath() %>/member/notifications">Notifications</a></li>
                <li><a href="<%= request.getContextPath() %>/member/profile">Profile</a></li>
                <li><a href="<%= request.getContextPath() %>/logout" class="btn btn-secondary btn-sm">Sign Out</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <div class="page-header">
            <div>
                <h1 class="page-title">Book Catalog Search</h1>
                <p class="page-subtitle">Discover titles, check live availability, and borrow books online</p>
            </div>
        </div>

        <% if ("book_unavailable".equals(request.getParameter("error"))) { %>
            <div class="alert alert-danger">Sorry, this book currently has 0 available copies in stock.</div>
        <% } else if (request.getParameter("error") != null) { %>
            <div class="alert alert-danger">Unable to process borrow request: <%= request.getParameter("error") %></div>
        <% } %>

        <div class="card">
            <form action="<%= request.getContextPath() %>/member/search" method="get">
                <div class="form-row" style="align-items: flex-end;">
                    <div class="form-group" style="flex: 2; margin-bottom: 0;">
                        <label class="form-label">Keywords</label>
                        <input type="text" name="q" class="form-control" placeholder="Search by title, author, or keyword..." value="<%= query != null ? query : "" %>" autofocus>
                    </div>

                    <div class="form-group" style="flex: 1; margin-bottom: 0;">
                        <label class="form-label">Search Criteria</label>
                        <select name="type" class="form-control">
                            <option value="TITLE" <%= "TITLE".equalsIgnoreCase(searchType) ? "selected" : "" %>>Book Title</option>
                            <option value="AUTHOR" <%= "AUTHOR".equalsIgnoreCase(searchType) ? "selected" : "" %>>Author</option>
                            <option value="GENRE" <%= "GENRE".equalsIgnoreCase(searchType) ? "selected" : "" %>>Genre</option>
                        </select>
                    </div>

                    <div class="form-group" style="margin-bottom: 0;">
                        <button type="submit" class="btn btn-primary" style="height: 44px; padding: 0 24px;">Search</button>
                    </div>
                </div>
            </form>
        </div>

        <div class="card">
            <div class="card-header">
                <h2 class="card-title">Catalog Titles (<%= books != null ? books.size() : 0 %> Found)</h2>
            </div>

            <div class="table-responsive">
                <table class="table">
                    <thead>
                        <tr>
                            <th>Book Title</th>
                            <th>Author</th>
                            <th>Genre</th>
                            <th>ISBN</th>
                            <th>Stock</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (books != null && !books.isEmpty()) { 
                            for (Book b : books) {
                        %>
                        <tr>
                            <td><strong><%= b.getTitle() %></strong></td>
                            <td><%= b.getAuthor() %></td>
                            <td><span class="badge badge-secondary"><%= b.getGenre() %></span></td>
                            <td><code><%= b.getIsbn() %></code></td>
                            <td>
                                <span class="stock"><%= b.getAvailableCopies() %>/<%= b.getTotalCopies() %></span>
                            </td>
                            <td>
                                <% if (b.getAvailableCopies() > 0) { %>
                                <form action="<%= request.getContextPath() %>/member/borrow" method="post" style="display:inline;">
                                    <input type="hidden" name="bookId" value="<%= b.getId() %>">
                                    <button type="submit" class="btn btn-primary btn-sm">Borrow Book</button>
                                </form>
                                <% } else { %>
                                    <span class="badge badge-danger">Out of Stock</span>
                                <% } %>
                            </td>
                        </tr>
                        <%  } 
                           } else { %>
                        <tr>
                            <td colspan="6" style="text-align:center; color: var(--text-muted); padding: 32px;">No books matched your search query. Try another term or clear the filter.</td>
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
