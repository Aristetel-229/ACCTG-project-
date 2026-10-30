// This class lives in the database package.
package com.tala.db;

// JDBC classes: Connection is an open link to the database, DriverManager creates it, SQLException is the error type.
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * MySQL connection helper.
 * Set TALA_DB_URL, TALA_DB_USER, and TALA_DB_PASSWORD
 * environment variables, or edit the defaults below for local development.
 */
public final class DB {
    // JDBC address of the MySQL database "tala" on this computer (port 3306); uses env variable if set, else this default.
    private static final String URL = env("TALA_DB_URL",
            "jdbc:mysql://localhost:3306/tala?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Manila");
    // Database username (default "root").
    private static final String USER = env("TALA_DB_USER", "root");
    // Database password (default is for local development only).
    private static final String PASSWORD = env("TALA_DB_PASSWORD", "Sealthiel_2006");

    // Private constructor: nobody can create a DB object; we only use its static methods.
    private DB() {
    }

    // Reads an environment variable; if it is missing or blank, returns the fallback value instead.
    private static String env(String key, String fallback) {
        // Ask the operating system for the variable's value.
        String value = System.getenv(key);
        // Use the fallback when nothing useful was set.
        return value == null || value.isBlank() ? fallback : value;
    }

    // Opens a NEW connection to MySQL. Callers must close it (they use try-with-resources).
    public static Connection getConnection() throws SQLException {
        // Connect using the URL, user and password defined above.
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
