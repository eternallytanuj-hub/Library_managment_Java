package com.library.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Base abstract class for all domain entities providing common primary key (id)
 * and auditing timestamps (createdAt, updatedAt).
 */
public abstract class BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    protected Integer id;
    protected Timestamp createdAt;
    protected Timestamp updatedAt;

    public BaseEntity() {
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    public BaseEntity(Integer id, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.createdAt = createdAt != null ? createdAt : new Timestamp(System.currentTimeMillis());
        this.updatedAt = updatedAt != null ? updatedAt : new Timestamp(System.currentTimeMillis());
    }

    public BaseEntity(Integer id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.createdAt = createdAt != null ? Timestamp.valueOf(createdAt) : new Timestamp(System.currentTimeMillis());
        this.updatedAt = updatedAt != null ? Timestamp.valueOf(updatedAt) : new Timestamp(System.currentTimeMillis());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt != null ? Timestamp.valueOf(createdAt) : null;
    }

    public LocalDateTime getCreatedAtLocalDateTime() {
        return createdAt != null ? createdAt.toLocalDateTime() : null;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt != null ? Timestamp.valueOf(updatedAt) : null;
    }

    public LocalDateTime getUpdatedAtLocalDateTime() {
        return updatedAt != null ? updatedAt.toLocalDateTime() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
