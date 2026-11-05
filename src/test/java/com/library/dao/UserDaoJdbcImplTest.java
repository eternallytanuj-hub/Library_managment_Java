package com.library.dao;

import com.library.dao.impl.BookDaoJdbcImpl;
import com.library.dao.impl.TransactionDaoJdbcImpl;
import com.library.dao.impl.UserDaoJdbcImpl;
import com.library.exception.DuplicateEntityException;
import com.library.model.Book;
import com.library.model.Librarian;
import com.library.model.Member;
import com.library.model.Role;
import com.library.model.Transaction;
import com.library.model.TransactionStatus;
import com.library.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("UserDaoJdbcImpl Integration Tests")
class UserDaoJdbcImplTest extends BaseDaoTest {

    private UserDao userDao;
    private BookDao bookDao;
    private TransactionDao transactionDao;

    @BeforeEach
    void setUp() {
        userDao = new UserDaoJdbcImpl();
        bookDao = new BookDaoJdbcImpl();
        transactionDao = new TransactionDaoJdbcImpl();
    }

    @Test
    @DisplayName("Should successfully save a new Member and retrieve by generated ID")
    void testSaveAndFindById_Success() throws Exception {
        Member member = new Member();
        member.setMembershipId("MEM-1001");
        member.setName("Alice Walker");
        member.setEmail("alice@test.com");
        member.setPasswordHash("$2a$10$hashedpassword");

        User saved = userDao.save(member);
        assertNotNull(saved.getId(), "Generated user ID must not be null");
        assertTrue(saved.getId() > 0, "Generated user ID must be positive");

        Optional<User> fetched = userDao.findById(saved.getId());
        assertTrue(fetched.isPresent(), "Saved user must be retrievable by ID");
        User u = fetched.get();
        assertEquals("Alice Walker", u.getName());
        assertEquals("alice@test.com", u.getEmail());
        assertEquals(Role.MEMBER, u.getRole());
        assertInstanceOf(Member.class, u, "Role MEMBER must deserialize as Member instance");
    }

    @Test
    @DisplayName("Should throw DuplicateEntityException when inserting duplicate email")
    void testSaveDuplicateEmail_ThrowsDuplicateEntityException() throws Exception {
        Member m1 = new Member();
        m1.setMembershipId("MEM-1001");
        m1.setName("Alice Walker");
        m1.setEmail("duplicate@test.com");
        m1.setPasswordHash("pass1");
        userDao.save(m1);

        Member m2 = new Member();
        m2.setMembershipId("MEM-1002");
        m2.setName("Bob Smith");
        m2.setEmail("duplicate@test.com");
        m2.setPasswordHash("pass2");

        assertThrows(DuplicateEntityException.class, () -> userDao.save(m2),
                "Saving user with duplicate email must throw DuplicateEntityException");
    }

    @Test
    @DisplayName("Should throw DuplicateEntityException when inserting duplicate membership ID")
    void testSaveDuplicateMembershipId_ThrowsDuplicateEntityException() throws Exception {
        Member m1 = new Member();
        m1.setMembershipId("DUPLICATE-ID");
        m1.setName("User One");
        m1.setEmail("one@test.com");
        m1.setPasswordHash("pass1");
        userDao.save(m1);

        Member m2 = new Member();
        m2.setMembershipId("DUPLICATE-ID");
        m2.setName("User Two");
        m2.setEmail("two@test.com");
        m2.setPasswordHash("pass2");

        assertThrows(DuplicateEntityException.class, () -> userDao.save(m2),
                "Saving user with duplicate membershipId must throw DuplicateEntityException");
    }

