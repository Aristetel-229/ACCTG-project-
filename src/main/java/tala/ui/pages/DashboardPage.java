package tala.ui.pages;

import tala.dao.AccountDAO;
import tala.dao.JournalDAO;
import tala.model.Account;
import tala.model.DashboardSummary;
import tala.model.JournalEntry;
import tala.ui.MainFrame;
import tala.ui.components.AppButton;
import tala.ui.components.Badge;
import tala.ui.components.Card;
import tala.ui.components.IconBox;
import tala.ui.components.Icons;
import tala.ui.components.PageHeader;
import tala.ui.components.ReadOnlyTableModel;
import tala.ui.components.StatCard;
import tala.ui.components.TalaTable;
import tala.ui.components.WrapLabel;
import tala.ui.theme.Theme;
import tala.util.DateUtil;
import tala.util.Money;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * DashboardPage
 * ------------------------------------------------------------
 * The books at a glance: six tiles, the latest entries,
 * the accounting equation, quick actions and a tip.
 */
public class DashboardPage extends BasePage {

    private final PageHeader header;
    private final StatCard assetsCard, liabilitiesCard, revenueCard, expensesCard, netIncomeCard, entriesCard;

    private final ReadOnlyTableModel recentModel = new ReadOnlyTableModel("Date", "Reference", "Description", "Amount (" + Money.PESO_SIGN.trim() + ")");
    private final TalaTable recentTable = new TalaTable(recentModel);
    private final JLabel recentEmpty = new JLabel("No entries yet. Record your first transaction in Journal Entry.");

    private final Badge equationBadge = Badge.ok("Balanced");
    private final JLabel assetsAmount = amountLabel();
    private final JLabel rightSideAmount = amountLabel();
    private final EquationBar assetsBar = new EquationBar();
    private final EquationBar rightSideBar = new EquationBar();
    private final JLabel liabilitiesLegend = new JLabel();
    private final JLabel equityLegend = new JLabel();

    private final WrapLabel tipText = new WrapLabel("");

    public DashboardPage(MainFrame mainFrame, AccountDAO accountDAO, JournalDAO journalDAO) {
        super(mainFrame, accountDAO, journalDAO);

        // ---------- page header ----------
        header = new PageHeader(Icons.DASHBOARD, "Dashboard", "Your books at a glance, based on all posted entries.");
        AppButton refresh = new AppButton("Refresh", Icons.get(Icons.REFRESH, 16, Theme.PURPLE_DEEP), AppButton.Style.SECONDARY);
        refresh.addActionListener(e -> refreshData());
        AppButton reset = new AppButton("Reset data", AppButton.Style.DANGER);
        reset.addActionListener(e -> resetData());
        header.addAction(refresh);
        header.addAction(reset);
        addSection(header);

        // ---------- six tiles (3 per row) ----------
        assetsCard = new StatCard(Icons.LAYERS, Theme.PURPLE_TINT, Theme.PURPLE, "Total assets", "What the business owns", StatCard.Look.NORMAL);
        liabilitiesCard = new StatCard(Icons.FILE, Theme.AMBER_BG, Theme.AMBER, "Total liabilities", "What the business owes", StatCard.Look.NORMAL);
        revenueCard = new StatCard(Icons.TREND_UP, Theme.GREEN_BG, Theme.GREEN, "Total revenue", "Money earned", StatCard.Look.NORMAL);
        expensesCard = new StatCard(Icons.TREND_DOWN, Theme.RED_BG, Theme.RED, "Total expenses", "Money spent", StatCard.Look.NORMAL);
        netIncomeCard = new StatCard(Icons.PESO, Theme.YELLOW, Theme.PURPLE_DEEP, "Net income", "Revenue minus expenses", StatCard.Look.HIGHLIGHT);
        entriesCard = new StatCard(Icons.PEN, Theme.PURPLE, Theme.YELLOW, "Posted entries", "Journal entries recorded", StatCard.Look.CREAM);

        JPanel tiles = new JPanel(new GridLayout(2, 3, 18, 18));
        tiles.setOpaque(false);
        tiles.add(assetsCard);
        tiles.add(liabilitiesCard);
        tiles.add(revenueCard);
        tiles.add(expensesCard);
        tiles.add(netIncomeCard);
        tiles.add(entriesCard);
        addSection(tiles);

        // ---------- left: recent entries + equation / right: quick actions + tip ----------
        JPanel left = stack(createRecentCard(), createEquationCard());
        JPanel right = stack(createQuickActionsCard(), createTipCard());
        addSection(twoColumns(left, 0.64, right, 0.36));
        finishSections();
    }

