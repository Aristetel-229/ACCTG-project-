// Package for the five screens of the app.
package com.tala.ui.pages;

import com.tala.model.AccountOption;
import com.tala.service.JournalService;
import com.tala.ui.BasePage;
import com.tala.ui.components.Fields;
import com.tala.ui.components.RoundedButton;
import com.tala.ui.MainFrame;
import com.tala.ui.Theme;
import com.tala.util.Money;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

// The form where the user records a new transaction (one debit account + one credit account + one amount).
public class JournalEntryPage extends BasePage {
    // Constructor: hand the main window to BasePage.
    public JournalEntryPage(MainFrame frame) { super(frame); }

    // Page title and subtitle.
    @Override protected String title() { return "Journal Entry"; }

    @Override protected String hint() { return "Record a transaction by choosing what is debited and what is credited."; }

    // Builds the form.
    @Override protected JComponent build() {
        JPanel p = pagePanel();
        // The form panel uses GridBagLayout (a flexible grid for label + field rows), white background, 14px padding.
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(14, 14, 14, 14));
        // "g" = layout rules for each added component:
        //   insets = 6px spacing around each cell; fill HORIZONTAL = stretch to the cell width; weightx = share of extra width.
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        // The input controls. The date starts as today's date (format YYYY-MM-DD).
        JTextField date = Fields.text(LocalDate.now().toString());
        // The reference number starts as "JE-" plus the last 6 digits of the current time in milliseconds
        // (a quick, probably-unique number, because reference_no must be unique in the database).
        JTextField reference = Fields.text("JE-" + System.currentTimeMillis() % 1000000);
        // Empty text field for the description.
        JTextField description = Fields.text();
        // Two dropdowns that will hold AccountOption objects.
        JComboBox<AccountOption> debitAccount = new JComboBox<>();
        JComboBox<AccountOption> creditAccount = new JComboBox<>();
        // Empty field for the amount.
        JTextField amount = Fields.text();

        try {
            // Load all active accounts from the database and put each into BOTH dropdowns.
            for (AccountOption option : JournalService.loadActiveAccounts()) {
                debitAccount.addItem(option);
                creditAccount.addItem(option);
            }
        } catch (java.sql.SQLException ex) {
            // Could not read accounts: tell the user (the dropdowns stay empty).
            JOptionPane.showMessageDialog(frame, "Could not load accounts: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        // Add the six form rows. "row++" uses the current row number, then increases it by one for the next row.
        int row = 0;
        addField(form, g, row++, "Transaction date (YYYY-MM-DD)", date);
        addField(form, g, row++, "Reference number", reference);
        addField(form, g, row++, "Description", description);
        addField(form, g, row++, "Debit account", debitAccount);
        addField(form, g, row++, "Credit account", creditAccount);
        addField(form, g, row++, "Amount (₱)", amount);

        // The main action button.
        JButton post = new RoundedButton("Validate and Post Entry");
        // On click: read every field (trim() removes spaces at both ends) and pass the values to postJournal().
        // getSelectedItem() returns a plain Object, so it is cast to AccountOption.
        post.addActionListener(e -> postJournal(date.getText().trim(), reference.getText().trim(),
                description.getText().trim(), (AccountOption) debitAccount.getSelectedItem(),
                (AccountOption) creditAccount.getSelectedItem(), amount.getText().trim()));
        // Place the button in column 1 (under the input fields), on the row after the last field.
        g.gridx = 1; g.gridy = row; g.weightx = 1;
        form.add(post, g);

        // A tip box at the bottom of the page explaining what a journal entry needs.
        JTextArea help = new JTextArea("Tip: every entry has a debit and a credit of the same amount.\n" +
                "Example: Debit Rent Expense and Credit Cash on Hand.\n" +
                "The database transaction is rolled back if posting fails.");
        // Read-only, light green background, hint font and padding.
        help.setEditable(false);
        help.setBackground(Theme.GREEN_LIGHT);
        help.setFont(Theme.HINT);
        help.setForeground(Theme.TEXT);
        help.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Put the form in a scroll pane (in case the window is small), form in the center and tip at the bottom.
        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        p.add(formScroll, BorderLayout.CENTER);
        p.add(help, BorderLayout.SOUTH);
        return p;
    }

    // Validates the user's input and, if everything is OK, saves the entry to the database.
    private void postJournal(String dateText, String reference, String description,
                             AccountOption debit, AccountOption credit, String amountText) {
        // CHECK 1: required fields. isBlank() is true for empty text or only spaces. null = nothing selected in a dropdown.
        if (description.isBlank() || reference.isBlank() || debit == null || credit == null) {
            error("Complete all fields before posting.");
            return;
        }
        // CHECK 2: a transaction cannot debit and credit the same account.
        if (debit.id == credit.id) {
            error("Choose different debit and credit accounts.");
            return;
        }
        // "final" variables are assigned exactly once, inside the try block below.
        final Date entryDate;
        final BigDecimal amount;
        try {
            // CHECK 3: the date must be a real date in YYYY-MM-DD format (LocalDate.parse throws if it is not),
            // then convert it to the java.sql.Date that JDBC needs.
            entryDate = Date.valueOf(LocalDate.parse(dateText));
            // CHECK 4: the amount must be a number with at most 2 decimals (Money.parse throws otherwise).
            amount = Money.parse(amountText);
            // CHECK 5: the amount must be greater than zero (signum() is 0 for zero, -1 for negatives).
            if (amount.signum() <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
        } catch (Exception ex) {
            // Any of the date/amount errors above end up here with one clear message for the user.
            error("Enter a valid date (YYYY-MM-DD) and a positive amount with no more than two decimal places.");
            return;
        }

        try {
            // All checks passed: save the entry in one database transaction and get its new id.
            long entryId = JournalService.postTwoLineEntry(entryDate, reference, description, debit, credit, amount);
            // Tell the user it worked.
            JOptionPane.showMessageDialog(frame, "Journal entry posted successfully.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            // Show the new entry number in the status bar.
            frame.setStatus(" Posted journal entry #" + entryId);
            // Rebuild the page = clears the form and creates a fresh reference number.
            show();
        } catch (Exception ex) {
            // Database failure (e.g. duplicate reference number): show the reason. Nothing was saved (rolled back).
            error("Posting failed: " + ex.getMessage());
        }
    }
}
