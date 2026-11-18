package tala.ui.pages;

import tala.dao.AccountDAO;
import tala.dao.JournalDAO;
import tala.model.JournalEntry;
import tala.model.JournalLine;
import tala.ui.MainFrame;
import tala.ui.components.AppButton;
import tala.ui.components.Badge;
import tala.ui.components.Card;
import tala.ui.components.FormField;
import tala.ui.components.Icons;
import tala.ui.components.PageHeader;
import tala.ui.components.ReadOnlyTableModel;
import tala.ui.components.TalaTable;
import tala.ui.theme.Theme;
import tala.util.DateUtil;
import tala.util.Money;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * GeneralJournalPage
 * ------------------------------------------------------------
 * The classic journal layout, oldest first:
 *
 *   Date    Particulars              Post ref   Debit       Credit
 *   Nov 1   Cash                     1010       100,000.00
 *              Owner's Capital       3010                   100,000.00
 *           (Owner invested cash)  JE-000001
 */
public class GeneralJournalPage extends BasePage {

    // Kinds of table rows (decide how each row is drawn)
    private static final String DEBIT_ROW = "debit", CREDIT_ROW = "credit", MEMO_ROW = "memo", TOTAL_ROW = "total";

    private final ReadOnlyTableModel model = new ReadOnlyTableModel("Date", "Particulars", "Post ref", "Debit", "Credit");
    private final List<String> rowKinds = new ArrayList<>();
    private final TalaTable table;

    private final JTextField searchField = new JTextField();
    private final JComboBox<String> monthCombo = new JComboBox<>();
    private final Card card = new Card("Journal entries", "");
    private final JLabel entriesLabel = footLabel(), debitLabel = footLabel(), creditLabel = footLabel();
    private final Badge balanceBadge = Badge.ok("In balance");
    private final JLabel emptyLabel = new JLabel("No journal entries match. Post one in Journal Entry.", JLabel.CENTER);

    private List<JournalEntry> allEntries = new ArrayList<>();
    private boolean loading = false;   // stops the month box from reloading while we fill it

