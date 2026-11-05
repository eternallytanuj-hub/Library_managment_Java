package com.library.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Concrete domain model representing a book loan transaction and its financial state.
 */
public class Transaction extends BaseEntity {
    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_LOAN_DAYS = 14;
    public static final BigDecimal DEFAULT_DAILY_FINE = new BigDecimal("1.00");

    private Integer bookId;
    private Integer userId;
    private Timestamp borrowDate;
    private Timestamp dueDate;
    private Timestamp returnDate;
    private TransactionStatus status;
    private BigDecimal fineAmount;

    // Optional navigational references for presentation layer
    private Book book;
    private User user;

    public Transaction() {
        super();
        this.status = TransactionStatus.BORROWED;
        this.fineAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public Transaction(Integer bookId, Integer userId, Timestamp borrowDate, Timestamp dueDate) {
        super();
        this.bookId = bookId;
        this.userId = userId;
        this.borrowDate = (borrowDate != null) ? borrowDate : new Timestamp(System.currentTimeMillis());
        this.dueDate = (dueDate != null) ? dueDate : Timestamp.valueOf(this.borrowDate.toLocalDateTime().plusDays(DEFAULT_LOAN_DAYS));
        this.returnDate = null;
        this.status = TransactionStatus.BORROWED;
        this.fineAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public Transaction(Integer bookId, Integer userId, LocalDateTime borrowDate, LocalDateTime dueDate) {
        super();
        this.bookId = bookId;
        this.userId = userId;
        this.borrowDate = (borrowDate != null) ? Timestamp.valueOf(borrowDate) : new Timestamp(System.currentTimeMillis());
        this.dueDate = (dueDate != null) ? Timestamp.valueOf(dueDate) : Timestamp.valueOf(this.borrowDate.toLocalDateTime().plusDays(DEFAULT_LOAN_DAYS));
        this.returnDate = null;
        this.status = TransactionStatus.BORROWED;
        this.fineAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public Transaction(int bookId, int userId, Timestamp borrowDate, Timestamp dueDate,
                       Timestamp returnDate, TransactionStatus status, double fineAmount) {
        super();
        this.bookId = bookId;
        this.userId = userId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status != null ? status : TransactionStatus.BORROWED;
        this.fineAmount = BigDecimal.valueOf(fineAmount).setScale(2, RoundingMode.HALF_UP);
    }

    public Transaction(Integer id, int bookId, int userId, Timestamp borrowDate, Timestamp dueDate,
                       Timestamp returnDate, TransactionStatus status, double fineAmount,
                       Timestamp createdAt, Timestamp updatedAt) {
        super(id, createdAt, updatedAt);
        this.bookId = bookId;
        this.userId = userId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status != null ? status : TransactionStatus.BORROWED;
        this.fineAmount = BigDecimal.valueOf(fineAmount).setScale(2, RoundingMode.HALF_UP);
    }

    public Transaction(Integer id, Integer bookId, Integer userId, LocalDateTime borrowDate, 
                       LocalDateTime dueDate, LocalDateTime returnDate, TransactionStatus status, 
                       BigDecimal fineAmount) {
        super(id, borrowDate, LocalDateTime.now());
        this.bookId = bookId;
        this.userId = userId;
        this.borrowDate = (borrowDate != null) ? Timestamp.valueOf(borrowDate) : null;
        this.dueDate = (dueDate != null) ? Timestamp.valueOf(dueDate) : null;
        this.returnDate = (returnDate != null) ? Timestamp.valueOf(returnDate) : null;
        this.status = (status != null) ? status : TransactionStatus.BORROWED;
        this.fineAmount = (fineAmount != null) ? fineAmount.setScale(2, RoundingMode.HALF_UP) 
                                               : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    // Business Methods
    public boolean isCurrentlyActive() {
        return status != null && status.isActive();
    }

    public boolean isActive() {
        return isCurrentlyActive();
    }

    public boolean isOverdue() {
        if (status == TransactionStatus.OVERDUE) {
            return true;
        }
        if (status == TransactionStatus.BORROWED && dueDate != null) {
            return System.currentTimeMillis() > dueDate.getTime();
        }
        return false;
    }

    public boolean isReturned() {
        return status == TransactionStatus.RETURNED || returnDate != null;
    }

    public long getDaysOverdue() {
        if (dueDate == null) {
            return 0;
        }
        long effectiveEnd = (returnDate != null) ? returnDate.getTime() : System.currentTimeMillis();
        long diffMs = effectiveEnd - dueDate.getTime();
        if (diffMs > 0) {
            return diffMs / (1000L * 60 * 60 * 24);
        }
        return 0;
    }

    public BigDecimal calculateFine(BigDecimal dailyRate) {
        long days = getDaysOverdue();
        if (days <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal rate = (dailyRate != null) ? dailyRate : DEFAULT_DAILY_FINE;
        return rate.multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
    }

    public double calculateFine(double dailyRate) {
        long days = getDaysOverdue();
        if (days <= 0) return 0.00;
        return days * dailyRate;
    }

    // Getters and Setters
    public Integer getBookId() {
        return bookId;
    }

    public void setBookId(Integer bookId) {
        this.bookId = bookId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Timestamp getBorrowDate() {
        return borrowDate;
    }

    public Timestamp getBorrowDateTimestamp() {
        return borrowDate;
    }

    public void setBorrowDate(Timestamp borrowDate) {
        this.borrowDate = borrowDate;
    }

    public void setBorrowDate(LocalDateTime borrowDate) {
        this.borrowDate = (borrowDate != null) ? Timestamp.valueOf(borrowDate) : null;
    }

    public LocalDateTime getBorrowDateLocalDateTime() {
        return borrowDate != null ? borrowDate.toLocalDateTime() : null;
    }

    public Timestamp getDueDate() {
        return dueDate;
    }

    public Timestamp getDueDateTimestamp() {
        return dueDate;
    }

    public void setDueDate(Timestamp dueDate) {
        this.dueDate = dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = (dueDate != null) ? Timestamp.valueOf(dueDate) : null;
    }

    public LocalDateTime getDueDateLocalDateTime() {
        return dueDate != null ? dueDate.toLocalDateTime() : null;
    }

    public Timestamp getReturnDate() {
        return returnDate;
    }

    public Timestamp getReturnDateTimestamp() {
        return returnDate;
    }

    public void setReturnDate(Timestamp returnDate) {
        this.returnDate = returnDate;
    }

    public void setReturnDate(LocalDateTime returnDate) {
        this.returnDate = (returnDate != null) ? Timestamp.valueOf(returnDate) : null;
    }

    public LocalDateTime getReturnDateLocalDateTime() {
        return returnDate != null ? returnDate.toLocalDateTime() : null;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public BigDecimal getFineAmount() {
        return fineAmount != null ? fineAmount : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public Double getFineAmountDouble() {
        return getFineAmount().doubleValue();
    }

    public void setFineAmount(BigDecimal fineAmount) {
        this.fineAmount = (fineAmount != null) ? fineAmount.setScale(2, RoundingMode.HALF_UP) 
                                               : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public void setFineAmount(Double fineAmount) {
        this.fineAmount = (fineAmount != null) ? BigDecimal.valueOf(fineAmount).setScale(2, RoundingMode.HALF_UP) 
                                               : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public void setFineAmount(double fineAmount) {
        this.fineAmount = BigDecimal.valueOf(fineAmount).setScale(2, RoundingMode.HALF_UP);
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", bookId=" + bookId +
                ", userId=" + userId +
                ", borrowDate=" + borrowDate +
                ", dueDate=" + dueDate +
                ", returnDate=" + returnDate +
                ", status=" + status +
                ", fineAmount=" + fineAmount +
                '}';
    }
}
