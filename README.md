# Online Library Management System

Web Application developed in Java for the GUVI Java Web Based Projects Evaluation. Provides role-based workflows for **Librarians** and **Members**, featuring catalog management, borrowing/returning transactions with ACID atomicity, real-time inventory tracking, analytics charts, and automated background overdue monitoring.

---

## Architecture Overview

The system strictly follows modern enterprise Java design patterns and a 4-tier architectural layout:

1. **Presentation Tier (`com.library.controller`, `com.library.filter`, `src/main/webapp`)**:
   - HTTP Servlets (`AuthServlet`, `BookServlet`, `MemberServlet`, `TransactionServlet`, `NotificationServlet`, `ReportServlet`, `ProfileServlet`).
   - Security Filters (`AuthenticationFilter`, `RoleFilter`, `LoggingFilter`) enforcing Role-Based Access Control (RBAC).
   - JSP views (`WEB-INF/views/`) adhering strictly to clean, modern, flat UI guidelines (**NO borders**, **NO shadows**, **NO sparkle icons**).
   - Embedded Tomcat 10.1 (`com.library.Main`) runner and Jetty Maven Plugin runner.

2. **Service Tier (`com.library.service`, `com.library.daemon`)**:
   - Business services (`BookService`, `MemberService`, `TransactionService`, `NotificationService`, `ReportService`).
   - Multithreaded background monitoring daemon (`ScheduledExecutorService`) for automated due date tracking and overdue alerts.
   - Fine-grained reentrant locks and synchronized stock checks to prevent race conditions during concurrent book borrowings.

3. **Data Access Tier (`com.library.dao`, `com.library.config`)**:
   - Generic DAO pattern (`GenericDao<T, ID>`) with typed entity sub-interfaces.
   - High-performance connection pooling via HikariCP.
   - Exclusive use of parameterized `PreparedStatement` to eliminate SQL injection vulnerabilities.
   - Explicit transactional boundaries (`conn.setAutoCommit(false)`, `conn.commit()`, `conn.rollback()`) guaranteeing ACID atomicity on book borrow and return operations.

4. **Persistence Tier (`src/main/resources/db/`)**:
   - Turnkey embedded file-based H2 database (`./data/librarydb`) in MySQL compatibility mode with zero external configuration required.
   - Production MySQL database support toggleable via `application.properties`.
   - Idempotent `schema.sql` and `data.sql` auto-bootstrapped on application launch.

---

## Core Domain Models & Hierarchy

- `BaseEntity`: Abstract root class providing identity (`id`), `createdAt`, and `updatedAt` audit timestamps.
- `User`: Abstract class encapsulating user identity, credentials, role, and notification preferences.
  - `Librarian`: Administrative user; cannot borrow books (`canBorrow() = false`).
  - `Member`: Library patron with active borrow quotas and tracking (`canBorrow() = true`).
- `Book`: Catalog entity with domain invariant protections (`0 <= availableCopies <= totalCopies`).
- `Transaction`: Loan transaction tracking borrow dates, due dates, return dates, loan status (`BORROWED`, `RETURNED`, `OVERDUE`), and overdue fines.
- `Notification`: System announcements and personal alerts with read receipt management.
- `SearchHistory`: Audit log of search queries and query categories.

---

## Custom Exception Hierarchy

All domain failure modes inherit from checked `LibraryException` to ensure compiler-enforced handling across service and controller boundaries:

```
java.lang.Exception
  └── com.library.exception.LibraryException
        ├── BookNotFoundException
        ├── BookUnavailableException
        ├── MemberNotFoundException
        ├── DuplicateEntityException
        ├── ValidationException
        ├── UnauthorizedAccessException
        └── DatabaseOperationException (wraps SQLException, preserves vendor error code and SQLState)
```

---

## Getting Started

### Prerequisites

- Java Development Kit: JDK 21+ (Java 21 bytecode target compiled cleanly on JDK 21 to JDK 25)
- Git 2.0+

### Quick Start with Maven Wrapper

The project includes the self-contained Maven Wrapper (`./mvnw`). No pre-installed Maven is required.

1. **Clone the repository**:
   ```bash
   git clone https://github.com/eternallytanuj-hub/Library_managment_Java.git
   cd Library_managment_Java
   ```

2. **Run tests**:
   ```bash
   ./mvnw clean test
   ```

3. **Compile and package**:
   ```bash
   ./mvnw clean package
   ```

4. **Run the embedded application**:
   ```bash
   ./mvnw compile exec:java
   ```
   Or run with Jetty:
   ```bash
   ./mvnw jetty:run
   ```

5. **Access the web application**:
   Open your browser and navigate to `http://localhost:8080/`.

---

## Default User Accounts

| Role | Email | Password | Membership ID | Description |
|------|-------|----------|---------------|-------------|
| Librarian | `librarian@library.com` | `admin123` | `LIB-001` | System Administrator / Head Librarian |
| Member | `alice@example.com` | `member123` | `MEM-1001` | Active Library Patron |
| Member | `bob@example.com` | `member123` | `MEM-1002` | Active Library Patron |

---

## Repository

- **GitHub Remote**: `https://github.com/eternallytanuj-hub/Library_managment_Java.git`
- **License**: MIT
