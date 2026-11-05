package com.library.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * Handles database bootstrapping and script execution.
 * Checks whether the schema is initialized; if absent, parses and executes
 * {@code schema.sql} and {@code data.sql} resources.
 */
public final class DBInitializer {

    private static final Logger LOGGER = Logger.getLogger(DBInitializer.class.getName());

    private DBInitializer() {
        // Utility class
    }

    /**
     * Inspects the database and triggers DDL and DML scripts if the users table is absent.
     *
     * @param dataSource the active DataSource to acquire a bootstrap connection from
     * @throws SQLException if a database error occurs during initialization
     */
    public static void initialize(DataSource dataSource) throws SQLException {
        boolean autoInit = AppConfig.getBoolean("app.db.autoInit", true);
        if (!autoInit) {
            LOGGER.info("Database auto-initialization is disabled via configuration.");
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            if (!isSchemaPresent(conn)) {
                LOGGER.info("Core database tables not detected. Commencing bootstrap execution...");
                String schemaPath = AppConfig.getProperty("app.db.schemaPath", "db/schema.sql");
                String dataPath = AppConfig.getProperty("app.db.dataPath", "db/data.sql");

                executeSqlScript(conn, schemaPath);
                executeSqlScript(conn, dataPath);
                LOGGER.info("Database schema and seed data successfully initialized.");
            } else {
                LOGGER.info("Database schema already detected. Skipping bootstrap script execution.");
            }
        }
    }

    /**
     * Checks if the core 'users' table is queryable.
     */
    public static boolean isSchemaPresent(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT 1 FROM users LIMIT 1");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Reads an SQL file from the classpath and executes statements separated by semicolons.
     * Skips single-line comments and empty lines.
     *
     * @param conn database connection to use
     * @param resourcePath classpath path to the SQL file
     * @throws SQLException if an execution or I/O error occurs
     */
    public static void executeSqlScript(Connection conn, String resourcePath) throws SQLException {
        InputStream is = DBInitializer.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null && !resourcePath.startsWith("/")) {
            is = DBInitializer.class.getClassLoader().getResourceAsStream("/" + resourcePath);
        }
        if (is == null) {
            throw new SQLException("SQL script resource could not be found: " + resourcePath);
        }

        LOGGER.info("Executing SQL script: " + resourcePath);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
             Statement stmt = conn.createStatement()) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                // Skip empty lines and comments
                if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("//")) {
                    continue;
                }

                sb.append(line).append("\n");

                // Check if statement concludes with a semicolon
                if (trimmed.endsWith(";")) {
                    String sql = sb.toString().trim();
                    if (sql.endsWith(";")) {
                        sql = sql.substring(0, sql.length() - 1).trim();
                    }
                    if (!sql.isEmpty()) {
                        stmt.execute(sql);
                    }
                    sb.setLength(0);
                }
            }

            // Execute trailing SQL statement if no closing semicolon was present
            String remaining = sb.toString().trim();
            if (!remaining.isEmpty()) {
                stmt.execute(remaining);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read SQL script resource: " + resourcePath, e);
            throw new SQLException("I/O error while reading script: " + resourcePath, e);
        }
    }
}
