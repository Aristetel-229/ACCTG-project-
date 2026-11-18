package tala.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseSetup
 * ------------------------------------------------------------
 * Runs once when the program starts:
 *   1. creates the tala_accounting database if it does not exist
 *   2. creates the four tables if they do not exist
 *   3. fills the Chart of Accounts if it is still empty
 *
 * The SQL here is the same as database/tala_accounting.sql,
 * so you can read either one to understand the tables.
 */
public class DatabaseSetup {

    private static final String CREATE_ACCOUNTS =
            "CREATE TABLE IF NOT EXISTS accounts ("
            + " account_id INT AUTO_INCREMENT PRIMARY KEY,"
            + " account_code INT NOT NULL UNIQUE,"
            + " account_name VARCHAR(100) NOT NULL UNIQUE,"
            + " account_type ENUM('Asset','Liability','Equity','Revenue','Expense') NOT NULL,"
            + " normal_balance ENUM('Debit','Credit') NOT NULL,"
            + " created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

    private static final String CREATE_JOURNAL_ENTRIES =
            "CREATE TABLE IF NOT EXISTS journal_entries ("
            + " entry_id INT AUTO_INCREMENT PRIMARY KEY,"
            + " entry_date DATE NOT NULL,"
            + " reference_no VARCHAR(20) NOT NULL UNIQUE,"
            + " description VARCHAR(255) NOT NULL,"
            + " entry_type ENUM('Regular','Adjusting') NOT NULL DEFAULT 'Regular',"
            + " created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

    private static final String CREATE_JOURNAL_LINES =
            "CREATE TABLE IF NOT EXISTS journal_lines ("
            + " line_id INT AUTO_INCREMENT PRIMARY KEY,"
            + " entry_id INT NOT NULL,"
            + " account_id INT NOT NULL,"
            + " debit DECIMAL(15,2) NOT NULL DEFAULT 0.00,"
            + " credit DECIMAL(15,2) NOT NULL DEFAULT 0.00,"
            + " CONSTRAINT fk_line_entry FOREIGN KEY (entry_id) REFERENCES journal_entries(entry_id) ON DELETE CASCADE,"
            + " CONSTRAINT fk_line_account FOREIGN KEY (account_id) REFERENCES accounts(account_id),"
            + " CONSTRAINT chk_line_amount CHECK (debit >= 0 AND credit >= 0"
            + "   AND (debit = 0 OR credit = 0) AND (debit > 0 OR credit > 0)))";

    private static final String CREATE_APP_SETTINGS =
            "CREATE TABLE IF NOT EXISTS app_settings ("
            + " setting_key VARCHAR(50) PRIMARY KEY,"
            + " setting_value VARCHAR(255) NOT NULL)";

    /**
     * The starting Chart of Accounts: { code, name, type, normal balance }.
     * Contra accounts (Allowance, Accumulated Depreciation) are Assets with a Credit balance.
     */
    private static final Object[][] DEFAULT_ACCOUNTS = {
        {1010, "Cash", "Asset", "Debit"},
        {1100, "Accounts Receivable", "Asset", "Debit"},
        {1110, "Allowance for Doubtful Accounts", "Asset", "Credit"},
        {1120, "Notes Receivable", "Asset", "Debit"},
        {1130, "Interest Receivable", "Asset", "Debit"},
        {1200, "Inventory", "Asset", "Debit"},
        {1300, "Supplies", "Asset", "Debit"},
        {1310, "Prepaid Insurance", "Asset", "Debit"},
        {1320, "Prepaid Rent", "Asset", "Debit"},
        {1330, "Advances to Employees", "Asset", "Debit"},
        {1500, "Office Equipment", "Asset", "Debit"},
        {1510, "Furniture", "Asset", "Debit"},
        {1520, "Vehicles", "Asset", "Debit"},
        {1590, "Accumulated Depreciation", "Asset", "Credit"},
        {1600, "Land", "Asset", "Debit"},
        {1610, "Building", "Asset", "Debit"},
        {2010, "Accounts Payable", "Liability", "Credit"},
        {2020, "Salaries Payable", "Liability", "Credit"},
        {2030, "Taxes Payable", "Liability", "Credit"},
        {2040, "Unearned Revenue", "Liability", "Credit"},
        {2100, "Notes Payable", "Liability", "Credit"},
        {3010, "Owner's Capital", "Equity", "Credit"},
        {3020, "Owner's Drawing", "Equity", "Debit"},
        {4010, "Service Revenue", "Revenue", "Credit"},
        {4020, "Interest Income", "Revenue", "Credit"},
        {5010, "Salaries Expense", "Expense", "Debit"},
        {5020, "Rent Expense", "Expense", "Debit"},
        {5030, "Utilities Expense", "Expense", "Debit"},
        {5040, "Supplies Expense", "Expense", "Debit"},
        {5050, "Insurance Expense", "Expense", "Debit"},
        {5060, "Depreciation Expense", "Expense", "Debit"},
        {5070, "Bad Debts Expense", "Expense", "Debit"}
    };

    /**
     * Prepares everything. Throws SQLException if MySQL cannot be reached,
     * so Main can show a helpful message.
     */
    public static void prepareDatabase() throws SQLException {
        createDatabaseIfMissing();
        createTablesIfMissing();
        addDefaultAccountsIfEmpty();
    }

    /** Step 1: CREATE DATABASE IF NOT EXISTS tala_accounting */
    private static void createDatabaseIfMissing() throws SQLException {
        try (Connection conn = DBConnection.getServerConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DBConnection.DATABASE_NAME);
        }
    }

    /** Step 2: create the tables (nothing happens if they already exist) */
    private static void createTablesIfMissing() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate(CREATE_ACCOUNTS);
            st.executeUpdate(CREATE_JOURNAL_ENTRIES);
            st.executeUpdate(CREATE_JOURNAL_LINES);
            st.executeUpdate(CREATE_APP_SETTINGS);
        }
    }

    /** Step 3: insert the starting accounts, but only into an empty table */
    private static void addDefaultAccountsIfEmpty() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {

            // How many accounts are there already?
            int count = 0;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM accounts")) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
            if (count > 0) {
                return; // the user already has a chart of accounts; leave it alone
            }

            String sql = "INSERT INTO accounts (account_code, account_name, account_type, normal_balance)"
                       + " VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Object[] row : DEFAULT_ACCOUNTS) {
                    ps.setInt(1, (Integer) row[0]);
                    ps.setString(2, (String) row[1]);
                    ps.setString(3, (String) row[2]);
                    ps.setString(4, (String) row[3]);
                    ps.addBatch();          // collect all inserts...
                }
                ps.executeBatch();          // ...then send them in one go
            }
        }
    }
}
