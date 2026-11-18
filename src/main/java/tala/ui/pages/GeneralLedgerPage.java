package tala.ui.pages;

import tala.dao.AccountDAO;
import tala.dao.JournalDAO;
import tala.model.Account;
import tala.model.JournalEntry;
import tala.model.LedgerRow;
import tala.service.AccountingService;
import tala.ui.MainFrame;
import tala.ui.components.AppButton;
import tala.ui.components.Badge;
import tala.ui.components.Card;
import tala.ui.components.Icons;
import tala.ui.components.PageHeader;
import tala.ui.components.ReadOnlyTableModel;
import tala.ui.components.RoundedPanel;
import tala.ui.components.TalaTable;
import tala.ui.theme.Theme;
import tala.util.DateUtil;
import tala.util.Money;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * GeneralLedgerPage
 * ------------------------------------------------------------
 * Left: every account that has been used, with its balance.
 * Right: the chosen account's ledger: each posting, the running
 * balance, the totals and the ending balance.
 *
 * A balance on the "wrong" side (e.g. Cash with a CR balance)
 * is flagged as an ABNORMAL balance so you can check the entries.
 */
public class GeneralLedgerPage extends BasePage {

    private final Card accountsCard = new Card("Accounts used", "");
    private final JPanel accountList = new JPanel();
    private final Card ledgerCard = new Card("", "");
    private final Badge abnormalBadge = Badge.warn("Abnormal balance: check these entries");
    private final JLabel totalDebitTile = tileValue(), totalCreditTile = tileValue(), endingTile = tileValue();

    private final ReadOnlyTableModel model = new ReadOnlyTableModel("Date", "Description", "Ref", "Debit", "Credit", "Balance");
    private final TalaTable table;
    private final JLabel emptyLabel = new JLabel("No accounts have been used yet. Post a journal entry first.", JLabel.CENTER);

    private List<Account> usedAccounts = new ArrayList<>();
    private List<JournalEntry> entries = new ArrayList<>();
    private int selectedAccountId = -1;
    private boolean selectedIsAbnormal = false;

    public GeneralLedgerPage(MainFrame mainFrame, AccountDAO accountDAO, JournalDAO journalDAO) {
        super(mainFrame, accountDAO, journalDAO);

        table = new TalaTable(model) {
            private boolean isTotalRow(int row) {
                return row == getRowCount() - 1 && getRowCount() > 0;
            }
            @Override protected boolean rowBold(int row) { return isTotalRow(row); }
            @Override protected Color rowBackground(int row, boolean selected) {
                return isTotalRow(row) ? Theme.CREAM : Color.WHITE;
            }
            @Override protected Color cellForeground(int row, int column) {
                return column == 5 && selectedIsAbnormal ? Theme.AMBER : Theme.TEXT;
            }
        };
        table.setRightAligned(3, 4, 5);
        table.setPillColumns(2);
        table.setColumnWidths(90, 0, 125, 125, 125, 165);
        table.setRowSelectionAllowed(false);

        PageHeader header = new PageHeader(Icons.LAYERS, "General Ledger",
                "One ledger per account you have used: every transaction on that account, "
                + "its running balance, total and ending balance. Pick an account on the left.");
        AppButton refresh = new AppButton("Refresh", Icons.get(Icons.REFRESH, 16, Theme.PURPLE_DEEP), AppButton.Style.SECONDARY);
        refresh.addActionListener(e -> refreshData());
        header.addAction(refresh);
        addSection(header);

        // left list
        accountList.setOpaque(false);
        accountList.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        emptyLabel.setFont(Theme.regular(15));
        emptyLabel.setForeground(Theme.MUTED);
        emptyLabel.setBorder(BorderFactory.createEmptyBorder(26, 16, 26, 16));
        JPanel listBody = new JPanel(new BorderLayout());
        listBody.setOpaque(false);
        listBody.add(accountList, BorderLayout.NORTH);
        listBody.add(emptyLabel, BorderLayout.CENTER);
        accountsCard.setBody(listBody);

        // right ledger
        ledgerCard.addToHeader(abnormalBadge);
        JPanel tiles = new JPanel(new GridLayout(1, 3, 12, 0));
        tiles.setOpaque(false);
        tiles.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        tiles.add(tile("Total debits", totalDebitTile));
        tiles.add(tile("Total credits", totalCreditTile));
        tiles.add(tile("Ending balance", endingTile));

        JPanel ledgerBody = new JPanel(new BorderLayout());
        ledgerBody.setOpaque(false);
        ledgerBody.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        ledgerBody.add(tiles, BorderLayout.NORTH);
        ledgerBody.add(table.inPanel(), BorderLayout.CENTER);
        ledgerCard.setBody(ledgerBody);

        addSection(twoColumns(accountsCard, 0.3, ledgerCard, 0.7));
        finishSections();
    }

