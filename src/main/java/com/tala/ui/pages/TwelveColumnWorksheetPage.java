// Package for the five screens of the app.
package com.tala.ui.pages;

import com.tala.model.AccountOption;
import com.tala.service.JournalService;
import com.tala.ui.BasePage;
import com.tala.ui.MainFrame;
import com.tala.ui.Theme;
import com.tala.ui.components.RoundedButton;
import com.tala.ui.components.Tables;
import com.tala.util.Money;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** One accounting worksheet containing the five debit/credit column pairs. */
// The "12 Columnar" page. 12 columns = Code + Account + 5 pairs of (Debit, Credit):
//   1) Unadjusted Trial Balance  2) Adjustments  3) Adjusted Trial Balance  4) Income Statement  5) Balance Sheet
public class TwelveColumnWorksheetPage extends BasePage {
    // Constructor: hand the main window to BasePage.
    public TwelveColumnWorksheetPage(MainFrame frame) { super(frame); }

    // Page title and subtitle.
    @Override protected String title() { return "12 Columnar"; }
    @Override protected String hint() { return "Unadjusted trial balance, adjustments, adjusted trial balance, income statement, and balance sheet."; }

    // Builds the worksheet page.
    @Override protected JComponent build() {
        JPanel p = pagePanel();
        // "Add Adjustment" button opens a popup form for adjusting entries.
        JButton adjustment = new RoundedButton("Add Adjustment");
        adjustment.addActionListener(e -> showAdjustmentDialog());
        // "Refresh" button rebuilds the page.
        JButton refresh = new RoundedButton("Refresh", RoundedButton.Kind.SECONDARY);
        refresh.addActionListener(e -> show());
        // Both buttons go in a bar at the TOP of this page.
        p.add(actionBar(adjustment, refresh), BorderLayout.NORTH);

        // STEP 1: get the raw numbers from the database. One row per active account.
        // The four SUM(CASE ...) columns add up the debits and credits of POSTED entries, split in two groups:
        //   unadj_dr / unadj_cr = lines of normal entries (reference is empty OR does NOT start with 'ADJ-')
        //   adj_dr / adj_cr     = lines of adjusting entries (reference starts with 'ADJ-')
        // COALESCE(..., 0) turns NULL into 0 for accounts that have no lines.
        // LEFT JOIN keeps accounts that have no journal lines yet (they appear with zeros).
        // GROUP BY = one result row per account; ORDER BY account code.
        String sql = """
            SELECT a.account_code, a.account_name, a.account_type,
                   COALESCE(SUM(CASE WHEN je.status='POSTED' AND (je.reference_no IS NULL OR je.reference_no NOT LIKE 'ADJ-%') THEN jel.debit ELSE 0 END),0) AS unadj_dr,
                   COALESCE(SUM(CASE WHEN je.status='POSTED' AND (je.reference_no IS NULL OR je.reference_no NOT LIKE 'ADJ-%') THEN jel.credit ELSE 0 END),0) AS unadj_cr,
                   COALESCE(SUM(CASE WHEN je.status='POSTED' AND je.reference_no LIKE 'ADJ-%' THEN jel.debit ELSE 0 END),0) AS adj_dr,
                   COALESCE(SUM(CASE WHEN je.status='POSTED' AND je.reference_no LIKE 'ADJ-%' THEN jel.credit ELSE 0 END),0) AS adj_cr
            FROM accounts a
            LEFT JOIN journal_entry_lines jel ON jel.account_id=a.id
            LEFT JOIN journal_entries je ON je.id=jel.journal_entry_id
            WHERE a.is_active=TRUE
            GROUP BY a.id,a.account_code,a.account_name,a.account_type
            ORDER BY a.account_code
            """;
        // "raw" = the unprocessed database result (7 columns: code, name, type, and the 4 sums above).
        DefaultTableModel raw = readTable(sql, new String[]{"Code","Account","Type","Unadj Dr","Unadj Cr","Adj Dr","Adj Cr"});
        // "model" = the table we actually DISPLAY, with the 12 worksheet columns (we fill it in the loop below).
        DefaultTableModel model = newTableModel(new String[]{"Code","Account","Unadjusted Dr","Unadjusted Cr","Adjustments Dr","Adjustments Cr","Adjusted Dr","Adjusted Cr","Income Stmt Dr","Income Stmt Cr","Balance Sheet Dr","Balance Sheet Cr"});
        // "totals" holds the running column totals for the 10 amount columns (index 0..9), all starting at 0.
        BigDecimal[] totals = new BigDecimal[10];
        java.util.Arrays.fill(totals, BigDecimal.ZERO);
        // "income" = net income so far (revenue minus expenses). Positive = profit, negative = loss.
        BigDecimal income = BigDecimal.ZERO;

        // STEP 2: process every account row and spread its amounts across the 5 column pairs.
        for (int i=0; i<raw.getRowCount(); i++) {
            // Read the four sums for this account (helper method "amount" is defined below).
            BigDecimal udr = amount(raw,i,3), ucr = amount(raw,i,4), adr = amount(raw,i,5), acr = amount(raw,i,6);
            // Adjusted balance expressed as (debits - credits): unadjusted debit - unadjusted credit + adjustment debit - adjustment credit.
            // Positive = the account has a DEBIT balance; negative = a CREDIT balance.
            BigDecimal net = udr.subtract(ucr).add(adr).subtract(acr);
            // The account type (ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE) from column 2.
            String type = raw.getValueAt(i,2).toString();
            // Four amounts to fill in: Income Statement Dr/Cr (idr, icr) and Balance Sheet Dr/Cr (bdr, bcr).
            BigDecimal idr=BigDecimal.ZERO, icr=BigDecimal.ZERO, bdr=BigDecimal.ZERO, bcr=BigDecimal.ZERO;
            // REVENUE goes to the Income Statement. A normal revenue has a credit balance (net is negative),
            // so negate it to get a positive credit; "income" grows by the revenue amount.
            if ("REVENUE".equals(type)) { icr=positive(net.negate()); idr=positive(net); income=income.add(net.negate()); }
            // EXPENSE goes to the Income Statement too. A normal expense has a debit balance (net positive);
            // "income" shrinks by the expense amount.
            else if ("EXPENSE".equals(type)) { idr=positive(net); icr=positive(net.negate()); income=income.subtract(net); }
            // ASSET goes to the Balance Sheet: a debit balance goes in the Dr column, a credit balance in the Cr column.
            else if ("ASSET".equals(type)) { bdr=positive(net); bcr=positive(net.negate()); }
            // Everything else (LIABILITY and EQUITY) also goes to the Balance Sheet, with the same rule.
            else { bcr=positive(net.negate()); bdr=positive(net); }
            // The ten amounts of this row, in column order:
            // [0]unadj Dr [1]unadj Cr [2]adjustments Dr [3]adjustments Cr [4]adjusted Dr [5]adjusted Cr
            // [6]income stmt Dr [7]income stmt Cr [8]balance sheet Dr [9]balance sheet Cr
            // (positive(net) / positive(net.negate()) = the adjusted debit / credit balance.)
            BigDecimal[] values={udr,ucr,adr,acr,positive(net),positive(net.negate()),idr,icr,bdr,bcr};
            // Add this row's ten amounts to the running column totals.
            for(int j=0;j<values.length;j++) totals[j]=totals[j].add(values[j]);
            // Build the display row: 12 cells. Cells 0 and 1 are the account code and name copied from the raw data.
            Object[] row=new Object[12]; row[0]=raw.getValueAt(i,0); row[1]=raw.getValueAt(i,1);
            // Cells 2..11 = the ten amounts as pesos text; zero amounts are left blank to keep the worksheet readable.
            for(int j=0;j<values.length;j++) row[j+2]=values[j].signum()==0?"":Money.currency(values[j]);
            // Add the finished row to the displayed table.
            model.addRow(row);
        }
        // Net income closes the worksheet: profit is an income-statement debit and a balance-sheet credit;
        // a loss is the opposite. This is what makes both pairs balance.
        // STEP 3: add the NET INCOME / NET LOSS row. "closed" is a copy of the totals that will include it
        // (clone() so the original totals stay untouched).
        BigDecimal[] closed = totals.clone();
        // The special row (12 cells, all empty by default).
        Object[] netRow = new Object[12];
        // Row label in column 1: NET INCOME if profit or break-even, NET LOSS otherwise.
        netRow[1] = income.signum() >= 0 ? "NET INCOME" : "NET LOSS";
        if (income.signum() >= 0) {
            // PROFIT: put the profit in the Income Statement DEBIT column (closed[6], displayed in cell 8)
            // and in the Balance Sheet CREDIT column (closed[9], displayed in cell 11).
            closed[6] = closed[6].add(income);  netRow[8] = Money.currency(income);
            closed[9] = closed[9].add(income);  netRow[11] = Money.currency(income);
        } else {
            // LOSS: the opposite. Put the loss amount (made positive with abs()) in the Income Statement CREDIT column
            // (closed[7], cell 9) and the Balance Sheet DEBIT column (closed[8], cell 10).
            closed[7] = closed[7].add(income.abs());  netRow[9] = Money.currency(income.abs());
            closed[8] = closed[8].add(income.abs());  netRow[10] = Money.currency(income.abs());
        }
        // Add the net income / loss row to the table.
        model.addRow(netRow);

        // STEP 4: GRAND TOTAL row = the total of each of the ten amount columns (including the net income row).
        Object[] totalRow = new Object[12]; totalRow[1] = "GRAND TOTAL";
        // Cells 2..11 get the ten grand totals formatted as pesos.
        for (int j = 0; j < closed.length; j++) totalRow[j + 2] = Money.currency(closed[j]);
        model.addRow(totalRow);

        // Debit minus credit for each of the five column pairs. Every one must be zero.
        // STEP 5: BALANCE DIFFERENCE row = for each pair, |total debit - total credit|. It should read ₱0.00 when the books balance.
        Object[] diffRow = new Object[12]; diffRow[1] = "BALANCE DIFFERENCE";
        // Remember the largest difference to decide the overall "balanced / NOT balanced" message below.
        BigDecimal maxDifference = BigDecimal.ZERO;
        // Loop over the five pairs (pair 0 = unadjusted, 1 = adjustments, 2 = adjusted, 3 = income statement, 4 = balance sheet).
        for (int pair = 0; pair < 5; pair++) {
            // Debit total of the pair minus its credit total (closed[pair*2] = Dr, closed[pair*2+1] = Cr).
            BigDecimal diff = closed[pair * 2].subtract(closed[pair * 2 + 1]);
            // Show the difference in the pair's Debit column (cell 2 + pair*2), always as a positive amount.
            diffRow[2 + pair * 2] = Money.currency(diff.abs());
            // Keep the biggest difference seen so far.
            if (diff.abs().compareTo(maxDifference) > 0) maxDifference = diff.abs();
        }
        model.addRow(diffRow);

        // STEP 6: display the table.
        JTable table=Tables.create(model);
        // Don't squeeze 12 columns into the window width; use fixed widths and a horizontal scrollbar instead.
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        // Preferred width in pixels of each of the 12 columns, in order.
        int[] widths={65,200,125,125,130,130,120,120,130,130,135,135};
        // Apply each width to its column.
        for(int i=0;i<widths.length;i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        // Tooltip when hovering over the header: explains which columns belong together.
        table.getTableHeader().setToolTipText("Column pairs: Unadjusted Trial Balance | Adjustments | Adjusted Trial Balance | Income Statement | Balance Sheet");
        JScrollPane scroll=Tables.scroll(table);
        // Show the horizontal scrollbar only when the window is too narrow for all columns.
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        p.add(scroll,BorderLayout.CENTER);

        // STEP 7: summary line at the bottom. "balanced" is true only if every pair has a difference of exactly 0.
        boolean balanced = maxDifference.signum() == 0;
        // Text such as: "Net income: ₱500.00 | Balance difference: ₱0.00 (balanced)".
        JLabel status=new JLabel((income.signum() >= 0 ? "Net income: " : "Net loss: ") + Money.currency(income.abs())
                + "     |     Balance difference: " + Money.currency(maxDifference)
                + (balanced ? "  (balanced)" : "  (NOT balanced - review entries)"));
        status.setBorder(new EmptyBorder(8,4,0,4)); status.setFont(Theme.BOLD);
        // Green text when balanced, red when not.
        status.setForeground(balanced ? Theme.GREEN_DARK : Theme.DANGER);
        p.add(status,BorderLayout.SOUTH);
        return p;
    }

    // Reads one cell of a table model and converts it to a BigDecimal (0 if the cell is empty).
    // Used to get the SUM values from the "raw" query result.
    private BigDecimal amount(DefaultTableModel m,int row,int column) {
        Object value=m.getValueAt(row,column);
        return value==null?BigDecimal.ZERO:new BigDecimal(value.toString());
    }
    // Returns n if it is greater than zero; otherwise returns 0. (Turns negative numbers into zero.)
    private BigDecimal positive(BigDecimal n) { return n.signum()>0?n:BigDecimal.ZERO; }

    // Opens a popup form to record an ADJUSTING entry (e.g. depreciation, accrued salaries).
    // It is saved like any other journal entry, but its reference must start with "ADJ-" so the worksheet puts it in the Adjustments columns.
    private void showAdjustmentDialog() {
        // Load the accounts for the dropdowns. "final" = assigned once.
        final List<AccountOption> accounts;
        // If loading fails, show an error and stop (nothing to choose from).
        try { accounts=JournalService.loadActiveAccounts(); }
        catch(SQLException ex) { error("Could not load accounts: "+ex.getMessage()); return; }
        // The popup's form: a grid with 2 columns (label, field) and as many rows as needed (0 = automatic), 8px gaps.
        JPanel form=new JPanel(new GridLayout(0,2,8,8));
        // The input controls. Date = today, reference = "ADJ-" + last 6 digits of the current time.
        JTextField date=new JTextField(LocalDate.now().toString());
        JTextField reference=new JTextField("ADJ-"+(System.currentTimeMillis()%1000000));
        JTextField description=new JTextField(); JComboBox<AccountOption> debit=new JComboBox<>(), credit=new JComboBox<>();
        // Fill both dropdowns with all active accounts.
        for(AccountOption a:accounts){debit.addItem(a);credit.addItem(a);}
        JTextField amount=new JTextField();
        // Add a label + its field for each row of the form.
        form.add(new JLabel("Date (YYYY-MM-DD)")); form.add(date);
        form.add(new JLabel("Reference")); form.add(reference);
        form.add(new JLabel("Description")); form.add(description);
        form.add(new JLabel("Debit account")); form.add(debit);
        form.add(new JLabel("Credit account")); form.add(credit);
        form.add(new JLabel("Amount (₱)")); form.add(amount);
        // Show the form in an OK / Cancel popup; "result" tells us which button was pressed.
        int result=JOptionPane.showConfirmDialog(frame,form,"Post an adjusting entry",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        // Cancel (or closing the popup) = do nothing.
        if(result!=JOptionPane.OK_OPTION)return;
        // CHECK 1: all fields must be filled in and both accounts selected.
        if(description.getText().isBlank()||reference.getText().isBlank()||debit.getSelectedItem()==null||credit.getSelectedItem()==null){error("Complete all adjustment fields.");return;}
        // CHECK 2: the reference must start with "ADJ-" (case ignored), otherwise it would not show in the adjustment columns.
        if(!reference.getText().trim().toUpperCase().startsWith("ADJ-")){error("Adjustment reference must start with ADJ- so it appears in the adjustment columns.");return;}
        // CHECK 3: debit and credit must be different accounts (compares their database ids).
        if(((AccountOption)debit.getSelectedItem()).id==((AccountOption)credit.getSelectedItem()).id){error("Choose different debit and credit accounts.");return;}
        try {
            // Convert the typed date and amount. These throw an exception if the date is invalid or the amount has more than 2 decimals.
            Date d=Date.valueOf(LocalDate.parse(date.getText().trim())); BigDecimal n=Money.parse(amount.getText().trim());
            // The amount must be greater than zero.
            if(n.signum()<=0)throw new IllegalArgumentException();
            // Save the adjusting entry (same method as normal entries: one transaction, one debit line and one credit line).
            JournalService.postTwoLineEntry(d,reference.getText().trim(),description.getText().trim(),(AccountOption)debit.getSelectedItem(),(AccountOption)credit.getSelectedItem(),n);
            // Update the status bar and refresh the worksheet so the new adjustment shows up.
            frame.setStatus(" Posted adjusting entry"); show();
        } catch(Exception ex){error("Could not post adjustment. Check the date, amount, and unique reference number.");}
        // Any error (bad date, bad amount, duplicate reference, database problem) ends up in the catch above with one general message.
    }
}
