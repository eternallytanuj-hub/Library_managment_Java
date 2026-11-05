package com.library.dao.impl;

import com.library.dao.SearchHistoryDao;
import com.library.exception.DatabaseOperationException;
import com.library.model.SearchHistory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link SearchHistoryDao} managing member catalog search logs.
 */
public class SearchHistoryDaoJdbcImpl extends BaseJdbcDao implements SearchHistoryDao {

    @Override
    public Optional<SearchHistory> findById(Integer id) throws DatabaseOperationException {
        if (id == null) return Optional.empty();
        String sql = "SELECT * FROM search_history WHERE search_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSearchHistory(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find search history by ID: " + id, e);
        }
    }

    @Override
    public List<SearchHistory> findAll() throws DatabaseOperationException {
        String sql = "SELECT * FROM search_history ORDER BY searched_at DESC, search_id DESC";
        List<SearchHistory> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToSearchHistory(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to list all search history", e);
        }
    }

    @Override
    public SearchHistory save(SearchHistory sh) throws DatabaseOperationException {
        String sql = "INSERT INTO search_history (user_id, query_text, search_type, searched_at) " +
                     "VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Timestamp now = sh.getSearchedAtTimestamp() != null ? sh.getSearchedAtTimestamp() : new Timestamp(System.currentTimeMillis());
            ps.setInt(1, sh.getUserId());
            ps.setString(2, sh.getQueryText());
            ps.setString(3, sh.getSearchType() != null ? sh.getSearchType().toUpperCase() : "ALL");
            ps.setTimestamp(4, now);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DatabaseOperationException("Creating search history failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    sh.setId(generatedKeys.getInt(1));
                } else {
                    throw new DatabaseOperationException("Creating search history failed, no ID obtained.");
                }
            }
            sh.setSearchedAt(now);
            sh.setCreatedAt(now);
            sh.setUpdatedAt(now);
            return sh;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to save search history", e);
        }
    }

    @Override
    public boolean update(SearchHistory sh) throws DatabaseOperationException {
        String sql = "UPDATE search_history SET user_id = ?, query_text = ?, search_type = ?, searched_at = ? " +
                     "WHERE search_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sh.getUserId());
            ps.setString(2, sh.getQueryText());
            ps.setString(3, sh.getSearchType());
            ps.setTimestamp(4, sh.getSearchedAtTimestamp());
            ps.setInt(5, sh.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to update search history: " + sh.getId(), e);
        }
    }

    @Override
    public boolean deleteById(Integer id) throws DatabaseOperationException {
        if (id == null) return false;
        String sql = "DELETE FROM search_history WHERE search_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to delete search history with ID: " + id, e);
        }
    }

    @Override
    public List<SearchHistory> findRecentByUserId(int userId, int limit) throws DatabaseOperationException {
        String sql = "SELECT * FROM search_history WHERE user_id = ? ORDER BY searched_at DESC, search_id DESC LIMIT ?";
        List<SearchHistory> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit > 0 ? limit : 10);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSearchHistory(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find recent searches for user: " + userId, e);
        }
    }

    @Override
    public boolean deleteByUserId(int userId) throws DatabaseOperationException {
        String sql = "DELETE FROM search_history WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() >= 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to clear search history for user: " + userId, e);
        }
    }

    @Override
    public int clearOldHistory(Timestamp beforeDate) throws DatabaseOperationException {
        String sql = "DELETE FROM search_history WHERE searched_at < ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, beforeDate != null ? beforeDate : new Timestamp(System.currentTimeMillis()));
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to clear old search history", e);
        }
    }

    protected SearchHistory mapResultSetToSearchHistory(ResultSet rs) throws SQLException {
        SearchHistory sh = new SearchHistory();
        sh.setId(rs.getInt("search_id"));
        sh.setUserId(rs.getInt("user_id"));
        sh.setQueryText(rs.getString("query_text"));
        sh.setSearchType(rs.getString("search_type"));
        sh.setSearchedAt(rs.getTimestamp("searched_at"));
        sh.setCreatedAt(rs.getTimestamp("searched_at"));
        sh.setUpdatedAt(rs.getTimestamp("searched_at"));
        return sh;
    }
}
