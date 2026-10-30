package com.tala.ui.pages;

// Model class that represents one account choice (id + name) shown in the drop-down lists.
import com.tala.model.AccountOption;
// Service that talks to the database to load accounts, post entries and delete entries.
import com.tala.service.JournalService;
// Base class that gives every page its title, hint text and common layout helpers.
import com.tala.ui.BasePage;
// Reusable text-field and form helpers, rounded button and table helpers.
import com.tala.ui.components.Fields;
import com.tala.ui.components.RoundedButton;
import com.tala.ui.components.Tables;
// The main application window, the colour/font theme and the money helper.
import com.tala.ui.MainFrame;
import com.tala.ui.Theme;
import com.tala.util.Money;

// Swing GUI classes (buttons, panels, tables, dialogs) and the border helper.
import javax.swing.*;
import javax.swing.border.EmptyBorder;
// Table model that stores the rows displayed in a JTable.
import javax.swing.table.DefaultTableModel;
// AWT classes for layouts, colours and fonts.
import java.awt.*;
// BigDecimal is used for money so there are no rounding errors (unlike double).
import java.math.BigDecimal;
// SQL date type that the database layer expects.
import java.sql.Date;
// Modern Java date type, used to read today's date and parse typed dates.
import java.time.LocalDate;

/**
 * Journal Entry page: the data-entry screen where the user records a transaction.
 * The user picks one account to DEBIT and one to CREDIT plus an amount, and the entry is
 * validated and posted to the database (a two-line journal entry, so debits always equal credits).
 * Below the form it lists posted entries and lets the user delete a single selected entry.
 */
public class JournalEntryPage extends BasePage {
    // Constructor: passes the main window to the parent class so the page can update the status bar.
    public JournalEntryPage(MainFrame frame) { super(frame); }

    // The page heading shown at the top of the screen.
    @Override protected String title() { return "Journal Entry"; }

    // Small help text shown under the heading to guide the user.
    @Override protected String hint() { return "Record a transaction by choosing what is debited and what is credited. Made a mistake? Select the entry below and delete just that one."; }