    @Override
    public void refreshData() {
        try {
            entries = journalDAO.getAllEntries();
            usedAccounts = accounting.getUsedAccounts(accountDAO.getAllAccounts(), entries);

            // keep the same account selected if it still exists
            boolean stillThere = false;
            for (Account a : usedAccounts) {
                if (a.getId() == selectedAccountId) {
                    stillThere = true;
                }
            }
            if (!stillThere) {
                selectedAccountId = usedAccounts.isEmpty() ? -1 : usedAccounts.get(0).getId();
            }
            buildAccountList();
            showSelectedLedger();
        } catch (SQLException e) {
            showError("Could not load the ledger from MySQL.", e);
        }
    }

    /** Rebuilds the clickable list on the left */
    private void buildAccountList() {
        accountList.removeAll();
        accountList.setLayout(new GridLayout(0, 1, 0, 2));
        int abnormalCount = 0;

        for (Account a : usedAccounts) {
            BigDecimal balance = accounting.getBalance(a, entries);
            boolean abnormal = balance.signum() < 0;
            if (abnormal) {
                abnormalCount++;
            }
            String balanceText = Money.peso(balance.abs()) + " " + AccountingService.sideOf(a, balance);
            AccountButton button = new AccountButton(a, balanceText, abnormal, a.getId() == selectedAccountId);
            button.addActionListener(e -> {
                selectedAccountId = a.getId();
                buildAccountList();
                showSelectedLedger();
            });
            accountList.add(button);
        }

        String sub = usedAccounts.size() + (usedAccounts.size() == 1 ? " account" : " accounts");
        if (abnormalCount > 0) {
            sub += " - " + abnormalCount + " need a look";
        }
        accountsCard.setSubtitle(sub);
        emptyLabel.setVisible(usedAccounts.isEmpty());
        accountList.revalidate();
        accountList.repaint();
    }

