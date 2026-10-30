package com.tala.ui.pages;

// Database connection helper and the service that computes report totals.
import com.tala.db.DB;
import com.tala.service.ReportService;
// UI classes: base page, main window, reset action, theme, and custom components.
import com.tala.ui.BasePage;
import com.tala.ui.MainFrame;
import com.tala.ui.ResetDataAction;
import com.tala.ui.Theme;
import com.tala.ui.components.Cards;
import com.tala.ui.components.RoundedButton;
import com.tala.ui.components.Tables;
// Helper that formats amounts as pesos.
import com.tala.util.Money;

// Swing/AWT classes and types for numbers and SQL.
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Dashboard page: shows summary cards (total assets, liabilities, revenue, expenses, net income,
 * number of posted entries) and a table of the 8 most recent journal entries.
 */
public class DashboardPage extends BasePage {
    // Amber color used for the liabilities card.
    private static final Color AMBER = new Color(176, 125, 35);

    // Constructor: passes the main window to BasePage.
    public DashboardPage(MainFrame frame) { super(frame); }

    // Page title.
    @Override protected String title() { return "Dashboard"; }
    // Hint shown under the title.
    @Override protected String hint() { return "Your books at a glance, based on all posted entries."; }

    // Builds the dashboard content.
    @Override protected JComponent build() {
        // Labels that will display the card values; they start at zero in case the database is not available.
        JLabel assets = new JLabel("₱0.00"), liabilities = new JLabel("₱0.00"), revenue = new JLabel("₱0.00");
        JLabel expenses = new JLabel("₱0.00"), netIncome = new JLabel("₱0.00"), entries = new JLabel("0");
        // Net income is shown in dark green by default (a profit).
        Color netColor = Theme.GREEN_DARK;

        // Revenue and expense totals start at zero; they are needed later to compute net income.
        BigDecimal rev = BigDecimal.ZERO, exp = BigDecimal.ZERO;
        // Open a database connection (closed automatically at the end of the block).
        try (Connection c = DB.getConnection()) {
            // Total assets: what the business owns.
            assets.setText(Money.currency(ReportService.accountTypeBalance(c, "ASSET")));
            // Total liabilities: what the business owes.
            liabilities.setText(Money.currency(ReportService.accountTypeBalance(c, "LIABILITY")));
            // Total revenue (income earned), kept for the net income calculation.
            rev = ReportService.accountTypeBalance(c, "REVENUE");
            // Total expenses (costs), also kept for net income.
            exp = ReportService.accountTypeBalance(c, "EXPENSE");
            // Show revenue and expenses as formatted peso text.
            revenue.setText(Money.currency(rev));
            expenses.setText(Money.currency(exp));
            // Count of journal entries that have status POSTED.
            entries.setText(String.valueOf(ReportService.countPostedEntries(c)));
            // Tell the user the database connection works.
            frame.setStatus(" Connected to MySQL");
        // If the connection or a query fails...
        } catch (SQLException ex) {
            // ...show a failure message in the status bar...
            frame.setStatus(" MySQL connection failed");
            // ...and a pop-up telling the user how to fix it.
            JOptionPane.showMessageDialog(frame,
                    "Could not connect to MySQL.\nRun database/tala.sql and check the DB.java settings.\n\n" + ex.getMessage(),
                    "Database Connection", JOptionPane.ERROR_MESSAGE);
        }
        // Net income = Revenue - Expenses (positive = profit, negative = loss).
        BigDecimal net = rev.subtract(exp);
        // Show a minus sign for a loss; the amount itself is formatted from the absolute value.
        netIncome.setText((net.signum() < 0 ? "-" : "") + Money.currency(net.abs()));
        // A net loss is shown in red.
        if (net.signum() < 0) netColor = Theme.DANGER;

        // Grid of cards: 2 rows x 3 columns with 12 pixel gaps.
        JPanel cards = new JPanel(new GridLayout(2, 3, 12, 12));
        // Transparent background.
        cards.setOpaque(false);
        // Each card: title, short explanation, value label, accent color.
        cards.add(Cards.metric("TOTAL ASSETS", "What the business owns", assets, Theme.PRIMARY));
        cards.add(Cards.metric("TOTAL LIABILITIES", "What the business owes", liabilities, AMBER));
        cards.add(Cards.metric("TOTAL REVENUE", "Money earned", revenue, new Color(22, 163, 74)));
        cards.add(Cards.metric("TOTAL EXPENSES", "Money spent", expenses, Theme.DANGER));
        cards.add(Cards.metric("NET INCOME", "Revenue minus expenses", netIncome, netColor));
        cards.add(Cards.metric("POSTED ENTRIES", "Journal entries recorded", entries, Theme.GREEN_ACTIVE));

        // SQL (a text block) that gets the 8 newest posted entries with their total amount.
        // Note: no comments inside the text block, because it is part of the SQL string.
        // SUM(debit) per entry = the entry amount (total debits equal total credits in a balanced entry).
        String sql = """
            SELECT je.entry_date, je.reference_no, je.description, SUM(jel.debit) AS amount
            FROM journal_entries je
            JOIN journal_entry_lines jel ON jel.journal_entry_id = je.id
            WHERE je.status = 'POSTED'
            GROUP BY je.id, je.entry_date, je.reference_no, je.description
            ORDER BY je.entry_date DESC, je.id DESC
            LIMIT 8
            """;
        // Run the query and put the results in a table model with these column headings.
        DefaultTableModel model = readTable(sql, new String[]{"Date", "Reference", "Description", "Amount"});
        // Create the styled table.
        JTable table = Tables.create(model);
        // Wrap the table in a scroll pane.
        JScrollPane scroll = Tables.scroll(table);
        // Small heading above the table.
        JLabel recent = new JLabel("Recent entries");
        // Bold green style.
        recent.setFont(Theme.BOLD);
        recent.setForeground(Theme.GREEN_DARK);
        // Small spacing around the heading.
        recent.setBorder(BorderFactory.createEmptyBorder(0, 2, 6, 0));
        // Panel that holds the heading and the table together.
        JPanel lower = new JPanel(new BorderLayout());
        // Transparent background.
        lower.setOpaque(false);
        // Heading on top, table below it.
        lower.add(recent, BorderLayout.NORTH);
        lower.add(scroll, BorderLayout.CENTER);

        // Refresh button reloads the dashboard numbers.
        JButton refresh = new RoundedButton("Refresh", RoundedButton.Kind.SECONDARY);
        // On click, rebuild this page.
        refresh.addActionListener(e -> show());
        // Red Reset Data button for deleting all entries.
        JButton reset = new RoundedButton("Reset Data", RoundedButton.Kind.DANGER);
        // On click, ask for confirmation twice and then refresh this page after the reset.
        reset.addActionListener(e -> ResetDataAction.confirmAndRun(frame, this::show));

        // Main page panel.
        JPanel p = pagePanel();
        // Cards at the top, recent entries in the middle, buttons at the bottom.
        p.add(cards, BorderLayout.NORTH);
        p.add(lower, BorderLayout.CENTER);
        p.add(actionBar(refresh, reset), BorderLayout.SOUTH);
        // Return the finished page.
        return p;
    }
}
