-- =============================================================================
-- Online Library Management System - Database Schema (DDL)
-- Dual-Engine Compatible: Embedded H2 (MySQL Mode) and Standalone MySQL 8.0+
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. USERS Table
-- Stores credentials, roles (LIBRARIAN, MEMBER), and notification preferences.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id INT NOT NULL AUTO_INCREMENT,
    membership_id VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    email_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    due_date_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    new_arrival_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_users_membership_id UNIQUE (membership_id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('LIBRARIAN', 'MEMBER')),
    INDEX idx_users_role (role),
    INDEX idx_users_email (email)
);

-- -----------------------------------------------------------------------------
-- 2. BOOKS Table
-- Catalog of books with stock inventory counters and uniqueness on ISBN.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS books (
    book_id INT NOT NULL AUTO_INCREMENT,
    isbn VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(150) NOT NULL,
    genre VARCHAR(50) NOT NULL,
    total_copies INT NOT NULL,
    available_copies INT NOT NULL,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (book_id),
    CONSTRAINT uk_books_isbn UNIQUE (isbn),
    CONSTRAINT chk_books_total_copies CHECK (total_copies >= 0),
    CONSTRAINT chk_books_available_copies CHECK (available_copies >= 0 AND available_copies <= total_copies),
    INDEX idx_books_title (title),
    INDEX idx_books_author (author),
    INDEX idx_books_genre (genre)
);

-- -----------------------------------------------------------------------------
-- 3. TRANSACTIONS Table
-- Borrowing/returning ledger, due date tracking, and overdue fine accumulation.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id INT NOT NULL AUTO_INCREMENT,
    book_id INT NOT NULL,
    user_id INT NOT NULL,
    borrow_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_date TIMESTAMP NOT NULL,
    return_date TIMESTAMP NULL DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'BORROWED',
    fine_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (transaction_id),
    CONSTRAINT fk_transactions_book FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE RESTRICT,
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    CONSTRAINT chk_transactions_status CHECK (status IN ('BORROWED', 'RETURNED', 'OVERDUE')),
    INDEX idx_transactions_user_status (user_id, status),
    INDEX idx_transactions_book_status (book_id, status),
    INDEX idx_transactions_due_status (due_date, status),
    INDEX idx_transactions_status (status)
);

-- -----------------------------------------------------------------------------
-- 4. NOTIFICATIONS Table
-- User notification inbox for system notices, due date reminders, and arrivals.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    notification_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(30) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notification_id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT chk_notifications_type CHECK (type IN ('DUE_DATE', 'NEW_ARRIVAL', 'SYSTEM', 'GENERAL')),
    INDEX idx_notifications_user_read (user_id, is_read),
    INDEX idx_notifications_created_at (created_at)
);

-- -----------------------------------------------------------------------------
-- 5. SEARCH_HISTORY Table
-- Audit trail of search queries performed by members for personalized analytics.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS search_history (
    search_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    query_text VARCHAR(255) NOT NULL,
    search_type VARCHAR(50) NOT NULL,
    searched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (search_id),
    CONSTRAINT fk_search_history_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT chk_search_history_type CHECK (search_type IN ('TITLE', 'AUTHOR', 'GENRE', 'ALL')),
    INDEX idx_search_history_user_date (user_id, searched_at)
);