    // =================================================================
    //  Building the cards
    // =================================================================

    private Card createRecentCard() {
        Card card = new Card("Recent entries", "The latest transactions you posted");
        JButton viewAll = linkButton("View all in General Journal");
        viewAll.addActionListener(e -> mainFrame.showPage(MainFrame.GENERAL_JOURNAL));
        card.addToHeader(viewAll);

        recentTable.setRightAligned(3);
        recentTable.setPillColumns(1);
        recentTable.setColumnWidths(130, 150, 0, 160);

        recentEmpty.setFont(Theme.regular(15));
        recentEmpty.setForeground(Theme.MUTED);
        recentEmpty.setHorizontalAlignment(JLabel.CENTER);
        recentEmpty.setBorder(BorderFactory.createEmptyBorder(26, 20, 26, 20));

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        body.add(recentTable.inPanel(), BorderLayout.CENTER);
        body.add(recentEmpty, BorderLayout.SOUTH);
        card.setBody(body);
        return card;
    }

    private Card createEquationCard() {
        Card card = new Card("Accounting equation", "Assets = Liabilities + Owner's equity");
        card.addToHeader(equationBadge);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(16, 22, 20, 22));

        body.add(barRow("Assets", assetsAmount));
        body.add(Box.createVerticalStrut(8));
        body.add(assetsBar);
        body.add(Box.createVerticalStrut(16));
        body.add(barRow("Liabilities + Owner's equity", rightSideAmount));
        body.add(Box.createVerticalStrut(8));
        body.add(rightSideBar);
        body.add(Box.createVerticalStrut(10));

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        legend.setOpaque(false);
        legend.setAlignmentX(Component.LEFT_ALIGNMENT);
        styleLegend(liabilitiesLegend, Theme.YELLOW);
        styleLegend(equityLegend, Theme.GREEN_BAR);
        legend.add(liabilitiesLegend);
        legend.add(Box.createHorizontalStrut(18));
        legend.add(equityLegend);
        body.add(legend);

