// Package (folder) for database-related classes.
package com.tala.db;

// JDBC = Java's built-in standard for talking to databases.
// Connection = an open link to the database.
import java.sql.Connection;
// DriverManager = the helper that opens a Connection using the MySQL driver.
import java.sql.DriverManager;
// SQLException = the error type thrown when any database operation fails.
import java.sql.SQLException;

/**
 * MySQL connection helper.
 * Set TALA_DB_URL, TALA_DB_USER, and TALA_DB_PASSWORD
 * environment variables, or edit the defaults below for local development.
 */
// "final" = this class cannot be extended. It only holds static helpers, so it is never instantiated.
public final class DB {
    // The JDBC connection address. env(...) first looks for the environment variable TALA_DB_URL;
    // if it is not set, the second text is used as the default.
    // Breakdown of the default URL:
    //   jdbc:mysql://localhost:3306/tala  -> MySQL running on THIS computer, port 3306, database named "tala"
    //   useSSL=false                      -> don't encrypt the connection (fine for local development)
    //   allowPublicKeyRetrieval=true      -> lets MySQL 8 login work with its default password method
    //   serverTimezone=Asia/Manila        -> dates/times use the Philippine timezone
    private static final String URL = env("TALA_DB_URL",
            "jdbc:mysql://localhost:3306/tala?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Manila");
    // MySQL username: environment variable TALA_DB_USER, or "root" if not set.
    private static final String USER = env("TALA_DB_USER", "root");
    // SECURITY NOTE: the second value below is your real MySQL password written directly in the code.
    // Anyone who gets this file (GitHub, a zip, a screenshot) can see it.
    // Better: set the TALA_DB_PASSWORD environment variable and put a dummy value here.
    // (Left unchanged so the app still runs exactly as before.)
    private static final String PASSWORD = env("TALA_DB_PASSWORD", "Sealthiel_2006");

    // Private constructor: nobody can write "new DB()". This class is only used through DB.getConnection().
    private DB() {
    }

    // Reads an environment variable. Returns the fallback (default) if the variable
    // does not exist (null) or is empty/only spaces (isBlank).
    private static String env(String key, String fallback) {
        // System.getenv reads a variable from the operating system's environment.
        String value = System.getenv(key);
        // Ternary operator: condition ? valueIfTrue : valueIfFalse
        return value == null || value.isBlank() ? fallback : value;
    }

    // Opens and returns a NEW connection to MySQL every time it is called.
    // The caller is responsible for closing it (the services use try-with-resources for that).
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
