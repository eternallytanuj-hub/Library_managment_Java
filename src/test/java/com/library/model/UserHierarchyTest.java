package com.library.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Hierarchy and OOP Polymorphism Unit Tests")
class UserHierarchyTest {

    @Test
    @DisplayName("Verify Librarian instantiation and administrative properties")
    void testLibrarianInstantiation() {
        Librarian librarian = new Librarian(1, "LIB-001", "Head Librarian", "admin@library.com", "$2a$10$hashedAdminPass");

        assertEquals(1, librarian.getId());
        assertEquals("LIB-001", librarian.getMembershipId());
        assertEquals("Head Librarian", librarian.getName());
        assertEquals("admin@library.com", librarian.getEmail());
        assertEquals(Role.LIBRARIAN, librarian.getRole());
        assertTrue(librarian.isLibrarian());
        assertFalse(librarian.isMember());

        // Polymorphic methods
        assertFalse(librarian.canBorrow(), "Administrative accounts must not have borrowing privileges");
        assertEquals(0, librarian.getMaxBorrowLimit());
    }

    @Test
    @DisplayName("Verify Member instantiation, borrowing quota, and counter limits")
    void testMemberInstantiation() {
        Member member = new Member(2, "MEM-1001", "Alice Johnson", "alice@example.com", "$2a$10$hashedMemberPass");

        assertEquals(2, member.getId());
        assertEquals("MEM-1001", member.getMembershipId());
        assertEquals("Alice Johnson", member.getName());
        assertEquals("alice@example.com", member.getEmail());
        assertEquals(Role.MEMBER, member.getRole());
        assertTrue(member.isMember());
        assertFalse(member.isLibrarian());

        // Borrowing privileges
        assertEquals(5, member.getMaxBorrowLimit());
        assertEquals(0, member.getActiveBorrowCount());
        assertTrue(member.canBorrow());

        // Increment loans
        for (int i = 0; i < 5; i++) {
            member.incrementActiveBorrowCount();
        }
        assertEquals(5, member.getActiveBorrowCount());
        assertFalse(member.canBorrow(), "Member should not be able to borrow after reaching max borrow limit");

        // Attempting to exceed max borrow limit throws IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, member::incrementActiveBorrowCount);
        assertTrue(ex.getMessage().contains("reached maximum borrow limit"));

        // Decrement loans
        member.decrementActiveBorrowCount();
        assertEquals(4, member.getActiveBorrowCount());
        assertTrue(member.canBorrow());
    }

    @Test
    @DisplayName("Verify role immutability guards against privilege escalation")
    void testRoleImmutabilityProtection() {
        Librarian librarian = new Librarian("LIB-001", "Librarian", "lib@library.com", "pass");
        Member member = new Member("MEM-001", "Member", "mem@example.com", "pass");

        // Changing Librarian role to Member must fail
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> librarian.setRole(Role.MEMBER));
        assertTrue(ex1.getMessage().contains("Cannot change role of Librarian"));

