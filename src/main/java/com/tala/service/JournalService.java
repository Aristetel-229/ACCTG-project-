// This class lives in the service package.
package com.tala.service;

// Our database helper and the AccountOption model.
import com.tala.db.DB;
import com.tala.model.AccountOption;

// Imports: BigDecimal for exact money, JDBC types for SQL, and List classes.
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Database access for journal entries. */
public final class JournalService {
    // Private constructor: only static methods are used.
    private JournalService() {}

    // Loads all active accounts from the Chart of Accounts (used to fill the debit/credit combo boxes).
    public static List<AccountOption> loadActiveAccounts() throws SQLException {
        // List to fill with accounts.
        List<AccountOption> options = new ArrayList<>();
        // Open connection, prepare the SQL (only active accounts, sorted by code, no parameters) and run it.
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, account_code, account_name FROM accounts WHERE is_active=TRUE ORDER BY account_code");
             ResultSet rs = ps.executeQuery()) {
            // Go through each account row.
            while (rs.next()) {
                // Create an AccountOption from id, code and name and add it to the list.
                options.add(new AccountOption(rs.getLong("id"),
                        rs.getString("account_code"), rs.getString("account_name")));
            }
        }
        // Return the accounts.
        return options;
    }

    /** Posts a simple two-line entry in one transaction and returns the new entry ID. */
    public static long postTwoLineEntry(Date entryDate, String reference, String description,
                                        AccountOption debit, AccountOption credit, BigDecimal amount) throws SQLException {
        // SQL 1: insert the entry header (date, reference, description); status is fixed to 'POSTED'. Three ? placeholders.
        String insertEntry = "INSERT INTO journal_entries(entry_date, reference_no, description, status) VALUES (?, ?, ?, 'POSTED')";
        // SQL 2: insert one line (which entry, which account, debit amount, credit amount, note). Five ? placeholders.
        String insertLine = "INSERT INTO journal_entry_lines(journal_entry_id, account_id, debit, credit, line_description) VALUES (?, ?, ?, ?, ?)";
        // Open a connection (closed automatically).
        try (Connection c = DB.getConnection()) {
            // Start a transaction: header and both lines are saved together or not at all.
            c.setAutoCommit(false);
            try {
                // Will hold the auto-generated ID of the new entry.
                long entryId;
                // Prepare the header insert and ask MySQL to return the generated key.
                try (PreparedStatement ps = c.prepareStatement(insertEntry, Statement.RETURN_GENERATED_KEYS)) {
                    // Parameter 1: entry date.
                    ps.setDate(1, entryDate);
                    // Parameter 2: reference number.
                    ps.setString(2, reference);
                    // Parameter 3: description.
                    ps.setString(3, description);
                    // Run the INSERT.
                    ps.executeUpdate();
                    // Read the new auto-increment ID.
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        // If no key came back, something is wrong, so fail (this triggers the rollback).
                        if (!keys.next()) throw new SQLException("Could not obtain journal entry ID.");
                        // Save the new entry ID.
                        entryId = keys.getLong(1);
                    }
                }
                // Prepare the line insert once and reuse it for both lines.
                try (PreparedStatement ps = c.prepareStatement(insertLine)) {
                    // Debit line: entry ID, debited account, debit = amount, credit = 0.
                    ps.setLong(1, entryId); ps.setLong(2, debit.id);
                    ps.setBigDecimal(3, amount); ps.setBigDecimal(4, BigDecimal.ZERO);
                    // Line description, then run the insert.
                    ps.setString(5, description); ps.executeUpdate();

                    // Credit line: same entry, credited account, debit = 0, credit = amount (so total debits equal total credits).
                    ps.setLong(1, entryId); ps.setLong(2, credit.id);
                    ps.setBigDecimal(3, BigDecimal.ZERO); ps.setBigDecimal(4, amount);
                    // Line description, then run the insert.
                    ps.setString(5, description); ps.executeUpdate();
                }
                // Save everything permanently.
                c.commit();
                // Return the new entry ID to the caller.
                return entryId;
            // On any error, undo all inserts.
            } catch (SQLException | RuntimeException ex) {
                // Roll back the transaction.
                c.rollback();
                // Re-throw so the UI can show the error.
                throw ex;
            } finally {
                // Return the connection to normal auto-commit mode.
                c.setAutoCommit(true);
            }
        }
    }

    /**
     * Deletes ONE journal entry and its lines in a single transaction.
     * Nothing else is touched, so a mistake can be removed without resetting the database.
     * Returns false if the entry no longer exists.
     */
    public static boolean deleteEntry(long entryId) throws SQLException {
        // Open a connection (closed automatically).
        try (Connection c = DB.getConnection()) {
            // Start a transaction so both deletes succeed or fail together.
            c.setAutoCommit(false);
            try {
                // Number of entry rows deleted (0 means it did not exist).
                int removed;
                // Lines first, because they reference the entry (foreign key).
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM journal_entry_lines WHERE journal_entry_id = ?")) {
                    // Parameter 1: the entry whose lines should be deleted.
                    ps.setLong(1, entryId);
                    // Run the DELETE.
                    ps.executeUpdate();
                }
                // Then delete the entry header itself.
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM journal_entries WHERE id = ?")) {
                    // Parameter 1: the entry ID.
                    ps.setLong(1, entryId);
                    // executeUpdate returns how many rows were deleted.
                    removed = ps.executeUpdate();
                }
                // Save the deletions permanently.
                c.commit();
                // True if an entry was really removed.
                return removed > 0;
            // On any error, undo both deletes.
            } catch (SQLException | RuntimeException ex) {
                // Roll back the transaction.
                c.rollback();
                // Re-throw so the UI can show the error.
                throw ex;
            } finally {
                // Return the connection to normal auto-commit mode.
                c.setAutoCommit(true);
            }
        }
    }
}
