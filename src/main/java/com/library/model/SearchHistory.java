package com.library.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Concrete domain model recording a member's search query for analytics and personal history.
 */
public class SearchHistory extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Integer userId;
    private String queryText;
    private String searchType;
    private Timestamp searchedAt;

    public SearchHistory() {
        super();
        this.searchedAt = new Timestamp(System.currentTimeMillis());
    }

    public SearchHistory(Integer userId, String queryText, String searchType) {
        super();
        this.userId = userId;
        this.queryText = (queryText != null) ? queryText.trim() : "";
        this.searchType = (searchType != null && !searchType.trim().isEmpty()) 
                ? searchType.trim().toUpperCase() : "ALL";
        this.searchedAt = new Timestamp(System.currentTimeMillis());
    }

    public SearchHistory(Integer id, Integer userId, String queryText, String searchType, Timestamp searchedAt) {
        super(id, searchedAt, searchedAt);
        this.userId = userId;
        this.queryText = (queryText != null) ? queryText.trim() : "";
        this.searchType = (searchType != null && !searchType.trim().isEmpty()) 
                ? searchType.trim().toUpperCase() : "ALL";
        this.searchedAt = (searchedAt != null) ? searchedAt : new Timestamp(System.currentTimeMillis());
    }

    public SearchHistory(Integer id, Integer userId, String queryText, String searchType, LocalDateTime searchedAt) {
        super(id, searchedAt, searchedAt);
        this.userId = userId;
        this.queryText = (queryText != null) ? queryText.trim() : "";
        this.searchType = (searchType != null && !searchType.trim().isEmpty()) 
                ? searchType.trim().toUpperCase() : "ALL";
        this.searchedAt = (searchedAt != null) ? Timestamp.valueOf(searchedAt) : new Timestamp(System.currentTimeMillis());
    }

    // Getters and Setters
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText != null ? queryText.trim() : "";
    }

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = (searchType != null && !searchType.trim().isEmpty()) 
                ? searchType.trim().toUpperCase() : "ALL";
    }

    public LocalDateTime getSearchedAt() {
        return searchedAt != null ? searchedAt.toLocalDateTime() : null;
    }

    public Timestamp getSearchedAtTimestamp() {
        return searchedAt;
    }

    public void setSearchedAt(Timestamp searchedAt) {
        this.searchedAt = searchedAt;
    }

    public void setSearchedAt(LocalDateTime searchedAt) {
        this.searchedAt = searchedAt != null ? Timestamp.valueOf(searchedAt) : null;
    }

    public LocalDateTime getSearchedAtLocalDateTime() {
        return getSearchedAt();
    }

    @Override
    public String toString() {
        return "SearchHistory{" +
                "id=" + id +
                ", userId=" + userId +
                ", queryText='" + queryText + '\'' +
                ", searchType='" + searchType + '\'' +
                ", searchedAt=" + searchedAt +
                '}';
    }
}