        card.setBody(body);
        return card;
    }

    private Card createQuickActionsCard() {
        Card card = new Card("Quick actions", null);
        JPanel list = new JPanel(new GridLayout(4, 1, 0, 2));
        list.setOpaque(false);
        list.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        list.add(new QuickAction(Icons.PEN, Theme.PURPLE_TINT, Theme.PURPLE, "Record a journal entry",
                "Debit one account, credit another", () -> mainFrame.showPage(MainFrame.JOURNAL_ENTRY)));
        list.add(new QuickAction(Icons.PLUS, Theme.AMBER_BG, Theme.AMBER, "Add an account",
                "Grow your chart of accounts", () -> mainFrame.showPage(MainFrame.CHART_OF_ACCOUNTS)));
        list.add(new QuickAction(Icons.SLIDERS, Theme.GREEN_BG, Theme.GREEN, "Add an adjustment",
                "Adjust balances at period end", () -> mainFrame.openAddAdjustment()));
        list.add(new QuickAction(Icons.LAYERS, Theme.BLUE_BG, Theme.BLUE, "Open the general ledger",
                "Running balance per account", () -> mainFrame.showPage(MainFrame.GENERAL_LEDGER)));
        card.setBody(list);
        return card;
    }

    private Card createTipCard() {
        Card card = Card.plain();
        card.setFill(Theme.CREAM).setBorderColor(Theme.CREAM_BORDER);

        JLabel title = new JLabel("Tip");
        title.setFont(Theme.bold(16));
        title.setForeground(Theme.TITLE);
        tipText.setFont(Theme.regular(14.5f));
        tipText.setForeground(Theme.TEXT_SOFT);

        JPanel texts = new JPanel(new BorderLayout(0, 2));
        texts.setOpaque(false);
        texts.add(title, BorderLayout.NORTH);
        texts.add(tipText, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(12, 0));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JPanel iconHolder = new JPanel(new BorderLayout());
        iconHolder.setOpaque(false);
        iconHolder.add(new IconBox(Icons.BULB, 40, Theme.YELLOW, Theme.PURPLE_DEEP), BorderLayout.NORTH);
        body.add(iconHolder, BorderLayout.WEST);
        body.add(texts, BorderLayout.CENTER);
        card.setBody(body);
        return card;
    }

    // =================================================================
    //  Loading data
    // =================================================================

    @Override
    public void refreshData() {
        try {
            List<Account> accounts = accountDAO.getAllAccounts();
            List<JournalEntry> entries = journalDAO.getAllEntries();
            DashboardSummary s = accounting.buildSummary(accounts, entries);

            // tiles
            assetsCard.setValue(Money.peso(s.totalAssets));
            liabilitiesCard.setValue(Money.peso(s.totalLiabilities));
            revenueCard.setValue(Money.peso(s.totalRevenue));
            expensesCard.setValue(Money.peso(s.totalExpenses));
            netIncomeCard.setValue(Money.peso(s.netIncome));
            netIncomeCard.setNote(s.netIncome.signum() < 0 ? "Net loss: expenses are higher" : "Revenue minus expenses");
            entriesCard.setValue(String.valueOf(s.entryCount));
            updateTileTips(s, entries);

            // header line
            List<JournalEntry> sorted = accounting.sortByDate(entries);
            if (sorted.isEmpty()) {
                header.setDescription("Your books at a glance, based on all posted entries. No entries posted yet.");
            } else {
                JournalEntry last = sorted.get(sorted.size() - 1);
                header.setDescription("Your books at a glance, based on all posted entries. Latest entry dated "
                        + DateUtil.toLong(last.getDate()) + ".");
            }

            // recent entries: newest 5
            recentModel.clear();
            for (int i = sorted.size() - 1; i >= 0 && i >= sorted.size() - 5; i--) {
                JournalEntry e = sorted.get(i);
                recentModel.addRow(new Object[] {
                        DateUtil.toLong(e.getDate()), e.getReferenceNo(), e.getDescription(),
                        Money.format(e.getTotalDebit())});
            }
            recentEmpty.setVisible(sorted.isEmpty());

            updateEquation(s);
            updateTip(s);
            mainFrame.setDatabaseStatus(true);
            revalidate();
            repaint();
        } catch (SQLException e) {
            mainFrame.setDatabaseStatus(false);
            showError("Could not load the dashboard from MySQL.", e);
        }
    }

    /** The explanations shown when the mouse rests on a tile */
    private void updateTileTips(DashboardSummary s, List<JournalEntry> entries) {
        int adjusting = 0;
        for (JournalEntry e : entries) {
            if (e.isAdjusting()) {
                adjusting++;
            }
        }
        assetsCard.setTip("<b>Total assets</b><br>All asset accounts: debits - credits.<br>"
                + "Contra assets (Accumulated Depreciation) reduce it.<br>"
                + "= Liabilities + Owner's equity = " + Money.peso(s.totalLiabilities.add(s.totalEquity)));
        liabilitiesCard.setTip("<b>Total liabilities</b><br>All liability accounts: credits - debits.<br>"
                + "Debts the business still has to pay.");
        revenueCard.setTip("<b>Total revenue</b><br>All revenue accounts: credits - debits.");
        expensesCard.setTip("<b>Total expenses</b><br>All expense accounts: debits - credits.");
        netIncomeCard.setTip("<b>" + (s.netIncome.signum() < 0 ? "Net loss" : "Net income") + "</b><br>"
                + "Revenue - Expenses<br>= " + Money.peso(s.totalRevenue) + " - " + Money.peso(s.totalExpenses)
                + "<br>= " + Money.peso(s.netIncome));
        entriesCard.setTip("<b>Posted entries</b><br>" + (s.entryCount - adjusting) + " regular + "
                + adjusting + " adjusting " + (adjusting == 1 ? "entry" : "entries"));
    }

    private void updateEquation(DashboardSummary s) {
        BigDecimal rightSide = s.totalLiabilities.add(s.totalEquity);
        assetsAmount.setText(Money.peso(s.totalAssets));
        rightSideAmount.setText(Money.peso(rightSide));

        // bar lengths are shares of the larger side
        double biggest = Math.max(Math.max(s.totalAssets.doubleValue(), rightSide.doubleValue()), 1);
        assetsBar.setParts(new double[] {Math.max(0, s.totalAssets.doubleValue()) / biggest},
                new Color[] {Theme.PURPLE_BAR});
        rightSideBar.setParts(new double[] {
                        Math.max(0, s.totalLiabilities.doubleValue()) / biggest,
                        Math.max(0, s.totalEquity.doubleValue()) / biggest},
                new Color[] {Theme.YELLOW, Theme.GREEN_BAR});

        liabilitiesLegend.setText("Liabilities " + Money.peso(s.totalLiabilities));
        equityLegend.setText("Owner's equity " + Money.peso(s.totalEquity));

        if (s.isEquationBalanced()) {
            equationBadge.showOk("Balanced");
        } else {
            equationBadge.showBad("Not balanced");
        }
    }

    private void updateTip(DashboardSummary s) {
        String text;
        if (s.entryCount == 0) {
            text = "Start by recording your first transaction in Journal Entry, for example the owner's investment.";
        } else if (!s.abnormalAccounts.isEmpty()) {
            int n = s.abnormalAccounts.size();
            text = (n == 1 ? s.abnormalAccounts.get(0).getName() + " shows" : n + " accounts show")
                    + " an abnormal balance (for example, an asset with a credit balance)."
                    + " Open the General Ledger to check those entries.";
        } else {
            text = "Every account has a normal balance. Check the 12 Columnar to see your financial statements.";
        }
        tipText.setText(text);
    }

    /** Reset data: deletes every journal entry after the user confirms */
    private void resetData() {
        boolean yes = confirm("Reset data",
                "Delete ALL journal entries and adjustments?\n"
                + "Your chart of accounts stays. This cannot be undone.",
                "Delete all entries", "Keep my data");
        if (!yes) {
            return;
        }
        try {
            journalDAO.deleteAllEntries();
            refreshData();
            showInfo("All entries were deleted. You can start fresh.");
        } catch (SQLException e) {
            showError("Could not delete the entries.", e);
        }
    }

    // =================================================================
    //  Small helpers and inner components
    // =================================================================

    private static JLabel amountLabel() {
        JLabel label = new JLabel();
        label.setFont(Theme.bold(15.5f));
        label.setForeground(Theme.TITLE);
        return label;
    }

    private JComponent barRow(String text, JLabel amount) {
        JLabel name = new JLabel(text);
        name.setFont(Theme.regular(15.5f));
        name.setForeground(Theme.TEXT);
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(name, BorderLayout.WEST);
        row.add(amount, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        return row;
    }

    private void styleLegend(JLabel label, final Color color) {
        label.setFont(Theme.regular(14));
        label.setForeground(Theme.TEXT_SOFT);
        label.setIcon(new javax.swing.Icon() {     // a small colored square before the text
            public int getIconWidth() { return 11; }
            public int getIconHeight() { return 11; }
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(color);
                g.fillRoundRect(x, y, 11, 11, 4, 4);
            }
        });
        label.setIconTextGap(6);
    }

    private JButton linkButton(String text) {
        JButton link = new JButton(text);
        link.setFont(Theme.bold(15));
        link.setForeground(Theme.PURPLE);
        link.setContentAreaFilled(false);
        link.setBorderPainted(false);
        link.setFocusPainted(false);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return link;
    }

    /** A rounded progress-style bar split into colored parts (fractions 0..1) */
    private static class EquationBar extends JComponent {
        private double[] parts = {0};
        private Color[] colors = {Theme.PURPLE_BAR};

        EquationBar() {
            setPreferredSize(new Dimension(100, 14));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        void setParts(double[] parts, Color[] colors) {
            this.parts = parts;
            this.colors = colors;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setClip(new java.awt.geom.RoundRectangle2D.Double(0, 0, w, h, h, h));
            g2.setColor(new Color(0xf1edf8));
            g2.fillRect(0, 0, w, h);
            int x = 0;
            for (int i = 0; i < parts.length; i++) {
                int partWidth = (int) Math.round(parts[i] * w);
                g2.setColor(colors[i]);
                g2.fillRect(x, 0, partWidth, h);
                x += partWidth;
            }
            g2.dispose();
        }
    }

    /** One clickable row in the Quick actions card */
    private static class QuickAction extends JButton {
        QuickAction(String icon, Color iconBg, Color iconColor, String title, String note, Runnable action) {
            setLayout(new BorderLayout(12, 0));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            getAccessibleContext().setAccessibleName(title);

            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(Theme.bold(16));
            titleLabel.setForeground(Theme.TITLE);
            JLabel noteLabel = new JLabel(note);
            noteLabel.setFont(Theme.regular(14));
            noteLabel.setForeground(Theme.MUTED);
            JPanel texts = new JPanel(new GridLayout(2, 1));
            texts.setOpaque(false);
            texts.add(titleLabel);
            texts.add(noteLabel);

            add(new IconBox(icon, 40, iconBg, iconColor), BorderLayout.WEST);
            add(texts, BorderLayout.CENTER);
            add(new JLabel(Icons.get(Icons.CHEVRON, 16, new Color(0xb3a8cc))), BorderLayout.EAST);
            addActionListener(e -> action.run());
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, 62);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (getModel().isRollover() || isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.PURPLE_HOVER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 26, 26);
                g2.dispose();
            }
        }
    }
}
