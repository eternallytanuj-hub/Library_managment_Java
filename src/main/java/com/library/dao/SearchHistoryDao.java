package com.library.dao;

import com.library.model.SearchHistory;
import com.library.exception.DatabaseOperationException;

import java.sql.Timestamp;
import java.util.List;

/**
 * Data Access Object interface for {@link SearchHistory} search log audit entities.
 */
public interface SearchHistoryDao extends GenericDao<SearchHistory, Integer> {

    List<SearchHistory> findRecentByUserId(int userId, int limit) throws DatabaseOperationException;

    boolean deleteByUserId(int userId) throws DatabaseOperationException;

    int clearOldHistory(Timestamp beforeDate) throws DatabaseOperationException;
}