    // Builds the whole screen (form on top, posted-entries list below).
    @Override protected JComponent build() {
        // Outer panel provided by BasePage that holds everything on this page.
        JPanel p = pagePanel();
        // The form uses GridBagLayout so labels and fields line up in rows.
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        // Padding inside the form so the fields do not touch the edges.
        form.setBorder(new EmptyBorder(14, 14, 14, 14));
        // Layout rules reused for every field: spacing, stretch horizontally, full weight.
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        // Date field, pre-filled with today's date in YYYY-MM-DD format.
        JTextField date = Fields.text(LocalDate.now().toString());
        // Reference number field, pre-filled with a generated "JE-" number based on the clock so it is usually unique.
        JTextField reference = Fields.text("JE-" + System.currentTimeMillis() % 1000000);
        // Free-text description of the transaction (empty at first).
        JTextField description = Fields.text();
        // Drop-down lists for choosing the account to debit and the account to credit.
        JComboBox<AccountOption> debitAccount = new JComboBox<>();
        JComboBox<AccountOption> creditAccount = new JComboBox<>();
        // Amount field (empty at first).
        JTextField amount = Fields.text();

        // Loading accounts touches the database, so it may throw an SQLException.
        try {
            // Put every active account from the chart of accounts into both drop-downs.
            for (AccountOption option : JournalService.loadActiveAccounts()) {
                debitAccount.addItem(option);
                creditAccount.addItem(option);
            }
        } catch (java.sql.SQLException ex) {
            // If the database fails, tell the user instead of crashing.
            JOptionPane.showMessageDialog(frame, "Could not load accounts: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        // Row counter so each field is placed one row below the previous one.
        int row = 0;
        // Add each labelled input to the form (label on the left, field on the right).
        addField(form, g, row++, "Transaction date (YYYY-MM-DD)", date);
        addField(form, g, row++, "Reference number", reference);
        addField(form, g, row++, "Description", description);
        addField(form, g, row++, "Debit account", debitAccount);
        addField(form, g, row++, "Credit account", creditAccount);
        addField(form, g, row++, "Amount (₱)", amount);

        // The button that validates the form and posts the journal entry.
        JButton post = new RoundedButton("Validate and Post Entry");
        // When clicked, read all the fields (trimmed of extra spaces) and send them to postJournal.
        post.addActionListener(e -> postJournal(date.getText().trim(), reference.getText().trim(),
                description.getText().trim(), (AccountOption) debitAccount.getSelectedItem(),
                (AccountOption) creditAccount.getSelectedItem(), amount.getText().trim()));
        // Place the button in the second column on the row after the last field.
        g.gridx = 1; g.gridy = row; g.weightx = 1;
        form.add(post, g);

        // Wrap the form in a scroll pane so it can scroll on small windows.
        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        // Top: the entry form. Bottom: posted entries, where a single one can be deleted.
        // Split pane stacks the form above the list and lets the user drag the divider.
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, formScroll, buildEntryList());
        split.setBorder(null);
        split.setOpaque(false);
        // The form gets 55% of any extra height; the divider is 8 pixels thick.
        split.setResizeWeight(0.55);
        split.setDividerSize(8);
        // Put the split pane in the middle of the page and return the finished page.
        p.add(split, BorderLayout.CENTER);
        return p;
    }

    /** Table of posted entries with a button that deletes only the selected entry. */
    private JComponent buildEntryList() {
        // SQL that lists each POSTED journal entry once: it joins the entry to its lines and accounts,
        // gathers the debited account names and credited account names, and totals the debit amount.
        String sql = """
            SELECT je.id, je.entry_date, je.reference_no, je.description,
                   GROUP_CONCAT(CASE WHEN jel.debit > 0 THEN a.account_name END SEPARATOR ', ') AS debit_account,
                   GROUP_CONCAT(CASE WHEN jel.credit > 0 THEN a.account_name END SEPARATOR ', ') AS credit_account,
                   SUM(jel.debit) AS amount
            FROM journal_entries je
            JOIN journal_entry_lines jel ON jel.journal_entry_id = je.id
            JOIN accounts a ON a.id = jel.account_id
            WHERE je.status = 'POSTED'
            GROUP BY je.id, je.entry_date, je.reference_no, je.description
            ORDER BY je.entry_date DESC, je.id DESC
            """;
        // Run the query and load the result into a table model with these column headings.
        DefaultTableModel model = readTable(sql,
                new String[]{"ID", "Date", "Reference", "Description", "Debit", "Credit", "Amount"});
        // Create a styled table from the model and allow only one row to be selected at a time.
        JTable table = Tables.create(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // The ID column stays in the model (so we know which entry to delete) but is hidden from view.
        table.removeColumn(table.getColumnModel().getColumn(0));

        // Red "danger" button for deleting the selected entry; it calls deleteSelected when clicked.
        JButton delete = new RoundedButton("Delete Selected Entry", RoundedButton.Kind.DANGER);
        delete.addActionListener(e -> deleteSelected(table, model));

        // Heading label above the list.
        JLabel title = new JLabel("Posted entries");
        title.setFont(Theme.BOLD);
        title.setForeground(Theme.GREEN_DARK);
        title.setBorder(new EmptyBorder(0, 2, 6, 0));

        // Panel that stacks heading (top), table (middle) and delete button (bottom).
        JPanel list = new JPanel(new BorderLayout(0, 6));
        list.setOpaque(false);
        list.add(title, BorderLayout.NORTH);
        list.add(Tables.scroll(table), BorderLayout.CENTER);
        list.add(actionBar(delete), BorderLayout.SOUTH);
        return list;
    }

    // Deletes the journal entry currently selected in the table (after asking for confirmation).
    private void deleteSelected(JTable table, DefaultTableModel model) {
        // Which row on screen is selected (-1 means none).
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            // Nothing selected: show a message and stop.
            error("Select the entry you want to delete first.");
            return;
        }
        // Convert the on-screen row to the model row (they differ if the table is sorted).
        int row = table.convertRowIndexToModel(viewRow);
        // Read the hidden ID column (column 0): this is the database id of the entry to delete.
        long id = ((Number) model.getValueAt(row, 0)).longValue();
        // Build a readable summary (reference - description (date, amount)) to show in the confirmation.
        String summary = model.getValueAt(row, 2) + " - " + model.getValueAt(row, 3)
                + " (" + model.getValueAt(row, 1) + ", " + Money.currency(new BigDecimal(model.getValueAt(row, 6).toString())) + ")";
        // Ask Yes/No so an entry is never deleted by accident.
        int choice = JOptionPane.showConfirmDialog(frame,
                "Delete this entry?\n\n" + summary + "\n\nOnly this entry is removed. Everything else stays.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        // If the user did not click Yes, do nothing.
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            // Ask the service to delete the entry; it returns false if the entry was already gone.
            boolean removed = JournalService.deleteEntry(id);
            // Show the result in the status bar.
            frame.setStatus(removed ? " Deleted journal entry #" + id : " Entry was already deleted");
            // Reload the page so the list and form are refreshed.
            show();
        } catch (Exception ex) {
            // Any failure leaves the data unchanged; tell the user.
            error("Could not delete the entry. Nothing was changed.\n\n" + ex.getMessage());
        }
    }

    // Validates the form values and, if they are correct, posts a two-line journal entry.
    private void postJournal(String dateText, String reference, String description,
                             AccountOption debit, AccountOption credit, String amountText) {
        // Every field must be filled in and both accounts must be chosen.
        if (description.isBlank() || reference.isBlank() || debit == null || credit == null) {
            error("Complete all fields before posting.");
            return;
        }
        // Debiting and crediting the same account would be meaningless, so it is rejected.
        if (debit.id == credit.id) {
            error("Choose different debit and credit accounts.");
            return;
        }
        // Parsed values; "final" because each is assigned exactly once inside the try block.
        final Date entryDate;
        final BigDecimal amount;
        try {
            // Convert the typed text to a date (fails if the format is not YYYY-MM-DD).
            entryDate = Date.valueOf(LocalDate.parse(dateText));
            // Convert the typed text to a money value (fails on bad format or more than 2 decimals).
            amount = Money.parse(amountText);
            // Amount must be positive; zero or negative amounts are not valid transactions.
            if (amount.signum() <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
        } catch (Exception ex) {
            // Any parsing problem lands here with one friendly message.
            error("Enter a valid date (YYYY-MM-DD) and a positive amount with no more than two decimal places.");
            return;
        }

        try {
            // Save the entry: one line debits the debit account and one credits the credit account for the same amount.
            long entryId = JournalService.postTwoLineEntry(entryDate, reference, description, debit, credit, amount);
            // Tell the user it worked.
            JOptionPane.showMessageDialog(frame, "Journal entry posted successfully.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            // Show the new entry number in the status bar.
            frame.setStatus(" Posted journal entry #" + entryId);
            // Rebuild the page so the new entry appears in the list and the form resets.
            show();
        } catch (Exception ex) {
            // Database or rule failure: show the reason.
            error("Posting failed: " + ex.getMessage());
        }
    }
}
