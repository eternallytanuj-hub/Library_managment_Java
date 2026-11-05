package com.library.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DBConnectionFactory and Bootstrap Integration Tests")
class DBConnectionFactoryTest {

    @BeforeAll
    static void setup() {
        // Force pool initialization
        AppConfig.loadProperties();
    }

    @AfterAll
    static void tearDown() {
        // Pool can remain open or close cleanly
    }

    @Test
    @DisplayName("Should initialize HikariCP pool and acquire valid connections")
    void testPoolInitializationAndConnectionAcquisition() throws SQLException {
        try (Connection conn = DBConnectionFactory.getConnection()) {
            assertNotNull(conn, "Acquired connection must not be null");
            assertFalse(conn.isClosed(), "Connection must be active and open");
            assertTrue(conn.isValid(2), "Connection must be valid within 2 seconds timeout");
        }
    }

    @Test
    @DisplayName("Should automatically bootstrap schema with all 5 core tables")
    void testSchemaBootstrapTablesPresent() throws SQLException {
        try (Connection conn = DBConnectionFactory.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            Set<String> tableNames = new HashSet<>();
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tableNames.add(rs.getString("TABLE_NAME").toLowerCase());
                }
            }

            assertTrue(tableNames.contains("users"), "Schema must contain 'users' table");
            assertTrue(tableNames.contains("books"), "Schema must contain 'books' table");
            assertTrue(tableNames.contains("transactions"), "Schema must contain 'transactions' table");
            assertTrue(tableNames.contains("notifications"), "Schema must contain 'notifications' table");
            assertTrue(tableNames.contains("search_history"), "Schema must contain 'search_history' table");
        }
    }

    @Test
    @DisplayName("Should successfully execute statements and return connections to pool")
    void testConnectionReuse() throws SQLException {
        for (int i = 0; i < 15; i++) {
            try (Connection conn = DBConnectionFactory.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                assertTrue(rs.next());
            }
        }
    }
}
