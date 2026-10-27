// Package for the five screens of the app.
package com.tala.ui.pages;

import com.tala.db.DB;
import com.tala.service.ReportService;
import com.tala.ui.BasePage;
import com.tala.ui.MainFrame;
import com.tala.ui.ResetDataAction;
import com.tala.ui.Theme;
import com.tala.ui.components.Cards;
import com.tala.ui.components.RoundedButton;
import com.tala.ui.components.Tables;
import com.tala.util.Money;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

// The home page: six summary cards, a table of recent entries, and the Refresh / Reset Data buttons.
public class DashboardPage extends BasePage {
    // A custom amber/orange color used only for the Liabilities card.
    private static final Color AMBER = new Color(176, 125, 35);

    // Constructor: hand the main window to BasePage.
    public DashboardPage(MainFrame frame) { super(frame); }

    // Page title and subtitle.
    @Override protected String title() { return "Dashboard"; }
    @Override protected String hint() { return "Your books at a glance, based on all posted entries."; }

    // Builds the page content.
    @Override protected JComponent build() {
        // Labels that will display the big numbers on the cards. They start with zero values,
        // so the cards still look right if the database cannot be reached.
        JLabel assets = new JLabel("₱0.00"), liabilities = new JLabel("₱0.00"), revenue = new JLabel("₱0.00");
        JLabel expenses = new JLabel("₱0.00"), netIncome = new JLabel("₱0.00"), entries = new JLabel("0");
        // Color of the net income number: green by default, changed to red below if it is a loss.
        Color netColor = Theme.GREEN_DARK;

        // Keep revenue and expense totals in variables because net income is computed from them later.
        BigDecimal rev = BigDecimal.ZERO, exp = BigDecimal.ZERO;
        // Open ONE connection and reuse it for all five queries below.
        try (Connection c = DB.getConnection()) {
            // Total of all ASSET accounts, formatted as pesos, shown on the Assets card.
            assets.setText(Money.currency(ReportService.accountTypeBalance(c, "ASSET")));
            // Total of all LIABILITY accounts.
            liabilities.setText(Money.currency(ReportService.accountTypeBalance(c, "LIABILITY")));
            // Revenue and expense totals are kept in variables (used for net income later).
            rev = ReportService.accountTypeBalance(c, "REVENUE");
            exp = ReportService.accountTypeBalance(c, "EXPENSE");
            revenue.setText(Money.currency(rev));
            expenses.setText(Money.currency(exp));
            // How many journal entries are posted.
            entries.setText(String.valueOf(ReportService.countPostedEntries(c)));
            // Everything worked: update the status bar.
            frame.setStatus(" Connected to MySQL");
        } catch (SQLException ex) {
            // Could not connect or a query failed: update the status bar and tell the user what to check.
            frame.setStatus(" MySQL connection failed");
            JOptionPane.showMessageDialog(frame,
                    "Could not connect to MySQL.\nRun database/tala.sql and check the DB.java settings.\n\n" + ex.getMessage(),
                    "Database Connection", JOptionPane.ERROR_MESSAGE);
        }
        // NET INCOME = Revenue - Expenses.
        BigDecimal net = rev.subtract(exp);
        // signum() returns -1, 0 or +1. For a loss, show "-" then the positive amount, e.g. "-₱500.00".
        netIncome.setText((net.signum() < 0 ? "-" : "") + Money.currency(net.abs()));
        // A loss is shown in red.
        if (net.signum() < 0) netColor = Theme.DANGER;

        // Grid of cards: 2 rows x 3 columns, 12px gaps.
        JPanel cards = new JPanel(new GridLayout(2, 3, 12, 12));
        cards.setOpaque(false);
        // Each Cards.metric(...) call: title, small hint, the label with the number, accent color.
        cards.add(Cards.metric("TOTAL ASSETS", "What the business owns", assets, Theme.PRIMARY));
        cards.add(Cards.metric("TOTAL LIABILITIES", "What the business owes", liabilities, AMBER));
        cards.add(Cards.metric("TOTAL REVENUE", "Money earned", revenue, new Color(22, 163, 74)));
        cards.add(Cards.metric("TOTAL EXPENSES", "Money spent", expenses, Theme.DANGER));
        cards.add(Cards.metric("NET INCOME", "Revenue minus expenses", netIncome, netColor));
        cards.add(Cards.metric("POSTED ENTRIES", "Journal entries recorded", entries, Theme.GREEN_ACTIVE));

        // SQL for the "Recent entries" table: one row per journal ENTRY (not per line).
        //   SUM(jel.debit) = the entry's total amount (total debits = total credits, so this is the entry amount).
        //   GROUP BY combines the lines of the same entry into a single row.
        //   ORDER BY newest first; LIMIT 8 = show only the 8 latest entries.
        String sql = """
            SELECT je.entry_date, je.reference_no, je.description, SUM(jel.debit) AS amount
            FROM journal_entries je
            JOIN journal_entry_lines jel ON jel.journal_entry_id = je.id
            WHERE je.status = 'POSTED'
            GROUP BY je.id, je.entry_date, je.reference_no, je.description
            ORDER BY je.entry_date DESC, je.id DESC
            LIMIT 8
            """;
        // Load the query into a table model, create the styled table, and wrap it in a scroll pane.
        DefaultTableModel model = readTable(sql, new String[]{"Date", "Reference", "Description", "Amount"});
        JTable table = Tables.create(model);
        JScrollPane scroll = Tables.scroll(table);
        // Small heading above the table.
        JLabel recent = new JLabel("Recent entries");
        recent.setFont(Theme.BOLD);
        recent.setForeground(Theme.GREEN_DARK);
        recent.setBorder(BorderFactory.createEmptyBorder(0, 2, 6, 0));
        // "lower" panel = heading on top, table below.
        JPanel lower = new JPanel(new BorderLayout());
        lower.setOpaque(false);
        lower.add(recent, BorderLayout.NORTH);
        lower.add(scroll, BorderLayout.CENTER);

        // Refresh button: rebuild this page (re-runs all queries).
        JButton refresh = new RoundedButton("Refresh", RoundedButton.Kind.SECONDARY);
        refresh.addActionListener(e -> show());
        // Red Reset Data button: runs the two-step confirmation; "this::show" refreshes the dashboard after a successful reset.
        JButton reset = new RoundedButton("Reset Data", RoundedButton.Kind.DANGER);
        reset.addActionListener(e -> ResetDataAction.confirmAndRun(frame, this::show));

        // Assemble the page: cards on top, recent entries in the middle, buttons at the bottom.
        JPanel p = pagePanel();
        p.add(cards, BorderLayout.NORTH);
        p.add(lower, BorderLayout.CENTER);
        p.add(actionBar(refresh, reset), BorderLayout.SOUTH);
        return p;
    }
}