    public GeneralJournalPage(MainFrame mainFrame, AccountDAO accountDAO, JournalDAO journalDAO) {
        super(mainFrame, accountDAO, journalDAO);

        // The table: memo rows in grey italics, credit lines indented, total row bold on cream
        table = new TalaTable(model) {
            @Override protected boolean rowBold(int row) {
                return kind(row).equals(TOTAL_ROW) || (kind(row).equals(DEBIT_ROW));
            }
            @Override protected Color rowBackground(int row, boolean selected) {
                return kind(row).equals(TOTAL_ROW) ? Theme.CREAM : Color.WHITE;
            }
            @Override protected Color cellForeground(int row, int column) {
                return kind(row).equals(MEMO_ROW) ? Theme.MUTED : Theme.TEXT;
            }
            @Override protected int cellIndent(int row, int column) {
                if (column != 1) return 0;
                if (kind(row).equals(CREDIT_ROW)) return 40;   // credits are indented
                if (kind(row).equals(MEMO_ROW)) return 20;
                return 0;
            }
        };
        table.setRightAligned(3, 4);
        table.setColumnWidths(120, 0, 110, 170, 170);
        table.setRowSelectionAllowed(false);

        PageHeader header = new PageHeader(Icons.BOOK, "General Journal",
                "Every entry in date order: date, particulars, post reference, debit and credit.");
        AppButton refresh = new AppButton("Refresh", Icons.get(Icons.REFRESH, 16, Theme.PURPLE_DEEP), AppButton.Style.SECONDARY);
        refresh.addActionListener(e -> refreshData());
        header.addAction(refresh);
        addSection(header);

        // search + month filter in the card header
        Theme.styleField(searchField);
        searchField.setPreferredSize(new Dimension(240, 46));
        Theme.styleCombo(monthCombo);
        monthCombo.setPreferredSize(new Dimension(190, 46));
        card.addToHeader(new FormField("Search", searchField, null));
        card.addToHeader(new FormField("Month", monthCombo, null));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { fillTable(); }
            public void removeUpdate(DocumentEvent e)  { fillTable(); }
            public void changedUpdate(DocumentEvent e) { fillTable(); }
        });
        monthCombo.addActionListener(e -> { if (!loading) fillTable(); });

        emptyLabel.setFont(Theme.regular(15));
        emptyLabel.setForeground(Theme.MUTED);
        emptyLabel.setBorder(BorderFactory.createEmptyBorder(26, 20, 26, 20));

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(table.inPanel(), BorderLayout.CENTER);
        body.add(emptyLabel, BorderLayout.SOUTH);
        card.setBody(body);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JPanel totals = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        totals.setOpaque(false);
        totals.add(entriesLabel);
        totals.add(debitLabel);
        totals.add(creditLabel);
        footer.add(totals, BorderLayout.WEST);
        footer.add(balanceBadge, BorderLayout.EAST);
        card.setFooter(footer);

        addSection(card);
        finishSections();
    }

    private String kind(int row) {
        return row < rowKinds.size() ? rowKinds.get(row) : DEBIT_ROW;
    }

    @Override
    public void refreshData() {
        try {
            allEntries = accounting.sortByDate(journalDAO.getAllEntries());

            // Month choices: "All months" + every month that has entries (newest first)
            loading = true;
            Object chosen = monthCombo.getSelectedItem();
            Set<String> months = new LinkedHashSet<>();
            for (int i = allEntries.size() - 1; i >= 0; i--) {
                months.add(DateUtil.toMonth(allEntries.get(i).getDate()));
            }
            monthCombo.removeAllItems();
            monthCombo.addItem("All months");
            for (String m : months) {
                monthCombo.addItem(m);
            }
            monthCombo.setSelectedItem(chosen != null && months.contains(chosen) ? chosen : "All months");
            loading = false;

            fillTable();
        } catch (SQLException e) {
            loading = false;
            showError("Could not load the journal from MySQL.", e);
        }
    }

    /** Rebuilds the table rows using the search text and the chosen month */
    private void fillTable() {
        String search = searchField.getText().trim().toLowerCase();
        String month = (String) monthCombo.getSelectedItem();

        model.clear();
        rowKinds.clear();
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        int count = 0;

        for (JournalEntry e : allEntries) {
            boolean monthMatches = month == null || month.equals("All months") || DateUtil.toMonth(e.getDate()).equals(month);
            boolean searchMatches = search.isEmpty()
                    || e.getDescription().toLowerCase().contains(search)
                    || e.getReferenceNo().toLowerCase().contains(search);
            if (!monthMatches || !searchMatches) {
                continue;
            }
            count++;

            boolean firstLine = true;
            for (JournalLine line : e.getLines()) {
                String date = firstLine ? DateUtil.toShort(e.getDate()) : "";
                String code = String.valueOf(line.getAccount().getCode());
                if (line.isDebit()) {
                    model.addRow(new Object[] {date, line.getAccount().getName(), code, Money.peso(line.getDebit()), ""});
                    rowKinds.add(DEBIT_ROW);
                } else {
                    model.addRow(new Object[] {date, line.getAccount().getName(), code, "", Money.peso(line.getCredit())});
                    rowKinds.add(CREDIT_ROW);
                }
                firstLine = false;
            }
            String memo = "(" + e.getDescription() + ")   " + e.getReferenceNo()
                    + (e.isAdjusting() ? "   - adjusting entry" : "");
            model.addRow(new Object[] {"", memo, "", "", ""});
            rowKinds.add(MEMO_ROW);

            totalDebit = totalDebit.add(e.getTotalDebit());
            totalCredit = totalCredit.add(e.getTotalCredit());
        }

        if (count > 0) {
            model.addRow(new Object[] {"Total", "", "", Money.peso(totalDebit), Money.peso(totalCredit)});
            rowKinds.add(TOTAL_ROW);
        }
        emptyLabel.setVisible(count == 0);

        card.setSubtitle((month == null ? "All months" : month) + " - " + count + (count == 1 ? " entry" : " entries"));
        entriesLabel.setText("<html>Entries <b>" + count + "</b></html>");
        debitLabel.setText("<html>Total debit <b>" + Money.peso(totalDebit) + "</b></html>");
        creditLabel.setText("<html>Total credit <b>" + Money.peso(totalCredit) + "</b></html>");
        if (totalDebit.compareTo(totalCredit) == 0) {
            balanceBadge.showOk("In balance");
        } else {
            balanceBadge.showBad("Out of balance by " + Money.peso(totalDebit.subtract(totalCredit).abs()));
        }
        revalidate();
        repaint();
    }

    private static JLabel footLabel() {
        JLabel label = new JLabel();
        label.setFont(Theme.regular(14.5f));
        label.setForeground(Theme.TEXT_SOFT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 24));
        return label;
    }
}