        // Changing Member role to Librarian must fail
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> member.setRole(Role.LIBRARIAN));
        assertTrue(ex2.getMessage().contains("Cannot change role of Member"));

        // Setting same role is allowed
        assertDoesNotThrow(() -> librarian.setRole(Role.LIBRARIAN));
        assertDoesNotThrow(() -> member.setRole(Role.MEMBER));
    }

    @Test
    @DisplayName("Verify polymorphic dispatch across a heterogeneous collection of Users")
    void testPolymorphicUserCollection() {
        Librarian librarian = new Librarian(1, "LIB-001", "Admin", "admin@library.com", "hash");
        Member memberWithRoom = new Member(2, "MEM-001", "Alice", "alice@example.com", "hash");
        Member memberFull = new Member(3, "MEM-002", "Bob", "bob@example.com", "hash");
        memberFull.setActiveBorrowCount(5);

        List<User> users = List.of(librarian, memberWithRoom, memberFull);

        assertFalse(users.get(0).canBorrow(), "Librarian cannot borrow");
        assertTrue(users.get(1).canBorrow(), "Member with 0 loans can borrow");
        assertFalse(users.get(2).canBorrow(), "Member with 5 loans cannot borrow");

        assertEquals(0, users.get(0).getMaxBorrowLimit());
        assertEquals(5, users.get(1).getMaxBorrowLimit());
        assertEquals(5, users.get(2).getMaxBorrowLimit());
    }

    @Test
    @DisplayName("Verify password hash is protected and redacted in toString()")
    void testPasswordHashRedaction() {
        Member member = new Member(1, "MEM-001", "Alice", "alice@example.com", "super_secret_raw_hash_999");
        String str = member.toString();

        assertTrue(str.contains("[PROTECTED]"), "toString must mask password hash");
        assertFalse(str.contains("super_secret_raw_hash_999"), "toString must never expose the raw password hash");
    }

    @Test
    @DisplayName("Verify alert preferences can be toggled independently")
    void testAlertPreferencesToggling() {
        Member member = new Member("MEM-001", "Alice", "alice@example.com", "pass");

        // Defaults should be true
        assertTrue(member.isEmailNotifications());
        assertTrue(member.isDueDateAlerts());
        assertTrue(member.isNewArrivalAlerts());

        // Toggle
        member.setEmailNotifications(false);
        member.setDueDateAlerts(false);
        member.setNewArrivalAlerts(false);

        assertFalse(member.isEmailNotifications());
        assertFalse(member.isDueDateAlerts());
        assertFalse(member.isNewArrivalAlerts());
    }

    @Test
    @DisplayName("Verify Role enum helper methods and URL resolutions")
    void testRoleEnum() {
        assertEquals("Librarian", Role.LIBRARIAN.getDisplayName());
        assertEquals("/librarian/dashboard", Role.LIBRARIAN.getDefaultDashboardUrl());

        assertEquals("Member", Role.MEMBER.getDisplayName());
        assertEquals("/member/dashboard", Role.MEMBER.getDefaultDashboardUrl());

        assertEquals(Role.LIBRARIAN, Role.fromString("LIBRARIAN"));
        assertEquals(Role.LIBRARIAN, Role.fromString("librarian"));
        assertEquals(Role.MEMBER, Role.fromString("MEMBER"));
        assertEquals(Role.MEMBER, Role.fromString("member"));
        assertNull(Role.fromString(null));
        assertNull(Role.fromString("   "));

        assertThrows(IllegalArgumentException.class, () -> Role.fromString("SUPER_ADMIN"));
    }

    @Test
    @DisplayName("Verify BaseEntity identity contract: equals and hashCode across entities")
    void testBaseEntityIdentity() {
        Member m1 = new Member(10, "M-1", "Name 1", "e1@test.com", "h");
        Member m2 = new Member(10, "M-2", "Name 2", "e2@test.com", "h");
        Member m3 = new Member(20, "M-1", "Name 1", "e1@test.com", "h");
        Librarian l1 = new Librarian(10, "L-1", "Admin", "a@test.com", "h");

        assertEquals(m1, m2, "Same ID and class should be equal");
        assertEquals(m1.hashCode(), m2.hashCode(), "Same ID and class should have same hashCode");
        assertNotEquals(m1, m3, "Different IDs should not be equal");
        assertNotEquals(m1, l1, "Different classes should not be equal even with same ID");
        assertNotEquals(m1, null);
    }

    @Test
    @DisplayName("Verify maxBooksAllowed rubric alias accessors on Member and User")
    void testMaxBooksAllowedRubricAliases() {
        Member member = new Member();
        assertEquals(Member.DEFAULT_MAX_BORROW_LIMIT, member.getMaxBooksAllowed());
        assertEquals(Member.DEFAULT_MAX_BOOKS_ALLOWED, member.getMaxBorrowLimit());

        member.setMaxBooksAllowed(7);
        assertEquals(7, member.getMaxBooksAllowed());
        assertEquals(7, member.getMaxBorrowLimit());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> member.setMaxBooksAllowed(-1));
        assertTrue(ex.getMessage().contains("cannot be negative"));

        User user = member;
        assertEquals(7, user.getMaxBooksAllowed());

        Librarian librarian = new Librarian();
        assertEquals(0, librarian.getMaxBooksAllowed());
    }
}
