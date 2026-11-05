-- =============================================================================
-- Online Library Management System - Initial Seed Data (DML)
-- Dual-Engine Compatible: Embedded H2 (MySQL Mode) and Standalone MySQL 8.0+
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. USERS SEED
-- Default Password for Librarians: admin123
-- Default Password for Members:    password123 or member123
-- BCrypt 10-round verified hashes:
--   admin123    -> $2a$10$UUW7uh/PF/CtSM5gW.yoDOltkiexNaANS1mof.GGyjM0Zk5Ghe6Um
--   password123 -> $2a$10$p0wl6HiexB4E5ftP2EmQ8eY00N10LkqGckNvZ86MLXwpfTdZc3WvW
-- -----------------------------------------------------------------------------
INSERT INTO users (membership_id, name, email, password_hash, role, email_notifications, due_date_alerts, new_arrival_alerts) VALUES
('LIB-001', 'Admin Librarian', 'admin@library.com', '$2a$10$UUW7uh/PF/CtSM5gW.yoDOltkiexNaANS1mof.GGyjM0Zk5Ghe6Um', 'LIBRARIAN', TRUE, TRUE, TRUE),
('LIB-002', 'Head Librarian', 'librarian@library.com', '$2a$10$UUW7uh/PF/CtSM5gW.yoDOltkiexNaANS1mof.GGyjM0Zk5Ghe6Um', 'LIBRARIAN', TRUE, TRUE, TRUE),
('LIB-003', 'Sarah Jenkins', 'sarah.jenkins@library.com', '$2a$10$UUW7uh/PF/CtSM5gW.yoDOltkiexNaANS1mof.GGyjM0Zk5Ghe6Um', 'LIBRARIAN', TRUE, TRUE, TRUE),
('MEM-1001', 'John Doe', 'john@example.com', '$2a$10$p0wl6HiexB4E5ftP2EmQ8eY00N10LkqGckNvZ86MLXwpfTdZc3WvW', 'MEMBER', TRUE, TRUE, TRUE),
('MEM-1002', 'Alice Smith', 'alice@example.com', '$2a$10$p0wl6HiexB4E5ftP2EmQ8eY00N10LkqGckNvZ86MLXwpfTdZc3WvW', 'MEMBER', TRUE, TRUE, TRUE),
('MEM-1003', 'Robert Johnson', 'robert@example.com', '$2a$10$p0wl6HiexB4E5ftP2EmQ8eY00N10LkqGckNvZ86MLXwpfTdZc3WvW', 'MEMBER', TRUE, FALSE, TRUE),
('MEM-1004', 'Emily Davis', 'emily@example.com', '$2a$10$p0wl6HiexB4E5ftP2EmQ8eY00N10LkqGckNvZ86MLXwpfTdZc3WvW', 'MEMBER', TRUE, TRUE, FALSE),
('MEM-1005', 'Michael Brown', 'michael@example.com', '$2a$10$p0wl6HiexB4E5ftP2EmQ8eY00N10LkqGckNvZ86MLXwpfTdZc3WvW', 'MEMBER', FALSE, FALSE, FALSE);

-- -----------------------------------------------------------------------------
-- 2. BOOKS SEED (19 Titles across 5 Genres)
-- -----------------------------------------------------------------------------
INSERT INTO books (isbn, title, author, genre, total_copies, available_copies) VALUES
-- Computer Science
('978-0134685991', 'Effective Java (3rd Edition)', 'Joshua Bloch', 'Computer Science', 5, 5),
('978-0132350884', 'Clean Code: A Handbook of Agile Software Craftsmanship', 'Robert C. Martin', 'Computer Science', 4, 3),
('978-0262033848', 'Introduction to Algorithms (3rd Edition)', 'Thomas H. Cormen, Charles E. Leiserson', 'Computer Science', 3, 3),
('978-0134494166', 'Design Patterns: Elements of Reusable Object-Oriented Software', 'Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides', 'Computer Science', 4, 4),
('978-1491950357', 'Designing Data-Intensive Applications', 'Martin Kleppmann', 'Computer Science', 5, 5),

-- Science & Mathematics
('978-0465024759', 'The Character of Physical Law', 'Richard P. Feynman', 'Science', 4, 4),
('978-0553380163', 'A Brief History of Time', 'Stephen Hawking', 'Science', 6, 6),
('978-0143110361', 'Cosmos', 'Carl Sagan', 'Science', 5, 5),
('978-0393356687', 'Astrophysics for People in a Hurry', 'Neil deGrasse Tyson', 'Science', 4, 4),

-- Fiction
('978-0451524935', '1984', 'George Orwell', 'Fiction', 6, 6),
('978-0061120084', 'To Kill a Mockingbird', 'Harper Lee', 'Fiction', 5, 5),
('978-0743273565', 'The Great Gatsby', 'F. Scott Fitzgerald', 'Fiction', 5, 5),
('978-0345391803', 'The Hitchhiker''s Guide to the Galaxy', 'Douglas Adams', 'Fiction', 4, 4),

-- History & Biography
('978-0062316097', 'Sapiens: A Brief History of Humankind', 'Yuval Noah Harari', 'History', 7, 6),
('978-0812979800', 'Guns, Germs, and Steel: The Fates of Human Societies', 'Jared Diamond', 'History', 4, 4),
('978-1501163401', 'Leonardo da Vinci', 'Walter Isaacson', 'History', 3, 3),

-- Literature & Philosophy
('978-0140449136', 'Crime and Punishment', 'Fyodor Dostoevsky', 'Literature', 4, 4),
('978-0140449273', 'Meditations', 'Marcus Aurelius', 'Literature', 6, 6),
('978-0142437230', 'The Republic', 'Plato', 'Literature', 3, 3);

-- -----------------------------------------------------------------------------
-- 3. TRANSACTIONS SEED
-- -----------------------------------------------------------------------------
INSERT INTO transactions (book_id, user_id, borrow_date, due_date, status, fine_amount)
VALUES (2, 4, CURRENT_TIMESTAMP, TIMESTAMPADD(DAY, 14, CURRENT_TIMESTAMP), 'BORROWED', 0.00);

INSERT INTO transactions (book_id, user_id, borrow_date, due_date, status, fine_amount)
VALUES (14, 5, TIMESTAMPADD(DAY, -20, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -6, CURRENT_TIMESTAMP), 'OVERDUE', 6.00);

-- -----------------------------------------------------------------------------
-- 4. NOTIFICATIONS SEED
-- -----------------------------------------------------------------------------
INSERT INTO notifications (user_id, title, message, type, is_read) VALUES
(4, 'Welcome to Online Library', 'Welcome John! Your library account has been activated. You may borrow up to 5 books at a time with a standard 14-day loan duration.', 'SYSTEM', FALSE),
(5, 'Overdue Book Notice', 'Your loan for "Sapiens: A Brief History of Humankind" is 6 days overdue. An accumulated fine of $6.00 has been recorded. Please return the book as soon as possible.', 'DUE_DATE', FALSE),
(1, 'Catalog Updated: Effective Java', 'New copies of Effective Java (3rd Edition) by Joshua Bloch have been added to the Computer Science section.', 'NEW_ARRIVAL', TRUE);

-- -----------------------------------------------------------------------------
-- 5. SEARCH_HISTORY SEED
-- -----------------------------------------------------------------------------
INSERT INTO search_history (user_id, query_text, search_type) VALUES
(4, 'Java', 'TITLE'),
(4, 'Computer Science', 'GENRE'),
(5, 'Harari', 'AUTHOR'),
(5, 'Fiction', 'GENRE');