    @Test
    @DisplayName("Should polymorphically instantiate Librarian and Member instances on query")
    void testPolymorphicRoleRetrieval() throws Exception {
        Librarian lib = new Librarian();
        lib.setMembershipId("LIB-001");
        lib.setName("Chief Librarian");
        lib.setEmail("chief@library.com");
        lib.setPasswordHash("adminpass");
        userDao.save(lib);

        Member mem = new Member();
        mem.setMembershipId("MEM-001");
        mem.setName("Student Member");
        mem.setEmail("student@school.com");
        mem.setPasswordHash("studentpass");
        userDao.save(mem);

        List<User> allUsers = userDao.findAll();
        assertEquals(2, allUsers.size());

        User user1 = userDao.findByEmail("chief@library.com").orElseThrow();
        User user2 = userDao.findByEmail("student@school.com").orElseThrow();

        assertInstanceOf(Librarian.class, user1);
        assertTrue(user1.isLibrarian());
        assertFalse(user1.isMember());

        assertInstanceOf(Member.class, user2);
        assertTrue(user2.isMember());
        assertFalse(user2.isLibrarian());
    }

    @Test
    @DisplayName("Should successfully update profile and password")
    void testUpdateProfileAndPassword() throws Exception {
        Member mem = new Member();
        mem.setMembershipId("MEM-UPDATE");
        mem.setName("Old Name");
        mem.setEmail("old@test.com");
        mem.setPasswordHash("oldpass");
        User saved = userDao.save(mem);

        boolean profileUpdated = userDao.updateProfile(saved.getId(), "New Name", "new@test.com");
        assertTrue(profileUpdated);

        boolean passUpdated = userDao.updatePassword(saved.getId(), "newpasshash");
        assertTrue(passUpdated);

        User refreshed = userDao.findById(saved.getId()).orElseThrow();
        assertEquals("New Name", refreshed.getName());
        assertEquals("new@test.com", refreshed.getEmail());
        assertEquals("newpasshash", refreshed.getPasswordHash());
    }

    @Test
    @DisplayName("Should update alert preferences correctly")
    void testUpdateAlertPreferences() throws Exception {
        Member mem = new Member();
        mem.setMembershipId("MEM-PREF");
        mem.setName("Preference User");
        mem.setEmail("pref@test.com");
        mem.setPasswordHash("pass");
        User saved = userDao.save(mem);

        boolean updated = userDao.updateAlertPreferences(saved.getId(), false, true, false);
        assertTrue(updated);

        User refreshed = userDao.findById(saved.getId()).orElseThrow();
        assertFalse(refreshed.isEmailNotifications());
        assertTrue(refreshed.isDueDateAlerts());
        assertFalse(refreshed.isNewArrivalAlerts());
    }

    @Test
    @DisplayName("Should delete user by ID and verify removal")
    void testDeleteById_Success() throws Exception {
        Member mem = new Member();
        mem.setMembershipId("MEM-DEL");
        mem.setName("Delete Me");
        mem.setEmail("del@test.com");
        mem.setPasswordHash("pass");
        User saved = userDao.save(mem);

        boolean deleted = userDao.deleteById(saved.getId());
        assertTrue(deleted);

        Optional<User> opt = userDao.findById(saved.getId());
        assertFalse(opt.isPresent(), "Deleted user should not be found");
    }

    @Test
    @DisplayName("Should accurately calculate active loan count per member")
    void testFindAllMembersWithActiveLoanCount() throws Exception {
        Member mem = new Member();
        mem.setMembershipId("MEM-LOAN");
        mem.setName("Borrower Pat");
        mem.setEmail("pat@test.com");
        mem.setPasswordHash("pass");
        User savedUser = userDao.save(mem);

        Book b1 = bookDao.save(new Book("ISBN-1", "Book One", "Author A", "Genre X", 5, 5));
        Book b2 = bookDao.save(new Book("ISBN-2", "Book Two", "Author B", "Genre Y", 5, 5));

        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp due = new Timestamp(System.currentTimeMillis() + 86400000L * 14);

        Transaction t1 = new Transaction(b1.getId(), savedUser.getId(), now, due, null, TransactionStatus.BORROWED, 0.0);
        Transaction t2 = new Transaction(b2.getId(), savedUser.getId(), now, due, null, TransactionStatus.OVERDUE, 2.0);
        transactionDao.save(t1);
        transactionDao.save(t2);

        List<Member> members = userDao.findAllMembersWithActiveLoanCount();
        assertEquals(1, members.size());
        assertEquals(2, members.get(0).getActiveLoansCount());
        assertEquals(2, userDao.countActiveLoans(savedUser.getId()));
    }
}
