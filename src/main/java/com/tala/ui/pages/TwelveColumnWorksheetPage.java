package com.tala.ui.pages;

// Model for an account choice in a drop-down, and the services that load/post data and compute the worksheet.
import com.tala.model.AccountOption;
import com.tala.service.JournalService;
import com.tala.service.WorksheetService;
// Base page, main window, theme, and custom table/button components.
import com.tala.ui.BasePage;
import com.tala.ui.MainFrame;
import com.tala.ui.Theme;
import com.tala.ui.components.RoundedButton;
import com.tala.ui.components.Tables;
// Money helper for formatting and parsing peso amounts.
import com.tala.util.Money;

// Swing, AWT, number, date, SQL and collection classes used below.
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * One accounting worksheet containing the six debit/credit column pairs (12 value columns).
 * The pairs are: Unadjusted Trial Balance, Adjustments, Adjusted Trial Balance, Income Statement,
 * Owner's Equity, and Balance Sheet. The page also lets the user post adjusting entries
 * (entries made at period end, e.g. depreciation or accrued expenses).
 */
public class TwelveColumnWorksheetPage extends BasePage {
    // Constructor: passes the main window to BasePage.
    public TwelveColumnWorksheetPage(MainFrame frame) { super(frame); }

    // Page title.
    @Override protected String title() { return "12 Columnar"; }
    // Hint describing the six column pairs.
    @Override protected String hint() { return "Unadjusted trial balance, adjustments, adjusted trial balance, income statement, owner's equity, and balance sheet."; }

    // Builds the worksheet page.
    @Override protected JComponent build() {
        // Main page panel.
        JPanel p = pagePanel();
        // Button to record an adjusting entry.
        JButton adjustment = new RoundedButton("Add Adjustment");
        // Opens the adjustment form when clicked.
        adjustment.addActionListener(e -> showAdjustmentDialog());
        // Refresh button reloads the worksheet.
        JButton refresh = new RoundedButton("Refresh", RoundedButton.Kind.SECONDARY);
        // On click, rebuild the page.
        refresh.addActionListener(e -> show());
        // Place both buttons in a bar at the top.
        p.add(actionBar(adjustment, refresh), BorderLayout.NORTH);

        // Only accounts that actually have posted entries are listed, so the sheet starts empty.
        // Holds all the computed worksheet numbers (lines, totals, net income, differences).
        WorksheetService.Result result;
        try {
            // Load the accounts with balances from the database and compute all 12 columns.
            result = WorksheetService.compute(WorksheetService.loadAccounts());
            // Show that the database is working.
            frame.setStatus(" Connected to MySQL");
        // On a database problem...
        } catch (SQLException ex) {
            // ...update the status bar and show an error pop-up...
            frame.setStatus(" Database error");
            error("Unable to load the worksheet: " + ex.getMessage());
            // ...and use an empty worksheet so the page still displays.
            result = WorksheetService.compute(java.util.List.of());
        }

        // Table model with 14 columns: Code, Account, then 6 debit/credit pairs (12 value columns).
        DefaultTableModel model = newTableModel(new String[]{"Code", "Account",
                "Unadjusted Dr", "Unadjusted Cr", "Adjustments Dr", "Adjustments Cr",
                "Adjusted Dr", "Adjusted Cr", "Income Stmt Dr", "Income Stmt Cr",
                "Owner's Equity Dr", "Owner's Equity Cr", "Balance Sheet Dr", "Balance Sheet Cr"});

        // Only fill rows if there is data (at least one posted entry).
        if (!result.isEmpty()) {
            // One row per account, showing its amounts in each column pair; zeros are left blank.
            for (WorksheetService.Line line : result.lines) {
                model.addRow(row(line.code, line.name, line.v, true));
            }
            // Net income (credit side of the income statement is bigger) or net loss, balancing the income statement columns.
            model.addRow(row("", result.income.signum() >= 0 ? "NET INCOME" : "NET LOSS", result.netIncomeRow, true));
            // Ending owner's equity row: capital + net income - drawings.
            model.addRow(row("", "ENDING OWNER'S EQUITY", result.endingEquityRow, true));
            // Grand total of every column; zeros are shown here.
            model.addRow(row("", "GRAND TOTAL", result.totals, false));

            // Debit minus credit of each column pair. Every one must read 0.00.
            // New row with 14 cells, first filled with empty text.
            Object[] diffRow = new Object[14];
            Arrays.fill(diffRow, "");
            // Label in the Account column.
            diffRow[1] = "BALANCE DIFFERENCE";
            // For each pair, put the absolute difference under the Debit column of that pair.
            for (int pair = 0; pair < WorksheetService.PAIRS; pair++) {
                diffRow[2 + pair * 2] = Money.currency(result.differences[pair].abs());
            }
            // Add the check row to the table.
            model.addRow(diffRow);
        }

        // Create the styled table.
        JTable table = Tables.create(model);
        // Turn off auto-resize so the wide table scrolls horizontally instead of squeezing columns.
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        // Preferred width in pixels for each of the 14 columns.
        int[] widths = {65, 200, 120, 120, 125, 125, 115, 115, 125, 125, 135, 135, 130, 130};
        // Apply each width to its column.
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        // Tooltip on the header explaining the six column pairs.
        table.getTableHeader().setToolTipText("Column pairs: Unadjusted Trial Balance | Adjustments | Adjusted Trial Balance | Income Statement | Owner's Equity | Balance Sheet");
        // Scroll pane around the table.
        JScrollPane scroll = Tables.scroll(table);
        // Show a horizontal scroll bar only when needed.
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        // Put the table in the center.
        p.add(scroll, BorderLayout.CENTER);

        // The largest debit/credit difference among the six pairs.
        BigDecimal maxDifference = result.maxDifference();
        // Books are balanced when the difference is exactly zero (total debits = total credits).
        boolean balanced = maxDifference.signum() == 0;
        // Summary message: empty-state text, or net income/loss plus the balance check.
        String text = result.isEmpty()
                ? "No posted entries yet. Post a journal entry and its accounts will appear here."
                : (result.income.signum() >= 0 ? "Net income: " : "Net loss: ") + Money.currency(result.income.abs())
                  + "     |     Balance difference: " + Money.currency(maxDifference)
                  + (balanced ? "  (balanced)" : "  (NOT balanced - review entries)");
        // Label that shows the summary text under the table.
        JLabel status = new JLabel(text);
        // Padding and bold font.
        status.setBorder(new EmptyBorder(8, 4, 0, 4)); status.setFont(Theme.BOLD);
        // Green if balanced, red if not.
        status.setForeground(balanced ? Theme.GREEN_DARK : Theme.DANGER);
        // Place at the bottom of the page.
        p.add(status, BorderLayout.SOUTH);
        // Return the finished page.
        return p;
    }

