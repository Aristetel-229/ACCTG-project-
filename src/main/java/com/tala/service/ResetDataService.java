// Package for services (database + business logic, no GUI).
package com.tala.service;

// Our connection helper.
import com.tala.db.DB;

// JDBC classes used below.
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Clears everything the user has entered (journal entries and adjustments). The Chart of Accounts is kept. */
public final class ResetDataService {
    // Static-only class: never instantiated.
    private ResetDataService() {}

    // Deletes ALL journal entries and their lines in one transaction.
    // Adjusting entries are stored as normal journal entries (reference "ADJ-..."), so they are deleted too.
    public static void resetInputs() throws SQLException {
        // Open a connection (auto-closed at the end).
        try (Connection c = DB.getConnection()) {
            // Remember the current auto-commit mode so we can put it back afterwards.
            boolean original = c.getAutoCommit();
            // Start a transaction: nothing is permanent until commit().
            c.setAutoCommit(false);
            // A Statement is used for plain SQL with no ? placeholders.
            try (Statement st = c.createStatement()) {
                // Lines FIRST: each line has a foreign key pointing to its entry,
                // so the child rows must be deleted before the parent rows.
                st.executeUpdate("DELETE FROM journal_entry_lines");
                // Then the entry headers.
                st.executeUpdate("DELETE FROM journal_entries");
                // Both deletes succeeded: make the change permanent.
                c.commit();
            } catch (SQLException | RuntimeException ex) {
                // Any failure: undo both deletes so nothing is half-deleted.
                c.rollback();
                // Tell the caller (the Reset Data button) that it failed.
                throw ex;
            } finally {
                // Restore the original auto-commit mode.
                c.setAutoCommit(original);
            }
        }
    }
}
