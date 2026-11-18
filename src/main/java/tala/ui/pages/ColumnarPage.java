package tala.ui.pages;

import tala.dao.AccountDAO;
import tala.dao.JournalDAO;
import tala.model.Account;
import tala.model.JournalEntry;
import tala.model.Worksheet;
import tala.model.WorksheetRow;
import tala.ui.MainFrame;
import tala.ui.components.AppButton;
import tala.ui.components.Badge;
import tala.ui.components.Card;
import tala.ui.components.IconBox;
import tala.ui.components.Icons;
import tala.ui.components.PageHeader;
import tala.ui.components.ReadOnlyTableModel;
import tala.ui.components.RoundedPanel;
import tala.ui.components.TalaTable;
import tala.ui.theme.Theme;
import tala.util.Money;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static tala.model.WorksheetRow.*;

/**
 * ColumnarPage  (the 12 Columnar worksheet)
 * ------------------------------------------------------------
 * Unadjusted TB | Adjustments | Adjusted TB | Income statement | Owner's equity | Balance sheet
 *   (each with a Dr and a Cr column = 12 money columns)
 *
 * It starts EMPTY and lists only the accounts that were actually used.
 * The math is done by AccountingService.buildWorksheet().
 */
public class ColumnarPage extends BasePage {

    private static final String[] GROUPS = {"Unadjusted", "Adjustments", "Adjusted", "Income statement", "Owner's equity", "Balance sheet"};
    private static final Color[] GROUP_COLORS = {
        Theme.PURPLE_TINT, Theme.AMBER_BG, Theme.PURPLE_TINT, Theme.GREEN_BG, Theme.BLUE_BG, new Color(0xfce7f3)};

    private final ReadOnlyTableModel model;
    private final TalaTable table;
    private final List<String> rowKinds = new ArrayList<>();
    private Worksheet worksheet = new Worksheet();

    private final JLabel tbTile = tileValue(), adjTile = tileValue(), niTile = tileValue(), diffTile = tileValue();
    private final JLabel niTileTitle = new JLabel("Net income");
    private RoundedPanel tbCard, adjCard, niCard, diffCard;   // the four tiles (for their tooltips)
    private final JLabel footNet = new JLabel(), footDiff = new JLabel();
    private final Badge balanceBadge = Badge.ok("Balanced");
    private final JLabel emptyLabel = new JLabel(
            "The worksheet starts empty. Post a journal entry and its accounts appear here.", JLabel.CENTER);

