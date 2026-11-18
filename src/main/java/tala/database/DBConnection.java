package tala.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection
 * ------------------------------------------------------------
 * The ONE place where the MySQL connection settings are kept.
 * There is no db.properties file: the settings are written here
 * in Java, so you only ever edit this class.
 *
 * If your MySQL uses a different username, password or port,
 * change the constants below and run the program again.
 */
public class DBConnection {

    // ===================== CHANGE THESE IF NEEDED =====================
    public static final String HOST          = "localhost";
    public static final int    PORT          = 3306;
    public static final String DATABASE_NAME = "tala_accounting";
    public static final String USERNAME      = "root";
    public static final String PASSWORD      = "Sealthiel_2006";   // XAMPP's default root password is empty
    // ==================================================================

    // The class name of the MySQL JDBC driver (inside mysql-connector-j.jar)
    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    // Extra options added to the end of the connection URL
    private static final String URL_OPTIONS =
            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Manila";

    /**
     * Opens a connection to the tala_accounting database.
     * Always close it after use (we use try-with-resources for that).
     */
    public static Connection getConnection() throws SQLException {
        loadDriver();
        String url = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE_NAME + URL_OPTIONS;
        return DriverManager.getConnection(url, USERNAME, PASSWORD);
    }

    /**
     * Opens a connection to the MySQL server itself (no database selected).
     * Used only once at start-up to create the database if it is missing.
     */
    public static Connection getServerConnection() throws SQLException {
        loadDriver();
        String url = "jdbc:mysql://" + HOST + ":" + PORT + "/" + URL_OPTIONS;
        return DriverManager.getConnection(url, USERNAME, PASSWORD);
    }

    /**
     * Makes sure the MySQL driver class can be found.
     * If the connector .jar was not added to the project, we explain it clearly.
     */
    private static void loadDriver() throws SQLException {
        try {
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "MySQL Connector/J was not found.\n"
                    + "Put mysql-connector-j-<version>.jar inside the lib folder\n"
                    + "(or open the project as a Maven project so it downloads it).", e);
        }
    }

    /** Quick yes/no check used by the "MySQL connected" label in the top bar. */
    public static boolean isConnected() {
        try (Connection conn = getConnection()) {
            return conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
}