    /** Builds one table row; zero amounts are left blank on account lines, but shown on total rows. */
    private Object[] row(String code, String name, BigDecimal[] values, boolean blankZeros) {
        // A row of 14 cells: code, name, and 12 amount cells.
        Object[] row = new Object[14];
        // First two cells are the account code and name.
        row[0] = code; row[1] = name;
        // Fill the 12 amount cells (starting at index 2).
        for (int j = 0; j < values.length; j++) {
            // Blank if the amount is zero and blanking is allowed; otherwise format as pesos.
            row[j + 2] = (blankZeros && values[j].signum() == 0) ? "" : Money.currency(values[j]);
        }
        // Return the finished row.
        return row;
    }

    // Shows a form where the user records an adjusting entry (one debit account, one credit account, one amount).
    private void showAdjustmentDialog() {
        // List of accounts that can be chosen in the drop-downs.
        final List<AccountOption> accounts;
        // Load active accounts from the database; if that fails, show an error and stop.
        try { accounts=JournalService.loadActiveAccounts(); }
        catch(SQLException ex) { error("Could not load accounts: "+ex.getMessage()); return; }
        // Form panel with a grid of 2 columns (label, field) and 8 pixel gaps.
        JPanel form=new JPanel(new GridLayout(0,2,8,8));
        // Date field, prefilled with today's date.
        JTextField date=new JTextField(LocalDate.now().toString());
        // Reference field, prefilled with "ADJ-" plus a number from the clock, so adjusting entries are easy to identify.
        JTextField reference=new JTextField("ADJ-"+(System.currentTimeMillis()%1000000));
        // Description text field and two drop-downs: the account to debit and the account to credit.
        JTextField description=new JTextField(); JComboBox<AccountOption> debit=new JComboBox<>(), credit=new JComboBox<>();
        // Put every active account in both drop-downs.
        for(AccountOption a:accounts){debit.addItem(a);credit.addItem(a);}
        // Amount field (the amount is used for both the debit and the credit so the entry balances).
        JTextField amount=new JTextField();
        // Add each label and its field to the form, in order.
        form.add(new JLabel("Date (YYYY-MM-DD)")); form.add(date);
        form.add(new JLabel("Reference")); form.add(reference);
        form.add(new JLabel("Description")); form.add(description);
        form.add(new JLabel("Debit account")); form.add(debit);
        form.add(new JLabel("Credit account")); form.add(credit);
        form.add(new JLabel("Amount (₱)")); form.add(amount);
        // Show the form in an OK/Cancel dialog; the button clicked is stored in 'result'.
        int result=JOptionPane.showConfirmDialog(frame,form,"Post an adjusting entry",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        // If the user did not click OK, do nothing.
        if(result!=JOptionPane.OK_OPTION)return;
        // Validation 1: description and reference must be filled in and both accounts selected.
        if(description.getText().isBlank()||reference.getText().isBlank()||debit.getSelectedItem()==null||credit.getSelectedItem()==null){error("Complete all adjustment fields.");return;}
        // Validation 2: reference must start with ADJ- so the entry is shown in the Adjustments columns of the worksheet.
        if(!reference.getText().trim().toUpperCase().startsWith("ADJ-")){error("Adjustment reference must start with ADJ- so it appears in the adjustment columns.");return;}
        // Validation 3: an entry cannot debit and credit the same account.
        if(((AccountOption)debit.getSelectedItem()).id==((AccountOption)credit.getSelectedItem()).id){error("Choose different debit and credit accounts.");return;}
        // Try to read the inputs and save the entry; any bad input ends up in the catch block.
        try {
            // Convert the text date to a SQL date and the text amount to a BigDecimal.
            Date d=Date.valueOf(LocalDate.parse(date.getText().trim())); BigDecimal n=Money.parse(amount.getText().trim());
            // The amount must be greater than zero; otherwise jump to the catch block.
            if(n.signum()<=0)throw new IllegalArgumentException();
            // Post a journal entry with two lines: debit one account and credit the other by the same amount.
            JournalService.postTwoLineEntry(d,reference.getText().trim(),description.getText().trim(),(AccountOption)debit.getSelectedItem(),(AccountOption)credit.getSelectedItem(),n);
            // Confirm in the status bar and reload the worksheet to include the new adjustment.
            frame.setStatus(" Posted adjusting entry"); show();
        // Covers a bad date, a bad amount, or a duplicate reference number (reference_no must be unique).
        } catch(Exception ex){error("Could not post adjustment. Check the date, amount, and unique reference number.");}
    }
}
