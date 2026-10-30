package com.tala.ui.pages;

// Imports: base class for pages, UI components, and the main window type.
import com.tala.ui.BasePage;
import com.tala.ui.components.RoundedButton;
import com.tala.ui.components.Tables;
import com.tala.ui.MainFrame;

// Swing and AWT classes for panels, tables and layouts.
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Chart of Accounts page: a read-only table listing every account (code, name, type,
 * normal balance, active flag) stored in the accounts table. The chart of accounts is the
 * master list of accounts the business may use when recording journal entries.
 */
public class ChartOfAccountsPage extends BasePage {
    // Constructor: passes the main window up to BasePage.
    public ChartOfAccountsPage(MainFrame frame) { super(frame); }

    // Title shown at the top of the page.
    @Override protected String title() { return "Chart of Accounts"; }

    // Hint line shown under the title.
    @Override protected String hint() { return "All the accounts your business can use, grouped by type."; }

    // Builds the content of this page.
    @Override protected JComponent build() {
        // Main panel using BorderLayout.
        JPanel p = pagePanel();
        // Column headings for the table.
        String[] columns = {"Code", "Account Name", "Type", "Normal Balance", "Active"};
        // Query all accounts sorted by account code (so assets 1xxx come first, then liabilities 2xxx, etc.).
        DefaultTableModel model = readTable(
                "SELECT account_code, account_name, account_type, normal_balance, is_active FROM accounts ORDER BY account_code",
                columns);
        // Create a styled table from the data.
        JTable table = Tables.create(model);
        // Wrap in a scroll pane and put it in the center.
        p.add(Tables.scroll(table), BorderLayout.CENTER);
        // Refresh button reloads the list from the database.
        JButton refresh = new RoundedButton("Refresh", RoundedButton.Kind.SECONDARY);
        // On click, rebuild and show this page again.
        refresh.addActionListener(e -> show());
        // Put the button in a right-aligned bar at the bottom.
        p.add(actionBar(refresh), BorderLayout.SOUTH);
        // Return the finished page.
        return p;
    }
}
