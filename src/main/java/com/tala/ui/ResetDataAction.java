package com.tala.ui;

// Service that actually deletes the journal data from the database.
import com.tala.service.ResetDataService;

// Swing classes for the pop-up dialogs.
import javax.swing.*;
// Exception type thrown when a database operation fails.
import java.sql.SQLException;

/**
 * Two-step "Reset Data" confirmation flow.
 * Because reset permanently deletes all journal entries and adjusting entries (the Chart of
 * Accounts is kept), the user must confirm twice: a Yes/No dialog, then typing the word RESET.
 */
public final class ResetDataAction {
    // Private constructor: only static methods are used, so no objects are needed.
    private ResetDataAction() {}

    // Runs the whole flow; 'afterReset' is code to run when done (e.g. reload the dashboard).
    public static void confirmAndRun(MainFrame frame, Runnable afterReset) {
        // Step 1: warning dialog with Yes/No; the result is stored in 'choice'.
        int choice = JOptionPane.showConfirmDialog(frame,
                "This will permanently delete ALL journal entries and adjustments.\n"
                        + "Your Chart of Accounts will be kept.\n\nDo you want to continue?",
                "Confirm Reset", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        // If the user did not click Yes, stop here and change nothing.
        if (choice != JOptionPane.YES_OPTION) return;

        // Step 2: ask the user to type RESET, as an extra safety check against accidents.
        String confirmation = JOptionPane.showInputDialog(frame,
                "This cannot be undone. Type RESET to confirm:", "Final Confirmation", JOptionPane.WARNING_MESSAGE);
        // Cancel (null) or any text other than RESET (ignoring spaces) cancels the reset.
        if (confirmation == null || !"RESET".equals(confirmation.trim())) {
            // Tell the user that nothing was deleted.
            JOptionPane.showMessageDialog(frame, "Reset cancelled. No data was changed.",
                    "Reset Cancelled", JOptionPane.INFORMATION_MESSAGE);
            // Leave the method without deleting anything.
            return;
        }
        // Both confirmations passed, so try to delete the data.
        try {
            // Delete all journal entries and lines (the books go back to empty).
            ResetDataService.resetInputs();
            // Show a message in the status bar at the bottom of the window.
            frame.setStatus(" TALA data reset");
            // Tell the user the reset succeeded.
            JOptionPane.showMessageDialog(frame, "All entries have been cleared.",
                    "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
            // Run the follow-up action, usually refreshing the page so it shows empty data.
            afterReset.run();
        // If the database fails, show the error instead of crashing.
        } catch (SQLException ex) {
            // The message says nothing was changed, since the delete failed.
            JOptionPane.showMessageDialog(frame, "Could not reset data. Nothing was changed.\n\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
