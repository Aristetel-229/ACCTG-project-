// Package for the UI framework classes.
package com.tala.ui;

// Swing components (JPanel, JLabel, JOptionPane, ...).
import javax.swing.*;
// DefaultTableModel = the data container behind a JTable.
import javax.swing.table.DefaultTableModel;
// AWT classes: layouts (BorderLayout, FlowLayout), Component, GridBagConstraints.
import java.awt.*;
// JDBC classes used by readTable().
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// Our connection helper.
import com.tala.db.DB;

/** Base class for every screen: shared layout and database-to-table helpers. */
// "abstract" = you cannot create a BasePage directly; you must extend it (DashboardPage, JournalEntryPage, ...).
public abstract class BasePage {
    // The main window. Pages use it to change the status bar text, show dialogs and display themselves.
    // "protected" = visible to this class and its subclasses (the pages).
    protected final MainFrame frame;

    // Constructor: every page receives the main window and stores it.
    protected BasePage(MainFrame frame) {
        this.frame = frame;
    }

    /** Title shown above the page. */
    // "abstract" = no body here; every page MUST provide its own title.
    protected abstract String title();

    /** One-line explanation shown under the title (helps students know what the page is for). */
    // Default is an empty hint; pages may override it to show a subtitle.
    protected String hint() { return ""; }

    /** Builds the page contents. */
    // Every page must build and return its own screen content (tables, forms, buttons, ...).
    protected abstract JComponent build();

    /** Builds the page and displays it in the main window. */
    public void show() {
        // getClass().getSimpleName() = the class name, e.g. "DashboardPage".
        // The main window uses that name to highlight the matching sidebar button.
        // build() is called every time, so show() also works as "refresh".
        frame.setPage(getClass().getSimpleName(), title(), hint(), build());
    }

    // Creates the empty panel that each page uses as its container.
    protected JPanel pagePanel() {
        // BorderLayout(10, 10) = 5 regions (north/south/east/west/center) with 10px gaps.
        JPanel p = new JPanel(new BorderLayout(10, 10));
        // Transparent, so the window's background color shows through.
        p.setOpaque(false);
        return p;
    }

    /** Right-aligned row holding one or more buttons. */
    // "JComponent..." (varargs) lets callers pass any number of buttons: actionBar(a), actionBar(a, b), ...
    protected JPanel actionBar(JComponent... buttons) {
        // FlowLayout.RIGHT = items line up from the right edge; 8px gap between them.
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bar.setOpaque(false);
        // Add every button that was passed in.
        for (JComponent b : buttons) bar.add(b);
        return bar;
    }

    // Adds one form row: a label on the left and an input control on the right,
    // inside a GridBagLayout panel (a flexible grid).
    protected void addField(JPanel panel, GridBagConstraints g, int row, String label, Component field) {
        // Column 0, the given row; weightx 0.35 = the label column gets a smaller share of extra width.
        g.gridx = 0; g.gridy = row; g.weightx = 0.35;
        // Create and style the label.
        JLabel l = new JLabel(label);
        l.setFont(Theme.BASE);
        l.setForeground(Theme.TEXT);
        // Place the label using the constraints above.
        panel.add(l, g);
        // Column 1; weightx 1 = the input field takes all the remaining width.
        g.gridx = 1; g.weightx = 1;
        panel.add(field, g);
    }

    // Shows a red error popup. Used for validation messages (empty fields, invalid amount, ...).
    protected void error(String message) {
        JOptionPane.showMessageDialog(frame, message, "Validation / Database Error", JOptionPane.ERROR_MESSAGE);
    }

    // Creates an empty table model with the given column names and zero rows.
    protected DefaultTableModel newTableModel(String[] columns) {
        // The "{ ... }" after the constructor is an anonymous subclass that overrides one method.
        return new DefaultTableModel(columns, 0) {
            // Returning false makes every cell read-only (the user cannot type into the table).
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    // Runs any SELECT query and converts the result into a table model ready to display.
    // "sql" = the query; "columns" = the column titles shown in the table header.
    protected DefaultTableModel readTable(String sql, String[] columns) {
        // Start with an empty model.
        DefaultTableModel model = newTableModel(columns);
        // Open connection, prepare statement, execute query. All three close automatically.
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            // How many columns did the query return?
            int count = rs.getMetaData().getColumnCount();
            // Loop over each row of the result.
            while (rs.next()) {
                // One array slot per column.
                Object[] row = new Object[count];
                // JDBC columns are numbered from 1, Java arrays from 0, hence i + 1.
                for (int i = 0; i < count; i++) {
                    // getObject returns the value as whatever type fits (Date, BigDecimal, String, ...).
                    Object value = rs.getObject(i + 1);
                    // Show an empty text instead of "null" in the table.
                    row[i] = value == null ? "" : value;
                }
                // Add the finished row to the table model.
                model.addRow(row);
            }
            // Success: update the status bar at the bottom of the window.
            frame.setStatus(" Connected to MySQL");
        } catch (SQLException ex) {
            // Failure: show the problem in the status bar...
            frame.setStatus(" Database error");
            // ...and in a popup that explains the most common causes.
            JOptionPane.showMessageDialog(frame,
                    "Unable to load data. Check that MySQL is running and database/tala.sql has been executed.\n\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
        // Return the model (it is empty if there was an error).
        return model;
    }
}
