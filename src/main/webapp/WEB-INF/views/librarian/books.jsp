<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.library.model.Book, java.util.List" %>
<%
    List<Book> books = (List<Book>) request.getAttribute("books");
    List<String> genres = (List<String>) request.getAttribute("genres");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Book Management - Online Library System</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/style.css">
</head>
<body>
    <header class="navbar">
        <a href="<%= request.getContextPath() %>/librarian/dashboard" class="navbar-brand">Library Staff Portal</a>
        <nav>
            <ul class="navbar-menu">
                <li><a href="<%= request.getContextPath() %>/librarian/dashboard">Dashboard</a></li>
                <li><a href="<%= request.getContextPath() %>/librarian/books" class="active">Books</a></li>
                <li><a href="<%= request.getContextPath() %>/librarian/members">Members</a></li>
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
                <h1 class="page-title">Book Management</h1>
                <p class="page-subtitle">Add, edit, and curate titles across the library catalog</p>
            </div>
        </div>

        <% if ("true".equals(request.getParameter("added"))) { %>
            <div class="alert alert-success">Book successfully added to the catalog!</div>
        <% } else if ("true".equals(request.getParameter("updated"))) { %>
            <div class="alert alert-success">Book details successfully updated!</div>
        <% } else if ("true".equals(request.getParameter("deleted"))) { %>
            <div class="alert alert-success">Book successfully removed from the catalog.</div>
        <% } else if ("duplicate_isbn".equals(request.getParameter("error"))) { %>
            <div class="alert alert-danger">Error: A book with this ISBN already exists.</div>
        <% } else if ("active_loans".equals(request.getParameter("error"))) { %>
            <div class="alert alert-danger">Error: Cannot delete book with active borrowing transactions.</div>
        <% } else if (request.getParameter("error") != null) { %>
            <div class="alert alert-danger">Operation failed. Please check inputs and try again.</div>
        <% } %>

        <div class="card">
            <h2 class="card-title" style="margin-bottom: 16px;">Add New Book</h2>
            <form action="<%= request.getContextPath() %>/librarian/books" method="post">
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label">Book Title</label>
                        <input type="text" name="title" class="form-control" placeholder="e.g. Clean Architecture" required>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Author</label>
                        <input type="text" name="author" class="form-control" placeholder="e.g. Robert C. Martin" required>
                    </div>
                </div>

                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label">ISBN</label>
                        <input type="text" name="isbn" class="form-control" placeholder="e.g. 978-0134494166" required>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Genre</label>
                        <input type="text" name="genre" class="form-control" placeholder="e.g. Computer Science" required>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Total Copies</label>
                        <input type="number" name="totalCopies" class="form-control" value="5" min="1" required>
                    </div>
                </div>

                <button type="submit" class="btn btn-primary">+ Add Book to Catalog</button>
            </form>
        </div>

        <div class="card">
            <div class="card-header">
                <h2 class="card-title">Catalog Inventory (<%= books != null ? books.size() : 0 %> Titles)</h2>
            </div>

            <div class="table-responsive">
                <table class="table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>ISBN</th>
                            <th>Title</th>
                            <th>Author</th>
                            <th>Genre</th>
                            <th>Stock</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (books != null && !books.isEmpty()) { 
                            for (Book b : books) {
                        %>
                        <tr>
                            <td>#<%= b.getId() %></td>
                            <td><code><%= b.getIsbn() %></code></td>
                            <td><strong><%= b.getTitle() %></strong></td>
                            <td><%= b.getAuthor() %></td>
                            <td><span class="badge badge-secondary"><%= b.getGenre() %></span></td>
                            <td>
                                <span class="stock"><%= b.getAvailableCopies() %>/<%= b.getTotalCopies() %></span>
                            </td>
                            <td>
                                <div style="display: flex; gap: 8px;">
                                    <form action="<%= request.getContextPath() %>/librarian/books" method="post" onsubmit="return confirm('Are you sure you want to remove this book?');">
                                        <input type="hidden" name="action" value="delete">
                                        <input type="hidden" name="bookId" value="<%= b.getId() %>">
                                        <button type="submit" class="btn btn-danger btn-sm">Delete</button>
                                    </form>
                                </div>
                            </td>
                        </tr>
                        <%  } 
                           } else { %>
                        <tr>
                            <td colspan="7" style="text-align:center; color: var(--text-muted); padding: 32px;">No books registered in the catalog.</td>
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
