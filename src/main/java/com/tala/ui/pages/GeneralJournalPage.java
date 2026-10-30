package com.tala.ui.pages;

// Database helper that hands out JDBC connections.
import com.tala.db.DB;
// Base page class, main window, colour/font theme, rounded button, table helpers and money formatter.
import com.tala.ui.BasePage;
import com.tala.ui.MainFrame;
import com.tala.ui.Theme;
import com.tala.ui.components.RoundedButton;
import com.tala.ui.components.Tables;
import com.tala.util.Money;

// Swing GUI classes, table model/renderer types and AWT drawing classes.
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
// BigDecimal gives exact money arithmetic.
import java.math.BigDecimal;
// JDBC classes used to query the database.
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
// Date and month-name helpers, plus collections and Locale.
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * General Journal: every posted entry in date order as
 * DATE | PARTICULARS | POST REF | DEBIT | CREDIT. The debited account comes first, the credited
 * account is indented under it, and an explanation line closes each entry.
 * Only accounts that were actually used appear, and a TOTAL row ends the page.
 */
public class GeneralJournalPage extends BasePage {
    // Two spaces put in front of credited account names; by accounting convention credits are indented.
    private static final String INDENT = "  ";

    // Constructor: passes the main window up to BasePage.
    public GeneralJournalPage(MainFrame frame) { super(frame); }

    // Page heading.
    @Override protected String title() { return "General Journal"; }

    // Short help text under the heading.
    @Override protected String hint() {
        return "Every entry in date order: date, particulars, post reference, debit and credit. Only accounts you have used appear.";
    }

