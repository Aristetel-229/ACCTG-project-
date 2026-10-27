// Package for the five screens of the app.
package com.tala.ui.pages;

import com.tala.ui.BasePage;
import com.tala.ui.components.RoundedButton;
import com.tala.ui.components.Tables;
import com.tala.ui.MainFrame;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

// A read-only page that lists every account. "extends BasePage" gives it show(), readTable(), actionBar(), etc.
public class ChartOfAccountsPage extends BasePage {
    // Constructor: pass the main window up to BasePage, which stores it.
    public ChartOfAccountsPage(MainFrame frame) { super(frame); }

    // Page title (shown in big green text at the top).
    @Override protected String title() { return "Chart of Accounts"; }

    // Subtitle shown under the title.
    @Override protected String hint() { return "All the accounts your business can use, grouped by type."; }

    // Builds the page content.
    @Override protected JComponent build() {
        // Empty container for the page.
        JPanel p = pagePanel();
        // Column titles shown in the table header.
        String[] columns = {"Code", "Account Name", "Type", "Normal Balance", "Active"};
        // Run the SELECT and load the result into a table model.
        // The query returns ALL accounts (active or not), sorted by account code. The 5 selected columns match the 5 titles above.
        DefaultTableModel model = readTable(
                "SELECT account_code, account_name, account_type, normal_balance, is_active FROM accounts ORDER BY account_code",
                columns);
        // Create the styled table from the model.
        JTable table = Tables.create(model);
        // Wrap in a scroll pane (so the header and scrolling work) and put it in the middle of the page.
        p.add(Tables.scroll(table), BorderLayout.CENTER);
        // "Refresh" button (light style).
        JButton refresh = new RoundedButton("Refresh", RoundedButton.Kind.SECONDARY);
        // On click, show() rebuilds the page, which re-runs the query = refreshed data.
        refresh.addActionListener(e -> show());
        // Put the button row at the bottom of the page.
        p.add(actionBar(refresh), BorderLayout.SOUTH);
        return p;
    }
}
