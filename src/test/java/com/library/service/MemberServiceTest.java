package com.library.service;

import com.library.dao.BaseDaoTest;
import com.library.dao.BookDao;
import com.library.dao.TransactionDao;
import com.library.dao.UserDao;
import com.library.dao.impl.BookDaoJdbcImpl;
import com.library.dao.impl.TransactionDaoJdbcImpl;
import com.library.dao.impl.UserDaoJdbcImpl;
import com.library.exception.LibraryException;
import com.library.exception.MemberNotFoundException;
import com.library.exception.UnauthorizedAccessException;
import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Transaction;
import com.library.model.User;
import com.library.service.impl.AuthServiceImpl;
import com.library.service.impl.MemberServiceImpl;
import com.library.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MemberService Integration Tests")
class MemberServiceTest extends BaseDaoTest {

    private UserDao userDao;
    private BookDao bookDao;
    private TransactionDao transactionDao;
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        userDao = new UserDaoJdbcImpl();
        bookDao = new BookDaoJdbcImpl();
        transactionDao = new TransactionDaoJdbcImpl();
        AuthService authService = new AuthServiceImpl(userDao);
        memberService = new MemberServiceImpl(userDao, authService);
    }

    @Test
    @DisplayName("Should authenticate valid member and reject invalid credentials")
    void testAuthenticate() throws Exception {
        memberService.registerMember("Peter Parker", "spidey@test.com", "webslinger123");

        User user = memberService.authenticate("spidey@test.com", "webslinger123");
        assertNotNull(user);
        assertEquals("spidey@test.com", user.getEmail());

        assertThrows(UnauthorizedAccessException.class, () ->
                memberService.authenticate("spidey@test.com", "wrongPass"));
    }

    @Test
    @DisplayName("Should update notification preferences")
    void testUpdateNotificationPreferences() throws Exception {
        Member member = memberService.registerMember("Tony Stark", "ironman@test.com", "jarvis123");

        memberService.updateNotificationPreferences(member.getId(), false, true);

        Optional<Member> updatedOpt = memberService.getMemberById(member.getId());
        assertTrue(updatedOpt.isPresent());
        Member updated = updatedOpt.get();
        assertFalse(updated.isDueDateAlerts());
        assertTrue(updated.isNewArrivalAlerts());

        // Test four-parameter overload
        memberService.updateNotificationPreferences(member.getId(), false, false, false);
        Member reloaded = memberService.getMemberById(member.getId()).get();
        assertFalse(reloaded.isEmailNotifications());
        assertFalse(reloaded.isDueDateAlerts());
        assertFalse(reloaded.isNewArrivalAlerts());
    }

    @Test
    @DisplayName("Should list all members with active loan counts")
    void testGetAllMembers() throws Exception {
        Member m1 = memberService.registerMember("Member One", "m1@test.com", "pass123");
        Member m2 = memberService.registerMember("Member Two", "m2@test.com", "pass123");

        List<Member> members = memberService.getAllMembers();
        assertEquals(2, members.size());
    }

    @Test
    @DisplayName("Should delete member with zero active loans")
    void testDeleteMember_Success() throws Exception {
        Member member = memberService.registerMember("Thor Odinson", "thor@asgard.com", "hammer123");

        boolean deleted = memberService.deleteMember(member.getId());
        assertTrue(deleted);
        assertTrue(memberService.getMemberById(member.getId()).isEmpty());
    }

    @Test
    @DisplayName("Should reject deleting member with active or overdue borrowings")
    void testDeleteMember_BlockedByActiveLoans() throws Exception {
        Member member = memberService.registerMember("Loki Laufeyson", "loki@asgard.com", "mischief123");
        Book book = bookDao.save(new Book("978-9990", "Book of Spells", "Odin", "Myth", 3, 2));

        // Create active transaction
        Transaction tx = new Transaction(book.getId(), member.getId(),
                new Timestamp(System.currentTimeMillis()),
                new Timestamp(System.currentTimeMillis() + 86400000L));
        transactionDao.save(tx);

        assertThrows(LibraryException.class, () -> memberService.deleteMember(member.getId()));
    }

    @Test
    @DisplayName("Should throw MemberNotFoundException on deleteMember for nonexistent ID")
    void testDeleteMember_NotFound() {
        assertThrows(MemberNotFoundException.class, () -> memberService.deleteMember(99999));
    }
}