    // Builds the page: reads posted entries from the database and lays them out as a journal table.
    @Override protected JComponent build() {
        // Outer panel for the page.
        JPanel p = pagePanel();
        // Table model with the five standard journal columns.
        DefaultTableModel model = newTableModel(new String[]{"Date", "Particulars", "Post Ref", "Debit", "Credit"});
        Set<Integer> explanationRows = new HashSet<>();   // rows drawn as the shaded "explanation" line
        // Running totals of all debits and credits; in a correct journal these are equal.
        BigDecimal totalDebit = BigDecimal.ZERO, totalCredit = BigDecimal.ZERO;
        // Counts how many journal entries were listed.
        int entries = 0;

        // SQL returning one row per journal line (an account debited or credited) for POSTED entries only.
        // The ORDER BY sorts by date, then entry, then debit lines first, so debits appear above credits.
        String sql = """
            SELECT je.id, je.entry_date, je.reference_no, je.description,
                   a.account_code, a.account_name, jel.debit, jel.credit
            FROM journal_entry_lines jel
            JOIN journal_entries je ON je.id = jel.journal_entry_id
            JOIN accounts a ON a.id = jel.account_id
            WHERE je.status = 'POSTED'
            ORDER BY je.entry_date, je.id, (jel.debit > 0) DESC, jel.id
            """;
        // try-with-resources opens the connection, statement and result set and closes them automatically.
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            // Id of the entry being written (-1 = none yet), the year last shown, and the explanation text to print.
            long currentEntry = -1;
            int currentYear = -1;
            String lastDescription = "";
            // Go through the result one journal line at a time.
            while (rs.next()) {
                // Which journal entry this line belongs to.
                long entryId = rs.getLong("id");
                // A different id means a new entry is starting.
                if (entryId != currentEntry) {
                    if (currentEntry != -1) {                       // close the previous entry
                        // Remember this row number so it gets shaded, then add the explanation line.
                        explanationRows.add(model.getRowCount());
                        model.addRow(new Object[]{"", lastDescription, "", "", ""});
                        model.addRow(new Object[]{"", "", "", "", ""});   // blank spacer row
                    }
                    // Date of the new entry.
                    LocalDate d = rs.getDate("entry_date").toLocalDate();
                    if (d.getYear() != currentYear) {               // year row, as in the journal sample
                        // Print the year once whenever it changes.
                        currentYear = d.getYear();
                        model.addRow(new Object[]{String.valueOf(currentYear), "", "", "", ""});
                    }
                    // Switch to the new entry and count it.
                    currentEntry = entryId;
                    entries++;
                    // Explanation line text = description plus reference number.
                    lastDescription = rs.getString("description") + "  ·  " + rs.getString("reference_no");
                    // Date shown as e.g. "Oct. 28"; only the first line of an entry shows the date.
                    String shortDate = d.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + ". " + d.getDayOfMonth();
                    model.addRow(lineRow(shortDate, rs));
                } else {
                    // Same entry: add another line (usually the credit) with a blank date.
                    model.addRow(lineRow("", rs));
                }
                // Add this line's debit and credit to the running totals.
                totalDebit = totalDebit.add(rs.getBigDecimal("debit"));
                totalCredit = totalCredit.add(rs.getBigDecimal("credit"));
            }
            if (currentEntry != -1) {                               // close the last entry
                // The last entry also needs its explanation line (no spacer needed after it).
                explanationRows.add(model.getRowCount());
                model.addRow(new Object[]{"", lastDescription, "", "", ""});
            }
            // Query worked, so show the connected status.
            frame.setStatus(" Connected to MySQL");
        } catch (SQLException ex) {
            // On a database error, update the status bar and warn the user.
            frame.setStatus(" Database error");
            JOptionPane.showMessageDialog(frame,
                    "Unable to load data. Check that MySQL is running and database/tala.sql has been executed.\n\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        // True if at least one row was loaded.
        boolean hasData = model.getRowCount() > 0;
        if (hasData) {
            // Final TOTAL row showing total debits and total credits (they should match).
            model.addRow(new Object[]{"", "TOTAL", "", Money.currency(totalDebit), Money.currency(totalCredit)});
        }

        // Build the visible table; rows cannot be selected because this page is read-only.
        JTable table = Tables.create(model);
        table.setRowSelectionAllowed(false);
        // Keep the standard look, but shade and italicize the explanation lines like the journal sample.
        // Remember the default cell renderer so we can reuse its normal look.
        TableCellRenderer base = table.getDefaultRenderer(Object.class);
        table.setDefaultRenderer(Object.class, (t, v, sel, foc, r, col) -> {
            // Let the default renderer prepare the cell first.
            Component cell = base.getTableCellRendererComponent(t, v, sel, foc, r, col);
            // Explanation rows get a light green background and italic text.
            if (explanationRows.contains(r)) {
                cell.setBackground(Theme.GREEN_LIGHT);
                cell.setFont(Theme.BASE.deriveFont(Font.ITALIC));
            }
            return cell;
        });
        // Column widths: narrow date, wide particulars, narrow post ref, medium debit and credit.
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(420);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(130);
        table.getColumnModel().getColumn(4).setPreferredWidth(130);
        // Put the table (inside a scroll pane) in the centre of the page.
        p.add(Tables.scroll(table), BorderLayout.CENTER);

        // Summary text: entry count with correct singular/plural, plus totals; or a message if empty.
        JLabel status = new JLabel(!hasData ? "No posted entries yet."
                : entries + (entries == 1 ? " entry" : " entries") + "   |   Total debit " + Money.currency(totalDebit)
                + "   |   Total credit " + Money.currency(totalCredit));
        status.setFont(Theme.BOLD);
        status.setForeground(Theme.GREEN_DARK);
        // Refresh button reloads the page (show() rebuilds it with fresh data).
        JButton refresh = new RoundedButton("Refresh", RoundedButton.Kind.SECONDARY);
        refresh.addActionListener(e -> show());
        // Bottom bar: summary on the left, refresh button on the right.
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(status, BorderLayout.WEST);
        south.add(actionBar(refresh), BorderLayout.EAST);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    /** One journal line: debit accounts sit at the left, credit accounts are indented. */
    private Object[] lineRow(String date, ResultSet rs) throws SQLException {
        // Read the debit and credit amounts of this line (one of them is zero).
        BigDecimal debit = rs.getBigDecimal("debit"), credit = rs.getBigDecimal("credit");
        // Name of the account used on this line.
        String name = rs.getString("account_name");
        // Return the table row: date, particulars (indented if credit), account code as post ref,
        // then the debit and credit amounts, leaving the empty side blank.
        return new Object[]{date,
                credit.signum() > 0 ? INDENT + name : name,
                rs.getString("account_code"),
                debit.signum() > 0 ? Money.currency(debit) : "",
                credit.signum() > 0 ? Money.currency(credit) : ""};
    }
}
