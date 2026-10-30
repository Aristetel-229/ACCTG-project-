package com.tala.ui;

// Swing components (panels, labels, dialogs, tables).
import javax.swing.*;
// Table model that stores the rows and columns shown in a JTable.
import javax.swing.table.DefaultTableModel;
// AWT layout managers and constraints (BorderLayout, FlowLayout, GridBagConstraints...).
import java.awt.*;
// JDBC classes used to talk to the MySQL database.
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// Helper that opens database connections.
import com.tala.db.DB;

/**
 * Base class for every screen (page). It gives each page the same structure: a title, a hint,
 * a build() method that creates the content, and helpers for layout, error messages and for
 * turning an SQL query into a table.
 */
public abstract class BasePage {
    // The main window; pages use it to change the content and the status bar.
    protected final MainFrame frame;

    // Constructor: each page must be told which main window it belongs to.
    protected BasePage(MainFrame frame) {
        // Save the window so subclasses can use it.
        this.frame = frame;
    }

    /** Title shown above the page. */
    // Every page must provide its own title (abstract = no body here).
    protected abstract String title();

    /** One-line explanation shown under the title (helps students know what the page is for). */
    // Default hint is empty; pages override it when they want to show a hint.
    protected String hint() { return ""; }

    /** Builds the page contents. */
    // Every page must build its own screen content.
    protected abstract JComponent build();

    /** Builds the page and displays it in the main window. */
    public void show() {
        // The class name (e.g. "DashboardPage") is the key that highlights the matching sidebar button.
        frame.setPage(getClass().getSimpleName(), title(), hint(), build());
    }

    // Creates an empty, see-through panel with a gap of 10 pixels between regions (north/center/south).
    protected JPanel pagePanel() {
        // BorderLayout(10, 10) = horizontal and vertical gaps of 10.
        JPanel p = new JPanel(new BorderLayout(10, 10));
        // Not opaque, so the page background color shows through.
        p.setOpaque(false);
        // Return the ready panel.
        return p;
    }

    /** Right-aligned row holding one or more buttons. */
    protected JPanel actionBar(JComponent... buttons) {
        // FlowLayout.RIGHT puts the buttons on the right with 8 pixels between them.
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        // Transparent background.
        bar.setOpaque(false);
        // Add every button that was passed in.
        for (JComponent b : buttons) bar.add(b);
        // Return the finished bar.
        return bar;
    }

    // Adds one "label + input field" row to a form that uses GridBagLayout.
    protected void addField(JPanel panel, GridBagConstraints g, int row, String label, Component field) {
        // Put the label in column 0 of the given row; weightx 0.35 gives it a smaller share of the width.
        g.gridx = 0; g.gridy = row; g.weightx = 0.35;
        // Create the label with the given text.
        JLabel l = new JLabel(label);
        // Use the shared theme font and text color.
        l.setFont(Theme.BASE);
        l.setForeground(Theme.TEXT);
        // Add the label to the form using the constraints.
        panel.add(l, g);
        // Put the input field in column 1 and let it take the remaining width (weightx 1).
        g.gridx = 1; g.weightx = 1;
        // Add the field next to the label.
        panel.add(field, g);
    }

    // Shows a red error pop-up (used for validation problems such as unbalanced entries).
    protected void error(String message) {
        JOptionPane.showMessageDialog(frame, message, "Validation / Database Error", JOptionPane.ERROR_MESSAGE);
    }

    // Creates an empty table model with the given column names and zero rows.
    protected DefaultTableModel newTableModel(String[] columns) {
        // Anonymous subclass of DefaultTableModel, because we change one behavior below.
        return new DefaultTableModel(columns, 0) {
            // No cell can be edited; tables are read-only so users cannot change recorded data by accident.
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    // Runs an SQL SELECT and returns its result as a table model, ready to display.
    protected DefaultTableModel readTable(String sql, String[] columns) {
        // Start with an empty model that has the right column headings.
        DefaultTableModel model = newTableModel(columns);
        // try-with-resources: the connection, statement and result set are closed automatically.
        try (Connection c = DB.getConnection();
             // Prepare the SQL statement (safe and efficient way to run SQL in JDBC).
             PreparedStatement ps = c.prepareStatement(sql);
             // Execute the query and get the result rows.
             ResultSet rs = ps.executeQuery()) {
            // Find out how many columns the query returned.
            int count = rs.getMetaData().getColumnCount();
            // Loop through each row of the result.
            while (rs.next()) {
                // Array that will hold the values of one table row.
                Object[] row = new Object[count];
                // Copy each column value (JDBC columns start at 1, Java arrays at 0).
                for (int i = 0; i < count; i++) {
                    // Read the value of column i+1 as a generic object.
                    Object value = rs.getObject(i + 1);
                    // Show an empty string instead of null so the table does not display "null".
                    row[i] = value == null ? "" : value;
                }
                // Add the finished row to the table model.
                model.addRow(row);
            }
            // Tell the user in the status bar that the database is working.
            frame.setStatus(" Connected to MySQL");
        // If anything goes wrong with the database...
        } catch (SQLException ex) {
            // ...show an error in the status bar...
            frame.setStatus(" Database error");
            // ...and a pop-up with hints on how to fix it (start MySQL, run tala.sql).
            JOptionPane.showMessageDialog(frame,
                    "Unable to load data. Check that MySQL is running and database/tala.sql has been executed.\n\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
        // Return the model (empty if the query failed).
        return model;
    }
}
