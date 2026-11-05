package com.library.dao;

import com.library.dao.impl.SearchHistoryDaoJdbcImpl;
import com.library.dao.impl.UserDaoJdbcImpl;
import com.library.model.Member;
import com.library.model.SearchHistory;
import com.library.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SearchHistoryDaoJdbcImpl Integration Tests")
class SearchHistoryDaoJdbcImplTest extends BaseDaoTest {

    private SearchHistoryDao searchHistoryDao;
    private UserDao userDao;
    private User sampleUser;

    @BeforeEach
    void setUp() throws Exception {
        searchHistoryDao = new SearchHistoryDaoJdbcImpl();
        userDao = new UserDaoJdbcImpl();

        Member m = new Member();
        m.setMembershipId("SEARCH-USER");
        m.setName("Searcher Pat");
        m.setEmail("searcher@test.com");
        m.setPasswordHash("pass");
        sampleUser = userDao.save(m);
    }

    @Test
    @DisplayName("Should save searches and retrieve recent searches with limit")
    void testSaveAndFindRecent() throws Exception {
        searchHistoryDao.save(new SearchHistory(sampleUser.getId(), "Effective Java", "TITLE"));
        searchHistoryDao.save(new SearchHistory(sampleUser.getId(), "Clean Code", "TITLE"));
        searchHistoryDao.save(new SearchHistory(sampleUser.getId(), "Robert Martin", "AUTHOR"));

        List<SearchHistory> recent = searchHistoryDao.findRecentByUserId(sampleUser.getId(), 2);
        assertEquals(2, recent.size(), "Should respect query limit of 2");
        assertNotNull(recent.get(0).getId());
        assertEquals("Robert Martin", recent.get(0).getQueryText(), "Most recent search must appear first");
    }

    @Test
    @DisplayName("Should clear search history for a user")
    void testDeleteByUserId() throws Exception {
        searchHistoryDao.save(new SearchHistory(sampleUser.getId(), "Query 1", "ALL"));
        searchHistoryDao.save(new SearchHistory(sampleUser.getId(), "Query 2", "ALL"));

        boolean cleared = searchHistoryDao.deleteByUserId(sampleUser.getId());
        assertTrue(cleared);

        List<SearchHistory> remaining = searchHistoryDao.findRecentByUserId(sampleUser.getId(), 10);
        assertTrue(remaining.isEmpty());
    }

    @Test
    @DisplayName("Should clear history older than specified threshold timestamp")
    void testClearOldHistory() throws Exception {
        SearchHistory oldSearch = new SearchHistory(sampleUser.getId(), "Ancient Search", "ALL");
        oldSearch.setSearchedAt(new Timestamp(System.currentTimeMillis() - 86400000L * 40)); // 40 days ago
        searchHistoryDao.save(oldSearch);

        SearchHistory newSearch = new SearchHistory(sampleUser.getId(), "Recent Search", "ALL");
        newSearch.setSearchedAt(new Timestamp(System.currentTimeMillis()));
        searchHistoryDao.save(newSearch);

        Timestamp cutoff = new Timestamp(System.currentTimeMillis() - 86400000L * 30); // 30 days ago
        int deleted = searchHistoryDao.clearOldHistory(cutoff);
        assertEquals(1, deleted);

        List<SearchHistory> remaining = searchHistoryDao.findRecentByUserId(sampleUser.getId(), 10);
        assertEquals(1, remaining.size());
        assertEquals("Recent Search", remaining.get(0).getQueryText());
    }
}
