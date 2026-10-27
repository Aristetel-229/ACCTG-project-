// Package for "services": classes that talk to the database and hold the business logic (no GUI code).
package com.tala.service;

// Our own helper that opens database connections.
import com.tala.db.DB;
// The small class that holds one account (id, code, name).
import com.tala.model.AccountOption;

// BigDecimal = exact money values.
import java.math.BigDecimal;
// Connection = open link to MySQL.
import java.sql.Connection;
// java.sql.Date = a date-only value that JDBC can store in a MySQL DATE column.
import java.sql.Date;
// PreparedStatement = a SQL command with ? placeholders (safe from SQL injection).
import java.sql.PreparedStatement;
// ResultSet = the rows returned by a SELECT query.
import java.sql.ResultSet;
// SQLException = database error type.
import java.sql.SQLException;
// Statement = used here only for the constant RETURN_GENERATED_KEYS.
import java.sql.Statement;
// ArrayList / List = a growable list of items.
import java.util.ArrayList;
import java.util.List;

/** Database access for journal entries. */
public final class JournalService {
    // Static-only class: no one can create an instance.
    private JournalService() {}

    // Loads every ACTIVE account so it can be shown in the debit/credit dropdowns.
    public static List<AccountOption> loadActiveAccounts() throws SQLException {
        // Empty list that we will fill with the rows from the database.
        List<AccountOption> options = new ArrayList<>();
        // try-with-resources: everything declared inside the ( ) is closed automatically
        // at the end (connection, statement, result set), even if an error occurs.
        try (Connection c = DB.getConnection();
             // Prepare the SQL: only active accounts, sorted by account code (1010, 1020, ...).
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, account_code, account_name FROM accounts WHERE is_active=TRUE ORDER BY account_code");
             // Run the query and get the resulting rows.
             ResultSet rs = ps.executeQuery()) {
            // rs.next() moves to the next row; returns false when there are no more rows.
            while (rs.next()) {
                // Build an AccountOption from the three columns of the current row and add it to the list.
                options.add(new AccountOption(rs.getLong("id"),
                        rs.getString("account_code"), rs.getString("account_name")));
            }
        }
        // Hand the finished list back to the caller (the Journal Entry page).
        return options;
    }

    /** Posts a simple two-line entry in one transaction and returns the new entry ID. */
    public static long postTwoLineEntry(Date entryDate, String reference, String description,
                                        AccountOption debit, AccountOption credit, BigDecimal amount) throws SQLException {
        // SQL 1: creates the journal ENTRY header (date, reference number, description). Status starts as POSTED.
        // The three ? are filled in later with ps.setXxx(1..3); 'POSTED' is written directly in the SQL.
        String insertEntry = "INSERT INTO journal_entries(entry_date, reference_no, description, status) VALUES (?, ?, ?, 'POSTED')";
        // SQL 2: creates one journal LINE (one debit or one credit) that belongs to an entry.
        String insertLine = "INSERT INTO journal_entry_lines(journal_entry_id, account_id, debit, credit, line_description) VALUES (?, ?, ?, ?, ?)";
        // Open a connection; it closes automatically when the try block ends.
        try (Connection c = DB.getConnection()) {
            // TRANSACTION START: turn off auto-commit so nothing is saved until we call c.commit().
            // This makes the entry "all or nothing" - never a debit without its credit.
            c.setAutoCommit(false);
            try {
                // Will hold the auto-generated ID of the new journal entry.
                long entryId;
                // RETURN_GENERATED_KEYS asks MySQL to give back the AUTO_INCREMENT id it just created.
                try (PreparedStatement ps = c.prepareStatement(insertEntry, Statement.RETURN_GENERATED_KEYS)) {
                    // Fill the ? placeholders in order (1 = first ?, 2 = second ?, ...).
                    ps.setDate(1, entryDate);
                    ps.setString(2, reference);
                    ps.setString(3, description);
                    // Run the INSERT.
                    ps.executeUpdate();
                    // Read the generated id back.
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        // If MySQL returned no key, something is wrong -> throw (this triggers the rollback below).
                        if (!keys.next()) throw new SQLException("Could not obtain journal entry ID.");
                        // Column 1 of the returned row is the new entry's id.
                        entryId = keys.getLong(1);
                    }
                }
                // Prepare the line INSERT once and run it twice (once for debit, once for credit).
                try (PreparedStatement ps = c.prepareStatement(insertLine)) {
                    // LINE 1 = the DEBIT side: entry id, debit account, debit amount, credit = 0, description.
                    ps.setLong(1, entryId); ps.setLong(2, debit.id);
                    ps.setBigDecimal(3, amount); ps.setBigDecimal(4, BigDecimal.ZERO);
                    ps.setString(5, description); ps.executeUpdate();

                    // LINE 2 = the CREDIT side: same entry id, credit account, debit = 0, credit amount.
                    // Because both lines use the same "amount", debits always equal credits.
                    ps.setLong(1, entryId); ps.setLong(2, credit.id);
                    ps.setBigDecimal(3, BigDecimal.ZERO); ps.setBigDecimal(4, amount);
                    ps.setString(5, description); ps.executeUpdate();
                }
                // Everything worked: save the entry header and both lines permanently.
                c.commit();
                // Return the new entry's id (the UI shows "Posted journal entry #id").
                return entryId;
            } catch (SQLException | RuntimeException ex) {
                // Something failed: undo everything done since setAutoCommit(false).
                c.rollback();
                // Re-throw so the page can show an error message to the user.
                throw ex;
            } finally {
                // Always restore normal auto-commit mode, whether we succeeded or failed.
                c.setAutoCommit(true);
            }
        }
    }
}
