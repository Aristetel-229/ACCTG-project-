// Package for the five screens of the app.
package com.tala.ui.pages;

import com.tala.ui.BasePage;
import com.tala.ui.components.RoundedButton;
import com.tala.ui.components.Tables;
import com.tala.ui.MainFrame;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

// Read-only page that lists every posted debit and credit line.
public class GeneralLedgerPage extends BasePage {
    // Constructor: hand the main window to BasePage.
    public GeneralLedgerPage(MainFrame frame) { super(frame); }

    // Page title.
    @Override protected String title() { return "Ledger"; }

    // Subtitle (the table supports click-to-sort, see below).
    @Override protected String hint() { return "Every posted debit and credit. Click a column header to sort."; }

    // Builds the page content.
    @Override protected JComponent build() {
        JPanel p = pagePanel();
        // SQL (text block). It reads one row per journal LINE and joins the related data:
        //   - journal_entry_lines (jel)  -> the debit / credit amounts
        //   - journal_entries (je)       -> date, reference number, description
        //   - accounts (a)               -> account code and name
        // WHERE status = 'POSTED' ignores reversed entries.
        // ORDER BY: oldest first by date, then by entry id, then by line id (keeps the debit before the credit).
        String sql = """
            SELECT je.entry_date, je.reference_no, a.account_code, a.account_name,
                   jel.debit, jel.credit, je.description
            FROM journal_entry_lines jel
            JOIN journal_entries je ON je.id = jel.journal_entry_id
            JOIN accounts a ON a.id = jel.account_id
            WHERE je.status = 'POSTED'
            ORDER BY je.entry_date, je.id, jel.id
            """;
        // Run the query; the 7 titles match the 7 selected columns in the same order.
        DefaultTableModel model = readTable(sql,
                new String[]{"Date", "Reference", "Code", "Account", "Debit", "Credit", "Description"});
        JTable table = Tables.create(model);
        // Lets the user click a column header to sort by that column.
        table.setAutoCreateRowSorter(true);
        p.add(Tables.scroll(table), BorderLayout.CENTER);
        // Refresh button at the bottom right.
        JButton refresh = new RoundedButton("Refresh", RoundedButton.Kind.SECONDARY);
        refresh.addActionListener(e -> show());
        p.add(actionBar(refresh), BorderLayout.SOUTH);
        return p;
    }
}
