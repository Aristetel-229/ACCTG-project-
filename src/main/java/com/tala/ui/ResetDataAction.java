// Package for the UI framework classes.
package com.tala.ui;

// The service that actually deletes the data.
import com.tala.service.ResetDataService;

import javax.swing.*;
import java.sql.SQLException;

/** Two-step "Reset Data" confirmation flow. */
public final class ResetDataAction {
    // Static-only class: never instantiated.
    private ResetDataAction() {}

    // Asks the user twice before deleting. "afterReset" is code to run once the reset is done
    // (the Dashboard passes this::show so it refreshes itself).
    public static void confirmAndRun(MainFrame frame, Runnable afterReset) {
        // CONFIRMATION 1: a Yes/No warning popup. "\n" = new line inside the message.
        int choice = JOptionPane.showConfirmDialog(frame,
                "This will permanently delete ALL journal entries and adjustments.\n"
                        + "Your Chart of Accounts will be kept.\n\nDo you want to continue?",
                "Confirm Reset", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        // Anything other than "Yes" (No, or closing the popup) = stop here, nothing deleted.
        if (choice != JOptionPane.YES_OPTION) return;

        // CONFIRMATION 2: the user must TYPE the word RESET. Returns null if they press Cancel.
        String confirmation = JOptionPane.showInputDialog(frame,
                "This cannot be undone. Type RESET to confirm:", "Final Confirmation", JOptionPane.WARNING_MESSAGE);
        // Cancel pressed, or the typed text (spaces trimmed) is not exactly "RESET" -> cancel the reset.
        if (confirmation == null || !"RESET".equals(confirmation.trim())) {
            JOptionPane.showMessageDialog(frame, "Reset cancelled. No data was changed.",
                    "Reset Cancelled", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            // Both confirmations passed: delete the data.
            ResetDataService.resetInputs();
            // Update the status bar.
            frame.setStatus(" TALA data reset");
            // Tell the user it worked.
            JOptionPane.showMessageDialog(frame, "All entries have been cleared.",
                    "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
            // Run the follow-up action (refresh the dashboard so the totals show zero).
            afterReset.run();
        } catch (SQLException ex) {
            // The service already rolled back, so nothing was deleted; explain that to the user.
            JOptionPane.showMessageDialog(frame, "Could not reset data. Nothing was changed.\n\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
