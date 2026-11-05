package com.library.service;

import com.library.dao.BaseDaoTest;
import com.library.dao.BookDao;
import com.library.dao.NotificationDao;
import com.library.dao.TransactionDao;
import com.library.dao.UserDao;
import com.library.dao.impl.BookDaoJdbcImpl;
import com.library.dao.impl.NotificationDaoJdbcImpl;
import com.library.dao.impl.TransactionDaoJdbcImpl;
import com.library.dao.impl.UserDaoJdbcImpl;
import com.library.exception.BookNotFoundException;
import com.library.exception.BookUnavailableException;
import com.library.exception.LibraryException;
import com.library.exception.MemberNotFoundException;
import com.library.exception.UnauthorizedAccessException;
import com.library.model.Book;
import com.library.model.Librarian;
import com.library.model.Member;
import com.library.model.Notification;
import com.library.model.NotificationType;
import com.library.model.Transaction;
import com.library.model.TransactionStatus;
import com.library.model.User;
import com.library.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TransactionService Integration Tests")
class TransactionServiceTest extends BaseDaoTest {

    private TransactionDao transactionDao;
    private BookDao bookDao;
    private UserDao userDao;
    private NotificationDao notificationDao;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionDao = new TransactionDaoJdbcImpl();
        bookDao = new BookDaoJdbcImpl();
        userDao = new UserDaoJdbcImpl();
        notificationDao = new NotificationDaoJdbcImpl();
        transactionService = new TransactionServiceImpl(transactionDao, bookDao, userDao, notificationDao);
    }

    @Test
    @DisplayName("Should successfully borrow book, decrement stock, and record transaction")
    void testBorrowBook_Success() throws Exception {
        Member member = new Member("MEM-1001", "Alice", "alice@test.com", "hash");
        userDao.save(member);

        Book book = new Book("978-001", "Domain-Driven Design", "Eric Evans", "Tech", 3, 3);
        bookDao.save(book);

        Transaction tx = transactionService.borrowBook(member.getId(), book.getId());

        assertNotNull(tx.getId());
        assertEquals(member.getId(), tx.getUserId());
        assertEquals(book.getId(), tx.getBookId());
        assertEquals(TransactionStatus.BORROWED, tx.getStatus());

        // Verify stock decremented
        Book updatedBook = bookDao.findById(book.getId()).get();
        assertEquals(2, updatedBook.getAvailableCopies());
    }

    @Test
    @DisplayName("Should throw MemberNotFoundException on borrowBook when user does not exist")
    void testBorrowBook_MemberNotFound() throws Exception {
        Book book = bookDao.save(new Book("978-002", "Refactoring", "Martin Fowler", "Tech", 2));
        assertThrows(MemberNotFoundException.class, () -> transactionService.borrowBook(9999, book.getId()));
    }

    @Test
    @DisplayName("Should prevent Librarian from borrowing books")
    void testBorrowBook_LibrarianCannotBorrow() throws Exception {
        Librarian lib = new Librarian("LIB-001", "Librarian Dan", "dan@library.com", "hash");
        userDao.save(lib);

        Book book = bookDao.save(new Book("978-003", "Test Book", "Author", "Tech", 2));

        assertThrows(LibraryException.class, () -> transactionService.borrowBook(lib.getId(), book.getId()));
    }

    @Test
    @DisplayName("Should enforce maximum borrow limit (5 active loans) per member")
    void testBorrowBook_MaxLimitExceeded() throws Exception {
        Member member = new Member("MEM-1002", "Borrowing Champion", "champ@test.com", "hash");
        userDao.save(member);

        // Create 5 active loans
        for (int i = 1; i <= 5; i++) {
            Book b = bookDao.save(new Book("978-01" + i, "Book " + i, "Author", "Tech", 2));
            transactionService.borrowBook(member.getId(), b.getId());
        }

        // 6th borrow should fail
        Book b6 = bookDao.save(new Book("978-016", "Book 6", "Author", "Tech", 2));
        assertThrows(LibraryException.class, () -> transactionService.borrowBook(member.getId(), b6.getId()));
    }

    @Test
    @DisplayName("Should throw BookUnavailableException when available copies is 0")
    void testBorrowBook_BookUnavailable() throws Exception {
        Member member = new Member("MEM-1003", "David", "david@test.com", "hash");
        userDao.save(member);
        Book book = bookDao.save(new Book("978-004", "Exhausted Stock", "Author", "Tech", 1, 0));

        assertThrows(BookUnavailableException.class, () ->
                transactionService.borrowBook(member.getId(), book.getId()));
    }

    @Test
    @DisplayName("Should return book, increment stock, and close transaction")
    void testReturnBook_Success() throws Exception {
        Member member = new Member("MEM-1004", "Emma", "emma@test.com", "hash");
        userDao.save(member);
        Book book = bookDao.save(new Book("978-005", "Microservices", "Sam Newman", "Tech", 4, 3));
        // 1 copy borrowed

        Transaction tx = transactionDao.save(new Transaction(book.getId(), member.getId(),
                new Timestamp(System.currentTimeMillis()),
                new Timestamp(System.currentTimeMillis() + 864000000L)));

        Transaction returned = transactionService.returnBook(tx.getId(), member.getId(), false);

        assertEquals(TransactionStatus.RETURNED, returned.getStatus());
        assertNotNull(returned.getReturnDate());

        // Stock incremented from 3 to 4
        Book reloaded = bookDao.findById(book.getId()).get();
        assertEquals(4, reloaded.getAvailableCopies());
    }

    @Test
    @DisplayName("Should calculate overdue fines on late book return")
    void testReturnBook_OverdueFineCalculation() throws Exception {
        Member member = new Member("MEM-1005", "Frank", "frank@test.com", "hash");
        userDao.save(member);
        Book book = bookDao.save(new Book("978-006", "Late Book", "Author", "Tech", 2, 1));

        // Due 5 days ago
        long fiveDaysMs = 5L * 24 * 60 * 60 * 1000L;
        Timestamp borrowDate = new Timestamp(System.currentTimeMillis() - 19L * 24 * 60 * 60 * 1000L);
        Timestamp dueDate = new Timestamp(System.currentTimeMillis() - fiveDaysMs);

        Transaction tx = transactionDao.save(new Transaction(book.getId(), member.getId(), borrowDate, dueDate));

        Transaction returned = transactionService.returnBook(tx.getId(), member.getId(), false);
        assertEquals(TransactionStatus.RETURNED, returned.getStatus());
        assertTrue(returned.getFineAmount().compareTo(BigDecimal.ZERO) > 0, "Fine should be assessed for late return");
    }

    @Test
    @DisplayName("Should allow Librarian to process return for any member")
    void testReturnBook_LibrarianDeskOverride() throws Exception {
        Member member = new Member("MEM-1006", "Grace", "grace@test.com", "hash");
        userDao.save(member);
        Librarian lib = new Librarian("LIB-002", "Desk Lib", "desk@lib.com", "hash");
        userDao.save(lib);
        Book book = bookDao.save(new Book("978-007", "Desk Return", "Author", "Tech", 2, 1));

        Transaction tx = transactionDao.save(new Transaction(book.getId(), member.getId(),
                new Timestamp(System.currentTimeMillis()),
                new Timestamp(System.currentTimeMillis() + 86400000L)));

        // Librarian executes return with librarian override flag = true
        Transaction returned = transactionService.returnBook(tx.getId(), lib.getId(), true);
        assertEquals(TransactionStatus.RETURNED, returned.getStatus());
    }

    @Test
    @DisplayName("Should reject return attempt from non-librarian member who does not own transaction")
    void testReturnBook_UnauthorizedMemberAttempt() throws Exception {
        Member memberA = new Member("MEM-1007", "Member A", "a@test.com", "hash");
        userDao.save(memberA);
        Member memberB = new Member("MEM-1008", "Member B", "b@test.com", "hash");
        userDao.save(memberB);
        Book book = bookDao.save(new Book("978-008", "Shared Book", "Author", "Tech", 2, 1));

        Transaction tx = transactionDao.save(new Transaction(book.getId(), memberA.getId(),
                new Timestamp(System.currentTimeMillis()),
                new Timestamp(System.currentTimeMillis() + 86400000L)));

        assertThrows(UnauthorizedAccessException.class, () ->
                transactionService.returnBook(tx.getId(), memberB.getId(), false));
    }

    @Test
    @DisplayName("Should prevent double-return of already returned transaction")
    void testReturnBook_AlreadyReturned() throws Exception {
        Member member = new Member("MEM-1009", "Henry", "henry@test.com", "hash");
        userDao.save(member);
        Book book = bookDao.save(new Book("978-009", "Single Return", "Author", "Tech", 2, 1));

        Transaction tx = transactionDao.save(new Transaction(book.getId(), member.getId(),
                new Timestamp(System.currentTimeMillis()),
                new Timestamp(System.currentTimeMillis() + 86400000L)));

        transactionService.returnBook(tx.getId(), member.getId(), false);

        assertThrows(LibraryException.class, () ->
                transactionService.returnBook(tx.getId(), member.getId(), false));
    }

    @Test
    @DisplayName("Should sweep overdue transactions, transition status, and enqueue notifications")
    void testProcessOverdueTransactions() throws Exception {
        Member member = new Member("MEM-1010", "Overdue Borrower", "overdue@test.com", "hash");
        userDao.save(member);
        Book book = bookDao.save(new Book("978-010", "Sweep Book", "Author", "Tech", 2, 1));

        // Create transaction with due date in the past
        Timestamp borrowDate = new Timestamp(System.currentTimeMillis() - 20L * 24 * 60 * 60 * 1000L);
        Timestamp dueDate = new Timestamp(System.currentTimeMillis() - 5L * 24 * 60 * 60 * 1000L);
        Transaction tx = transactionDao.save(new Transaction(book.getId(), member.getId(), borrowDate, dueDate));

        int processed = transactionService.processOverdueTransactions();
        assertEquals(1, processed);

        Transaction updatedTx = transactionDao.findById(tx.getId()).get();
        assertEquals(TransactionStatus.OVERDUE, updatedTx.getStatus());

        List<Notification> notifs = notificationDao.findByUserId(member.getId());
        assertEquals(1, notifs.size());
        assertEquals(NotificationType.DUE_DATE, notifs.get(0).getType());
    }

    @Test
    @DisplayName("Should query active loans, history, and filtered transactions")
    void testQueriesAndFiltering() throws Exception {
        Member member = new Member("MEM-1011", "Active Querier", "query@test.com", "hash");
        userDao.save(member);
        Book b1 = bookDao.save(new Book("978-020", "Active 1", "Author", "Tech", 3));
        Book b2 = bookDao.save(new Book("978-021", "Active 2", "Author", "Tech", 3));

        transactionService.borrowBook(member.getId(), b1.getId());
        Transaction tx2 = transactionService.borrowBook(member.getId(), b2.getId());
        transactionService.returnBook(tx2.getId(), member.getId(), false);

        List<Transaction> active = transactionService.getActiveLoansByUserId(member.getId());
        assertEquals(1, active.size());

        List<Transaction> history = transactionService.getBorrowHistoryByUserId(member.getId());
        assertEquals(2, history.size());

        List<Transaction> returnedOnly = transactionService.getAllTransactions("RETURNED");
        assertEquals(1, returnedOnly.size());
    }
}
