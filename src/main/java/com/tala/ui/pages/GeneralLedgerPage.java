package com.tala.ui.pages;

// Service that builds ledgers from posted journal lines, and its two data types:
// AccountLedger (one account's whole ledger) and Posting (one line in a ledger).
import com.tala.service.LedgerService;
import com.tala.service.LedgerService.AccountLedger;
import com.tala.service.LedgerService.Posting;
// Base page class, main window, colour/font theme, rounded button, table helpers and money formatter.
import com.tala.ui.BasePage;
import com.tala.ui.MainFrame;
import com.tala.ui.Theme;
import com.tala.ui.components.RoundedButton;
import com.tala.ui.components.Tables;
import com.tala.util.Money;

// Swing GUI, borders, table model/renderer, AWT drawing classes.
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
// BigDecimal for exact money, SQLException for database errors.
import java.math.BigDecimal;
import java.sql.SQLException;
// Date and month-name helpers, lists and Locale.
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * General Ledger: a separate ledger for every account that has been used, like the T-account pages
 * of a real ledger book. Each one lists that account's transactions with a running balance and ends
 * with a "Total / ending balance" row. Accounts are shown two to a row.
 */
public class GeneralLedgerPage extends BasePage {
    // Drop-down text meaning "show every account".
    private static final String ALL = "All accounts";
    // Column headings of every ledger table.
    private static final String[] COLUMNS = {"Date", "Description", "Ref", "Debit", "Credit", "Balance"};
    /** The description column never shrinks below this; the other columns are sized to their content. */
    private static final int DESC_MIN = 90;
    private static final int CELL_PAD = 11;      // left + right padding of a cell, plus a little slack
    // Space in pixels between the two ledger cards in a row.
    private static final int CARD_GAP = 10;
    // Small regular and bold fonts so six columns fit on half the screen.
    private static final Font SMALL = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font SMALL_BOLD = new Font("SansSerif", Font.BOLD, 12);

    // Constructor: passes the main window to BasePage.
    public GeneralLedgerPage(MainFrame frame) { super(frame); }

    // Page heading.
    @Override protected String title() { return "General Ledger"; }

    // Short help text under the heading.
    @Override protected String hint() {
        return "One ledger per account you have used: every transaction on that account, its running balance, total and ending balance.";
    }