    /** Fills the right card with the selected account's ledger */
    private void showSelectedLedger() {
        model.clear();
        Account account = null;
        for (Account a : usedAccounts) {
            if (a.getId() == selectedAccountId) {
                account = a;
            }
        }
        if (account == null) {
            ledgerCard.setTitle("No account selected");
            ledgerCard.setSubtitle("Post a journal entry and its accounts appear here.");
            abnormalBadge.setVisible(false);
            totalDebitTile.setText(Money.peso(BigDecimal.ZERO));
            totalCreditTile.setText(Money.peso(BigDecimal.ZERO));
            endingTile.setText(Money.peso(BigDecimal.ZERO));
            return;
        }

        List<LedgerRow> rows = accounting.buildLedger(account, entries);
        BigDecimal totalDebit = BigDecimal.ZERO, totalCredit = BigDecimal.ZERO, ending = BigDecimal.ZERO;
        for (LedgerRow r : rows) {
            totalDebit = totalDebit.add(r.getDebit());
            totalCredit = totalCredit.add(r.getCredit());
            ending = r.getRunningBalance();
            model.addRow(new Object[] {
                    DateUtil.toShort(r.getDate()), r.getDescription(), r.getReferenceNo(),
                    Money.formatOrBlank(r.getDebit()), Money.formatOrBlank(r.getCredit()),
                    Money.format(r.getRunningBalance().abs()) + " " + AccountingService.sideOf(account, r.getRunningBalance())});
        }
        String endingText = Money.peso(ending.abs()) + " " + AccountingService.sideOf(account, ending);
        model.addRow(new Object[] {"Total", "Ending balance", "", Money.format(totalDebit), Money.format(totalCredit), endingText});

        selectedIsAbnormal = ending.signum() < 0;
        ledgerCard.setTitle(account.getName());
        ledgerCard.setSubtitle("Account " + account.getCode() + " - " + account.getType().getLabel()
                + " - Normal balance: " + account.getNormalBalance());
        abnormalBadge.setVisible(selectedIsAbnormal);
        totalDebitTile.setText(Money.peso(totalDebit));
        totalCreditTile.setText(Money.peso(totalCredit));
        endingTile.setText(endingText);
        endingTile.setForeground(selectedIsAbnormal ? Theme.AMBER : Theme.TITLE);
        revalidate();
        repaint();
    }

    // ------------------------------------------------------------ small parts

    private static JLabel tileValue() {
        JLabel label = new JLabel();
        label.setFont(Theme.bold(20));
        label.setForeground(Theme.TITLE);
        return label;
    }

    private JPanel tile(String title, JLabel value) {
        RoundedPanel tile = new RoundedPanel(new BorderLayout(0, 2));
        tile.setRadius(12).setFill(Theme.PURPLE_HOVER).setBorderColor(Theme.DIVIDER).setShadow(false);
        tile.setBorder(BorderFactory.createEmptyBorder(10, 14, 12, 14));
        JLabel label = new JLabel(title);
        label.setFont(Theme.regular(13.5f));
        label.setForeground(Theme.MUTED);
        tile.add(label, BorderLayout.NORTH);
        tile.add(value, BorderLayout.CENTER);
        return tile;
    }

    /** One account in the left list: name, code, balance (and a warning if abnormal) */
    private static class AccountButton extends JButton {
        private final boolean selected;

        AccountButton(Account a, String balance, boolean abnormal, boolean selected) {
            this.selected = selected;
            setLayout(new BorderLayout(10, 0));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            getAccessibleContext().setAccessibleName(a.getName() + ", balance " + balance);

            JLabel name = new JLabel(a.getName());
            name.setFont(Theme.bold(15.5f));
            name.setForeground(Theme.TITLE);
            JLabel meta = new JLabel(a.getCode() + " - " + a.getType().getLabel());
            meta.setFont(Theme.regular(13.5f));
            meta.setForeground(Theme.MUTED);
            JPanel left = new JPanel(new GridLayout(2, 1));
            left.setOpaque(false);
            left.add(name);
            left.add(meta);

            JLabel amount = new JLabel(balance, JLabel.RIGHT);
            amount.setFont(Theme.bold(15));
            amount.setForeground(abnormal ? Theme.AMBER : Theme.TITLE);
            JLabel warning = new JLabel(abnormal ? "Abnormal balance" : " ", JLabel.RIGHT);
            warning.setFont(Theme.bold(12.5f));
            warning.setForeground(Theme.AMBER);
            JPanel right = new JPanel(new GridLayout(2, 1));
            right.setOpaque(false);
            right.add(amount);
            right.add(warning);

            add(left, BorderLayout.CENTER);
            add(right, BorderLayout.EAST);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, 60);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (selected) {
                g2.setColor(Theme.CREAM);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.setColor(Theme.CREAM_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
            } else if (getModel().isRollover() || isFocusOwner()) {
                g2.setColor(Theme.PURPLE_HOVER);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
            }
            g2.dispose();
        }
    }
}
