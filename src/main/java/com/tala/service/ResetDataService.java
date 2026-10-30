// This class lives in the service package (business logic + database work).
package com.tala.service;

// Our helper that opens database connections.
import com.tala.db.DB;

// JDBC types used below.
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Clears everything the user has entered (journal entries and adjustments). The Chart of Accounts is kept. */
public final class ResetDataService {
    // Private constructor: only static methods are used.
    private ResetDataService() {}

    // Deletes all journal data so the user can start fresh.
    public static void resetInputs() throws SQLException {
        // Open a connection; try-with-resources closes it automatically at the end.
        try (Connection c = DB.getConnection()) {
            // Remember the current auto-commit setting so we can restore it later.
            boolean original = c.getAutoCommit();
            // Turn off auto-commit: both deletes become one transaction (all succeed or none).
            c.setAutoCommit(false);
            // A Statement runs plain SQL that has no parameters.
            try (Statement st = c.createStatement()) {
                // Delete the debit/credit lines first because they reference journal_entries (foreign key).
                st.executeUpdate("DELETE FROM journal_entry_lines");
                // Then delete the journal entry headers.
                st.executeUpdate("DELETE FROM journal_entries");
                // Make both deletions permanent.
                c.commit();
            // If anything fails, undo everything done in this transaction.
            } catch (SQLException | RuntimeException ex) {
                // Roll back so the data is not left half-deleted.
                c.rollback();
                // Pass the error up so the screen can tell the user.
                throw ex;
            } finally {
                // Always restore the original auto-commit mode.
                c.setAutoCommit(original);
            }
        }
    }
}