    // Builds the page: loads the ledgers and shows them as cards with an account filter.
    @Override protected JComponent build() {
        // Outer panel for the page.
        JPanel p = pagePanel();

        // Start with an empty list so the page still works if loading fails.
        List<AccountLedger> ledgers = new ArrayList<>();
        try {
            // Load every account's ledger (posting lines + running balances) from the database.
            ledgers = LedgerService.load();
            frame.setStatus(" Connected to MySQL");
        } catch (SQLException ex) {
            // Database problem: update the status bar and warn the user.
            frame.setStatus(" Database error");
            JOptionPane.showMessageDialog(frame,
                    "Unable to load data. Check that MySQL is running and database/tala.sql has been executed.\n\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        // Refresh button reloads the page so new postings appear.
        JButton refresh = new RoundedButton("Refresh", RoundedButton.Kind.SECONDARY);
        refresh.addActionListener(e -> show());

        // No ledgers means nothing has been posted yet, so show a friendly message instead.
        if (ledgers.isEmpty()) {
            // Centred, white label with a border acting as the empty-state message.
            JLabel empty = new JLabel("No posted entries yet. Each account you use will get its own ledger here.", SwingConstants.CENTER);
            empty.setFont(Theme.BASE);
            empty.setForeground(Theme.MUTED);
            empty.setOpaque(true);
            empty.setBackground(Color.WHITE);
            empty.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
            // Message in the middle, refresh button at the bottom, then stop building.
            p.add(empty, BorderLayout.CENTER);
            p.add(actionBar(refresh), BorderLayout.SOUTH);
            return p;
        }

        // Panel that will hold all the ledger cards, placed inside a scroll pane.
        CardsPanel cards = new CardsPanel();
        JScrollPane scroll = new JScrollPane(cards);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.BACKGROUND);
        // Mouse-wheel scrolls 24 pixels at a time.
        scroll.getVerticalScrollBar().setUnitIncrement(24);

        // Drop-down filter: first "All accounts", then one item per account ("code · name").
        JComboBox<String> filter = new JComboBox<>();
        filter.addItem(ALL);
        for (AccountLedger l : ledgers) filter.addItem(label(l));
        // A final copy of the list so the lambda below is allowed to use it.
        final List<AccountLedger> all = ledgers;
        // Code that redraws the cards according to the chosen filter.
        Runnable render = () -> {
            // Which item is selected in the drop-down.
            String chosen = (String) filter.getSelectedItem();
            // Collect the ledgers that match (all of them, or only the chosen account).
            List<AccountLedger> shown = new ArrayList<>();
            for (AccountLedger l : all) if (ALL.equals(chosen) || label(l).equals(chosen)) shown.add(l);
            // Rebuild the cards with just those ledgers.
            fill(cards, shown);
            // After redrawing, scroll back to the top.
            SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
        };
        // Redraw whenever the user picks something else, and once now for the first display.
        filter.addActionListener(e -> render.run());
        render.run();

        // Label "Account:" next to the drop-down.
        JLabel filterLabel = new JLabel("Account:");
        filterLabel.setFont(Theme.BASE);
        filterLabel.setForeground(Theme.TEXT);
        // Top bar aligned to the right holding the label, the filter and the refresh button.
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bar.setOpaque(false);
        bar.add(filterLabel);
        bar.add(filter);
        bar.add(refresh);

        // Bar on top, scrolling ledger cards in the middle; return the finished page.
        p.add(bar, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // Text shown for an account in the filter: its code and name, e.g. "101 · Cash".
    private static String label(AccountLedger l) {
        return l.code + " · " + l.name;
    }

    /** Lays the ledgers out two to a row (a last odd one sits alone on the left). */
    static void fill(CardsPanel cards, List<AccountLedger> ledgers) {
        // Remove the old cards before adding new ones.
        cards.removeAll();
        // One set of column widths for every card, so the ledgers line up like pages of the same book.
        int[] widths = fixedWidths(ledgers);
        // Width of one card = minimum description width + 2 for the border...
        int card = DESC_MIN + 2;                                  // + the card border
        // ...plus the width of every other column (column 1 is the description, already counted).
        for (int c = 0; c < widths.length; c++) if (c != 1) card += widths[c];
        // two cards + the gap between them + the panel's side padding; narrower windows scroll sideways
        cards.minWidth = card * 2 + CARD_GAP + 16;
        // Step through the ledgers two at a time, one pair per row.
        for (int i = 0; i < ledgers.size(); i += 2) {
            JPanel row = new JPanel(new GridLayout(1, 2, CARD_GAP, 0));   // both cards in a row share one height
            row.setOpaque(false);
            // Left card is always present.
            row.add(top(ledgerCard(ledgers.get(i), widths)));
            // Right card exists only if there is a next ledger; otherwise an empty filler cell.
            row.add(i + 1 < ledgers.size() ? top(ledgerCard(ledgers.get(i + 1), widths)) : emptyCell());
            // Add the row, then a 12-pixel vertical gap below it.
            cards.add(row);
            cards.add(Box.createVerticalStrut(12));
        }
        // Tell Swing to recalculate the layout and redraw.
        cards.revalidate();
        cards.repaint();
    }

    // Wraps a card so it stays at the top of its grid cell instead of stretching to fill it.
    private static JComponent top(JComponent card) {
        JPanel cell = new JPanel(new BorderLayout());
        cell.setOpaque(false);
        cell.add(card, BorderLayout.NORTH);                          // keep the card at the top of its cell
        return cell;
    }

    // An invisible placeholder used when the last row has only one ledger.
    private static JComponent emptyCell() {
        JPanel cell = new JPanel();
        cell.setOpaque(false);
        return cell;
    }

    /** One account's ledger: title block, transactions with running balance, and the total / ending balance row. */
    static JComponent ledgerCard(AccountLedger l, int[] fixed) {
        // Table model with the ledger columns; cells are read-only because a ledger is only viewed here.
        DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        // One table row per posting (each posting is a debit or credit made to this account).
        for (int i = 0; i < l.postings.size(); i++) model.addRow(rowFor(l, i));
        // Last row: totals and ending balance.
        model.addRow(totalRow(l));

        // Create the styled table; no row selection, 26-pixel rows, columns resize together.
        JTable table = Tables.create(model);
        table.setRowSelectionAllowed(false);
        table.setRowHeight(26);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        // Set each column's width.
        for (int i = 0; i < COLUMNS.length; i++) {
            var col = table.getColumnModel().getColumn(i);
            if (i == 1) {                                   // description takes whatever space is left
                col.setMinWidth(DESC_MIN);
                col.setPreferredWidth(Math.max(DESC_MIN, fixed[1]));
            } else {                                        // numbers, dates and references always fit
                // Fixed columns: min = max = preferred, so they never change size.
                col.setMinWidth(fixed[i]);
                col.setMaxWidth(fixed[i]);
                col.setPreferredWidth(fixed[i]);
            }
        }
        // Compact header: smaller font, amounts right-aligned over their columns.
        // Keep the default header renderer and customise what it returns.
        TableCellRenderer headerBase = table.getTableHeader().getDefaultRenderer();
        table.getTableHeader().setDefaultRenderer((t, v, sel, foc, r, col) -> {
            JLabel h = (JLabel) headerBase.getTableCellRendererComponent(t, v, sel, foc, r, col);
            // Small bold font and padding for the header text.
            h.setFont(SMALL_BOLD);
            h.setBorder(new EmptyBorder(8, 5, 8, 5));
            // Debit, Credit and Balance (columns 3 and up) are numbers, so align them right.
            h.setHorizontalAlignment(col >= 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
            return h;
        });
        // Header height of 30 pixels (width 0 lets the layout decide).
        table.getTableHeader().setPreferredSize(new Dimension(0, 30));
        // Compact cells so six columns fit in half the page width.
        TableCellRenderer base = table.getDefaultRenderer(Object.class);
        table.setDefaultRenderer(Object.class, (t, v, sel, foc, r, col) -> {
            JLabel cell = (JLabel) base.getTableCellRendererComponent(t, v, sel, foc, r, col);
            // The last row is the total row; it is drawn in bold.
            boolean total = r == t.getRowCount() - 1;
            cell.setFont(total ? SMALL_BOLD : SMALL);
            cell.setBorder(new EmptyBorder(0, 5, 0, 5));
            // Cell text, treating null as an empty string.
            String text = v == null ? "" : v.toString();
            if (total && col == 1) {                        // shorten the label if the column is narrow
                // Space available for the text in the description column.
                int room = t.getColumnModel().getColumn(1).getWidth() - CELL_PAD;
                FontMetrics fm = cell.getFontMetrics(SMALL_BOLD);
                // Use the full label if it fits, else "Total / ending", else just "Total".
                text = fm.stringWidth(text) <= room ? text : fm.stringWidth("Total / ending") <= room ? "Total / ending" : "Total";
                cell.setText(text);
            }
            // Hovering over a description shows the full text as a tooltip (useful when it is cut off).
            if (col == 1) cell.setToolTipText(text.isEmpty() ? null : v.toString());
            return cell;
        });

        // Account name in big green bold text.
        JLabel name = new JLabel(l.name);
        name.setFont(Theme.BOLD.deriveFont(16f));
        name.setForeground(Theme.GREEN_DARK);
        // Sub-title: account code, type (asset, liability...) and its normal balance side
        // (assets/expenses normally have debit balances; liabilities/equity/revenue have credit balances).
        JLabel sub = new JLabel("Account " + l.code + " · " + capitalize(l.type) + " · Normal balance: "
                + (l.debitNormal ? "Debit" : "Credit"));
        sub.setFont(Theme.HINT);
        sub.setForeground(Theme.MUTED);
        // Header block stacks the name above the sub-title, with padding.
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setBorder(new EmptyBorder(10, 12, 10, 12));
        // Left-align both labels inside the vertical box.
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.add(name);
        head.add(sub);

        // Grid panel: column header on top and the table body below it (no scroll pane needed).
        JPanel grid = new JPanel(new BorderLayout());
        grid.add(table.getTableHeader(), BorderLayout.NORTH);
        grid.add(table, BorderLayout.CENTER);

        // The card: white background with a border, title block above and the table below.
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        card.add(head, BorderLayout.NORTH);
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    /**
     * Width every column needs to show its widest cell (and header) without clipping, across all the
     * ledgers shown. Index 1 (description) is only the width it would like, capped at 260.
     */
    private static int[] fixedWidths(List<AccountLedger> ledgers) {
        // A throw-away label used only to measure how wide text will be in each font.
        JLabel probe = new JLabel();
        FontMetrics fm = probe.getFontMetrics(SMALL), fb = probe.getFontMetrics(SMALL_BOLD);
        // One width per column.
        int[] w = new int[COLUMNS.length];
        // Start each width at the header text width plus padding.
        for (int c = 0; c < COLUMNS.length; c++) w[c] = fb.stringWidth(COLUMNS[c]) + CELL_PAD;
        // Look at every ledger and every row to find the widest cell in each column.
        for (AccountLedger l : ledgers) {
            for (int i = 0; i < l.postings.size(); i++) {
                Object[] row = rowFor(l, i);
                for (int c = 0; c < COLUMNS.length; c++) w[c] = Math.max(w[c], fm.stringWidth(String.valueOf(row[c])) + CELL_PAD);
            }
            // Also measure the bold total row.
            Object[] total = totalRow(l);                         // bold; its label shortens itself, so skip column 1
            for (int c = 0; c < COLUMNS.length; c++) {
                if (c != 1) w[c] = Math.max(w[c], fb.stringWidth(String.valueOf(total[c])) + CELL_PAD);
            }
        }
        // Cap the description width so one very long text does not make the cards huge.
        w[1] = Math.min(w[1], 260);
        return w;
    }

    // Builds the table row for posting number i of a ledger.
    private static Object[] rowFor(AccountLedger l, int i) {
        // The i-th posting (one journal line affecting this account).
        Posting po = l.postings.get(i);
        // Columns: date, description, reference (blank if null), debit (blank if zero),
        // credit (blank if zero), and the running balance after this posting.
        return new Object[]{
                shortDate(po.date()),
                po.description(),
                po.reference() == null ? "" : po.reference(),
                po.debit().signum() > 0 ? Money.currency(po.debit()) : "",
                po.credit().signum() > 0 ? Money.currency(po.credit()) : "",
                balance(l.running.get(i))};
    }

    // The closing row of a ledger: total debits, total credits and the ending balance.
    private static Object[] totalRow(AccountLedger l) {
        return new Object[]{"", "Total / ending balance", "",
                Money.currency(l.totalDebit), Money.currency(l.totalCredit), balance(l.endingNet())};
    }

    /** "Oct. 28" */
    private static String shortDate(LocalDate d) {
        // Abbreviated English month name, a dot, then the day number.
        return d.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + ". " + d.getDayOfMonth();
    }

    /** A running or ending balance on its real side: "₱44600.00 DR" or "₱500.00 CR". */
    static String balance(BigDecimal net) {
        // Zero balance has no side, so just show the zero amount.
        if (net.signum() == 0) return Money.currency(BigDecimal.ZERO);
        // Positive net (debits exceed credits) is a Debit balance (DR); negative is a Credit balance (CR).
        return Money.currency(net.abs()) + (net.signum() > 0 ? " DR" : " CR");
    }

    // Makes the first letter uppercase and the rest lowercase (e.g. "ASSET" becomes "Asset").
    private static String capitalize(String s) {
        return s.isEmpty() ? s : s.charAt(0) + s.substring(1).toLowerCase(Locale.ENGLISH);
    }

    /** Vertical stack of rows that fills the viewport width, but scrolls sideways when narrower than {@code minWidth}. */
    static final class CardsPanel extends JPanel implements Scrollable {
        // Smallest width the panel wants; below this a horizontal scroll bar appears.
        int minWidth = 0;

        // Constructor: stack children vertically, set background and outer padding.
        CardsPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Theme.BACKGROUND);
            setBorder(new EmptyBorder(10, 8, 0, 8));
        }
        // Scrollable methods tell the scroll pane how to behave:
        // the visible size it should start with is the panel's own preferred size.
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        // Arrow/wheel step is 24 pixels.
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        // Page step is the visible height minus 40 pixels (so some context stays visible), at least 24.
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(r.height - 40, 24); }
        // Match the viewport width when it is wide enough; otherwise keep minWidth and scroll sideways.
        @Override public boolean getScrollableTracksViewportWidth() {
            Container parent = getParent();
            return parent == null || parent.getWidth() >= minWidth;
        }
        // Height is never forced to match the viewport, so the panel scrolls vertically.
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
        // Preferred size is the normal one, but never narrower than minWidth.
        @Override public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.max(d.width, minWidth), d.height);
        }
    }
}