    public ColumnarPage(MainFrame mainFrame, AccountDAO accountDAO, JournalDAO journalDAO) {
        super(mainFrame, accountDAO, journalDAO);

        // 14 columns: Code, Account, then Dr/Cr for each of the 6 groups
        String[] columns = new String[2 + CELL_COUNT];
        columns[0] = "Code";
        columns[1] = "Account";
        for (int c = 0; c < CELL_COUNT; c++) {
            columns[2 + c] = (c % 2 == 0) ? "Dr" : "Cr";
        }
        model = new ReadOnlyTableModel(columns);

        table = new TalaTable(model) {
            @Override protected boolean rowBold(int row) {
                return !kind(row).equals(ACCOUNT);
            }
            @Override protected Color cellBackground(int row, int column, boolean selected) {
                String kind = kind(row);
                if (kind.equals(GRAND_TOTAL) && column >= 2 + BS_DR) {
                    return new Color(0xdcfce7);      // the two 0.00 cells at the very end
                }
                if (kind.equals(TOTAL) || kind.equals(GRAND_TOTAL)) {
                    return Theme.CREAM;
                }
                return selected ? Theme.CREAM : Color.WHITE;
            }
            @Override protected Color cellForeground(int row, int column) {
                if (kind(row).equals(GRAND_TOTAL) && column >= 2 + BS_DR) {
                    return worksheet.isBalanced() ? new Color(0x166534) : Theme.RED;
                }
                return Theme.TEXT;
            }
            // fit the window when there is room; otherwise scroll sideways
            @Override public boolean getScrollableTracksViewportWidth() {
                return getParent() != null && getPreferredSize().width < getParent().getWidth();
            }
            // we place our own 2-row header in the scroll pane; stop JTable from replacing it
            @Override protected void configureEnclosingScrollPane() { }
        };
        table.setCompact(14.5f, 6);
        table.setRowHeight(36);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        int[] rightColumns = new int[CELL_COUNT];
        for (int c = 0; c < CELL_COUNT; c++) {
            rightColumns[c] = 2 + c;
            table.getColumnModel().getColumn(2 + c).setPreferredWidth(88);
            table.getColumnModel().getColumn(2 + c).setMinWidth(84);
        }
        table.setRightAligned(rightColumns);
        table.getColumnModel().getColumn(0).setPreferredWidth(56);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setMinWidth(160);
        table.setRowSelectionAllowed(false);

        // ---------- header with actions ----------
        PageHeader header = new PageHeader(Icons.TABLE, "12 Columnar",
                "Unadjusted trial balance, adjustments, adjusted trial balance, income statement, "
                + "owner's equity and balance sheet. Amounts in " + Money.PESO_SIGN.trim() + ".");
        AppButton csv = new AppButton("Download CSV", Icons.get(Icons.DOWNLOAD, 16, Theme.PURPLE_DEEP), AppButton.Style.SECONDARY);
        csv.addActionListener(e -> exportCsv());
        AppButton refresh = new AppButton("Refresh", Icons.get(Icons.REFRESH, 16, Theme.PURPLE_DEEP), AppButton.Style.SECONDARY);
        refresh.addActionListener(e -> refreshData());
        AppButton addAdjustment = new AppButton("Add adjustment", Icons.get(Icons.PLUS, 16, Color.WHITE), AppButton.Style.PRIMARY);
        addAdjustment.addActionListener(e -> openAdjustmentDialog());
        header.addAction(csv);
        header.addAction(refresh);
        header.addAction(addAdjustment);
        addSection(header);

        // ---------- four tiles ----------
        JPanel tiles = new JPanel(new GridLayout(1, 4, 18, 0));
        tiles.setOpaque(false);
        // each tile has its own soft color: { tile fill, tile border, icon box }
        tbCard = statTile(Icons.SCALE, Theme.PURPLE,
                new Color[] {new Color(0xf6f2fe), new Color(0xe5dcf8), new Color(0xebe2fc)}, new JLabel("Trial balance"), tbTile);
        adjCard = statTile(Icons.SLIDERS, Theme.AMBER,
                new Color[] {Theme.CREAM, Theme.CREAM_BORDER, new Color(0xfdeaa3)}, new JLabel("Adjustments"), adjTile);
        niCard = statTile(Icons.PESO, Theme.GREEN,
                new Color[] {new Color(0xeefaf2), new Color(0xcfeedb), new Color(0xd5f2df)}, niTileTitle, niTile);
        diffCard = statTile(Icons.LAYERS, Theme.BLUE,
                new Color[] {new Color(0xeef4fe), new Color(0xd5e2fb), new Color(0xdbe7fd)}, new JLabel("Balance sheet difference"), diffTile);
        tiles.add(tbCard);
        tiles.add(adjCard);
        tiles.add(niCard);
        tiles.add(diffCard);
        addSection(tiles);

        // ---------- worksheet card ----------
        Card card = Card.plain();
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(1, 1, 0, 1));
        body.add(createWorksheetScroll(), BorderLayout.CENTER);
        emptyLabel.setFont(Theme.regular(15));
        emptyLabel.setForeground(Theme.MUTED);
        emptyLabel.setBorder(BorderFactory.createEmptyBorder(26, 20, 26, 20));
        body.add(emptyLabel, BorderLayout.SOUTH);
        card.setBody(body);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JPanel numbers = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        numbers.setOpaque(false);
        for (JLabel l : new JLabel[] {footNet, footDiff}) {
            l.setFont(Theme.regular(14.5f));
            l.setForeground(Theme.TEXT_SOFT);
            l.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 24));
            numbers.add(l);
        }
        footer.add(numbers, BorderLayout.WEST);
        footer.add(balanceBadge, BorderLayout.EAST);
        card.setFooter(footer);
        addSection(card);
        finishSections();
    }

    private String kind(int row) {
        return row < rowKinds.size() ? rowKinds.get(row) : ACCOUNT;
    }

    /**
     * The table inside a scroll pane that only scrolls SIDEWAYS
     * (the whole page scrolls up and down), with our 2-row header on top.
     */
    private JComponent createWorksheetScroll() {
        final WorksheetHeader header = new WorksheetHeader(table);
        table.setTableHeader(null);   // we draw our own header instead

        JScrollPane scroll = new JScrollPane(table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
            @Override
            public Dimension getPreferredSize() {
                int height = header.getPreferredSize().height + table.getRowCount() * table.getRowHeight() + 2;
                if (getHorizontalScrollBar().isVisible()) {
                    height += getHorizontalScrollBar().getPreferredSize().height;
                }
                return new Dimension(100, height);   // small width: the page decides the width
            }

            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
        };
        scroll.setColumnHeaderView(header);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setOpaque(false);
        return scroll;
    }

    // =================================================================
    //  Data
    // =================================================================

    @Override
    public void refreshData() {
        try {
            List<Account> accounts = accountDAO.getAllAccounts();
            List<JournalEntry> entries = journalDAO.getAllEntries();
            worksheet = accounting.buildWorksheet(accounts, entries);

            model.clear();
            rowKinds.clear();
            for (WorksheetRow row : worksheet.getRows()) {
                Object[] values = new Object[2 + CELL_COUNT];
                values[0] = row.getCode();
                values[1] = row.getName();
                for (int c = 0; c < CELL_COUNT; c++) {
                    BigDecimal amount = row.get(c);
                    // the grand total row shows every value, even 0.00; other rows hide zeros
                    values[2 + c] = row.getKind().equals(GRAND_TOTAL) ? Money.format(amount) : Money.formatOrBlank(amount);
                }
                model.addRow(values);
                rowKinds.add(row.getKind());
            }
            emptyLabel.setVisible(worksheet.isEmpty());

            // tiles and footer
            tbTile.setText(Money.peso(worksheet.getUnadjustedTotal()));
            adjTile.setText(worksheet.getAdjustingEntryCount() == 0 ? "None yet"
                    : Money.peso(worksheet.getAdjustmentsTotal()) + "  (" + worksheet.getAdjustingEntryCount() + ")");
            boolean loss = worksheet.getNetIncome().signum() < 0;
            niTileTitle.setText(loss ? "Net loss" : "Net income");
            niTile.setText(Money.peso(worksheet.getNetIncome().abs()));
            diffTile.setText(Money.peso(worksheet.getBalanceSheetDifference()));
            updateTileTips(loss);

            footNet.setText("<html>" + (loss ? "Net loss" : "Net income") + " <b>"
                    + Money.peso(worksheet.getNetIncome().abs()) + "</b></html>");
            footDiff.setText("<html>Balance sheet difference <b>" + Money.peso(worksheet.getBalanceSheetDifference()) + "</b></html>");
            if (worksheet.isBalanced()) {
                balanceBadge.showOk("Balanced");
            } else {
                balanceBadge.showBad("Not balanced");
            }
            revalidate();
            repaint();
        } catch (SQLException e) {
            showError("Could not build the worksheet from MySQL data.", e);
        }
    }

    /** The explanations shown when the mouse rests on a tile */
    private void updateTileTips(boolean loss) {
        if (worksheet.isEmpty()) {
            String empty = "<html>Nothing posted yet. Post a journal entry first.</html>";
            for (RoundedPanel tile : new RoundedPanel[] {tbCard, adjCard, niCard, diffCard}) {
                tile.setToolTipText(empty);
            }
            return;
        }
        tbCard.setToolTipText("<html><b>Unadjusted trial balance</b><br>"
                + "Total of the Dr column: " + Money.peso(worksheet.getUnadjustedTotal()) + "<br>"
                + (worksheet.isTrialBalanceBalanced() ? "Debits = Credits, so it balances." : "Debits and credits do NOT match.")
                + "</html>");
        adjCard.setToolTipText("<html><b>Adjustments</b><br>"
                + worksheet.getAdjustingEntryCount() + " adjusting "
                + (worksheet.getAdjustingEntryCount() == 1 ? "entry" : "entries")
                + " totaling " + Money.peso(worksheet.getAdjustmentsTotal()) + ".<br>"
                + "Use Add adjustment for supplies used, expired insurance,<br>depreciation and accrued expenses.</html>");
        niCard.setToolTipText("<html><b>" + (loss ? "Net loss" : "Net income") + "</b><br>"
                + "Income statement Cr - Dr (revenue - expenses)<br>"
                + "= " + Money.peso(worksheet.getNetIncome()) + "<br>"
                + "Ending capital: " + Money.peso(worksheet.getEndingCapital()) + "</html>");
        diffCard.setToolTipText("<html><b>Balance sheet difference</b><br>"
                + "Balance sheet Dr - (Cr + ending capital).<br>"
                + (worksheet.isBalanced() ? "It is " + Money.peso(BigDecimal.ZERO) + ", so the books balance."
                                          : "It should be " + Money.peso(BigDecimal.ZERO) + ". Check your entries.")
                + "</html>");
    }

    /** Opens the Add adjustment pop-up; reloads the worksheet if one was added */
    public void openAdjustmentDialog() {
        try {
            AddAdjustmentDialog dialog = new AddAdjustmentDialog(mainFrame, accountDAO, journalDAO);
            dialog.setVisible(true);   // waits here until the dialog closes
            if (dialog.wasPosted()) {
                refreshData();
            }
        } catch (SQLException e) {
            showError("Could not open the adjustment form.", e);
        }
    }

    /** Saves the worksheet as a .csv file (opens in Excel) */
    private void exportCsv() {
        if (worksheet.isEmpty()) {
            showInfo("The worksheet is empty, so there is nothing to export yet.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("12-columnar-worksheet.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8.name())) {
            out.println(",,Unadjusted,,Adjustments,,Adjusted,,Income statement,,Owner's equity,,Balance sheet,");
            out.println("Code,Account,Dr,Cr,Dr,Cr,Dr,Cr,Dr,Cr,Dr,Cr,Dr,Cr");
            for (WorksheetRow row : worksheet.getRows()) {
                StringBuilder line = new StringBuilder();
                line.append(row.getCode()).append(',').append('"').append(row.getName().replace("\"", "\"\"")).append('"');
                for (int c = 0; c < CELL_COUNT; c++) {
                    BigDecimal amount = row.get(c);
                    line.append(',').append(amount == null ? "" : amount.setScale(2).toPlainString());
                }
                out.println(line);
            }
            showInfo("Saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            showError("Could not save the CSV file.", e);
        }
    }

    // =================================================================
    //  Small parts
    // =================================================================

    private static JLabel tileValue() {
        JLabel label = new JLabel("-");
        label.setFont(Theme.bold(20));
        label.setForeground(Theme.TITLE);
        return label;
    }

    /**
     * One colored tile. colors = { tile fill, tile border, icon box background }.
     * It is hoverable: it lifts and its border turns accentColor under the mouse.
     */
    private RoundedPanel statTile(String icon, Color accentColor, Color[] colors, JLabel title, JLabel value) {
        RoundedPanel tile = new RoundedPanel(new BorderLayout(14, 0));
        tile.setFill(colors[0]).setBorderColor(colors[1]).setHoverable(accentColor);
        tile.setBorder(BorderFactory.createEmptyBorder(16, 18, 18, 18));
        title.setFont(Theme.bold(14));
        title.setForeground(Theme.TEXT_SOFT);
        JPanel texts = new JPanel(new GridLayout(2, 1));
        texts.setOpaque(false);
        texts.add(title);
        texts.add(value);
        JPanel iconHolder = new JPanel(new BorderLayout());
        iconHolder.setOpaque(false);
        iconHolder.add(new IconBox(icon, 40, colors[2], accentColor), BorderLayout.CENTER);
        tile.add(iconHolder, BorderLayout.WEST);
        tile.add(texts, BorderLayout.CENTER);
        return tile;
    }

    /**
     * The two-row worksheet header:
     *   |      |         |   Unadjusted   |  Adjustments  | ...
     *   | Code | Account |   Dr   |  Cr   |  Dr   |  Cr   | ...
     * It reads the column widths from the table so it always lines up.
     */
    private static class WorksheetHeader extends JComponent {
        private final JTable table;

        WorksheetHeader(JTable table) {
            this.table = table;
            table.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
                public void columnMarginChanged(ChangeEvent e) { revalidate(); repaint(); }
                public void columnAdded(TableColumnModelEvent e) { }
                public void columnRemoved(TableColumnModelEvent e) { }
                public void columnMoved(TableColumnModelEvent e) { }
                public void columnSelectionChanged(ListSelectionEvent e) { }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(table.getColumnModel().getTotalColumnWidth(), 68);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            TableColumnModel columns = table.getColumnModel();
            int top = 34, h = getHeight();
            g2.setColor(Theme.PURPLE_TINT);
            g2.fillRect(0, 0, getWidth(), h);

            int x = 0;
            int[] lefts = new int[columns.getColumnCount() + 1];
            for (int i = 0; i < columns.getColumnCount(); i++) {
                lefts[i] = x;
                x += columns.getColumn(i).getWidth();
            }
            lefts[columns.getColumnCount()] = x;

            g2.setFont(Theme.bold(14));
            // Code and Account span both rows
            drawText(g2, "Code", lefts[0] + 8, 0, h, false, lefts[1] - lefts[0] - 16);
            drawText(g2, "Account", lefts[1] + 8, 0, h, false, lefts[2] - lefts[1] - 16);

            // group titles over each Dr/Cr pair
            for (int gIndex = 0; gIndex < GROUPS.length; gIndex++) {
                int left = lefts[2 + gIndex * 2];
                int right = lefts[4 + gIndex * 2];
                g2.setColor(GROUP_COLORS[gIndex]);
                g2.fillRect(left, 0, right - left, top);
                g2.setColor(new Color(0xe6def5));
                g2.drawLine(left, 0, left, h);
                g2.drawLine(left, top, right, top);
                g2.setColor(Theme.TABLE_HEAD);
                drawCentered(g2, GROUPS[gIndex], left, right, 0, top);
            }

            // Dr / Cr labels
            g2.setFont(Theme.bold(13.5f));
            for (int c = 0; c < CELL_COUNT; c++) {
                int left = lefts[2 + c];
                int right = lefts[3 + c];
                g2.setColor(Theme.TABLE_HEAD);
                drawText(g2, c % 2 == 0 ? "Dr" : "Cr", right - 8, top, h - top, true, right - left);
            }
            g2.setColor(new Color(0xe6def5));
            g2.drawLine(0, h - 1, getWidth(), h - 1);
            g2.dispose();
        }

        private void drawCentered(Graphics2D g2, String text, int left, int right, int y, int height) {
            FontMetrics fm = g2.getFontMetrics();
            int textX = left + (right - left - fm.stringWidth(text)) / 2;
            int textY = y + (height + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(text, Math.max(left + 4, textX), textY);
        }

        /** Left-aligned text at x, or right-aligned ending at x when alignRight is true */
        private void drawText(Graphics2D g2, String text, int x, int y, int height, boolean alignRight, int maxWidth) {
            g2.setColor(Theme.TABLE_HEAD);
            FontMetrics fm = g2.getFontMetrics();
            int textY = y + (height + fm.getAscent() - fm.getDescent()) / 2;
            int textX = alignRight ? x - fm.stringWidth(text) : x;
            g2.drawString(text, textX, textY);
        }
    }
}
