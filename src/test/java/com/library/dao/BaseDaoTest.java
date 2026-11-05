package com.library.dao;

import com.library.config.AppConfig;
import com.library.config.DBConnectionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Base abstract test class for DAO integration tests.
 * Cleans the database tables before each individual test to guarantee test isolation.
 */
public abstract class BaseDaoTest {

    @BeforeAll
    static void initTestSuite() throws Exception {
        AppConfig.loadProperties();
        // Trigger pool initialization and bootstrap DDL
        try (Connection conn = DBConnectionFactory.getConnection()) {
            // ensure connection is live
        }
    }

    @BeforeEach
    void resetDatabaseState() throws Exception {
        try (Connection conn = DBConnectionFactory.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");
            stmt.execute("TRUNCATE TABLE search_history");
            stmt.execute("TRUNCATE TABLE notifications");
            stmt.execute("TRUNCATE TABLE transactions");
            stmt.execute("TRUNCATE TABLE books");
            stmt.execute("TRUNCATE TABLE users");
            stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }
}
