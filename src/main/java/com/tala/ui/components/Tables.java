// This class lives in the reusable UI components package.
package com.tala.ui.components;

// Theme holds the app's shared colors and fonts.
import com.tala.ui.Theme;

// Swing, border, table and AWT classes, plus mouse event classes for the hover effect.
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Creates tables with a styled header, striped rows, row hover, and bold total rows. */
public final class Tables {
    // Private constructor: only static methods are used.
    private Tables() {}

    // Builds a JTable for the given data model and applies the app's styling.
    public static JTable create(TableModel model) {
        // Index of the row under the mouse (-1 = none); it is a 1-element array so inner classes can change it.
        final int[] hoverRow = {-1};
        // Create the table showing the model's data.
        JTable table = new JTable(model);
        // Each row is 28 pixels tall.
        table.setRowHeight(28);
        // Hide vertical grid lines for a cleaner look.
        table.setShowVerticalLines(false);
        // Color of the horizontal grid lines.
        table.setGridColor(Theme.BORDER);
        // Colors used when a row is selected.
        table.setSelectionBackground(Theme.ROW_SELECTED);
        table.setSelectionForeground(Theme.TEXT);
        // Default font for cells.
        table.setFont(Theme.BASE);
        // Let the table fill the whole scroll area even with few rows.
        table.setFillsViewportHeight(true);
        // No horizontal space between cells, 1px vertical.
        table.setIntercellSpacing(new Dimension(0, 1));

        // Get the header (column titles) of the table.
        JTableHeader header = table.getTableHeader();
        // Do not let users drag columns to reorder them.
        header.setReorderingAllowed(false);
        // Custom drawing for header cells.
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                // Let the parent set up the label text, without selection/focus effects.
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                // Dark green background with white bold text.
                setBackground(Theme.GREEN_DARK);
                setForeground(Color.WHITE);
                setFont(Theme.BOLD);
                // Inner padding for the header text.
                setBorder(new EmptyBorder(8, 10, 8, 10));
                // Return this label as the cell's look.
                return this;
            }
        });
        // Header is 36 pixels tall (width 0 = automatic).
        header.setPreferredSize(new Dimension(0, 36));

        // Custom drawing for all normal body cells.
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                // Let the parent prepare the label; focus outline is turned off.
                super.getTableCellRendererComponent(t, v, sel, false, r, c);
                // Left/right padding inside the cell.
                setBorder(new EmptyBorder(0, 10, 0, 10));
                // Text to show (empty if the value is null).
                String text = v == null ? "" : v.toString();
                // Treat real numbers and peso-formatted text as numbers.
                boolean number = v instanceof Number || text.startsWith("₱");
                // Numbers align right (like accounting columns), other text aligns left.
                setHorizontalAlignment(number ? SwingConstants.RIGHT : SwingConstants.LEFT);
                // Only color unselected rows; selected rows keep the selection colors.
                if (!sel) {
                    // Background priority: total row, then hovered row, then alternating white/light stripes.
                    setBackground(isTotalRow(t, r) ? Theme.ROW_TOTAL
                            : r == hoverRow[0] ? Theme.ROW_HOVER : (r % 2 == 0 ? Color.WHITE : Theme.ROW_ALT));
                    // Normal text color.
                    setForeground(Theme.TEXT);
                }
                // Bold font for total rows, normal font for the rest.
                setFont(isTotalRow(t, r) ? Theme.BOLD : Theme.BASE);
                // Return this label as the cell's look.
                return this;
            }
        });

        // Mouse listener that tracks which row the mouse is over.
        MouseAdapter hover = new MouseAdapter() {
            // When the mouse moves, find the row under it and update the hover highlight.
            @Override public void mouseMoved(MouseEvent e) { setHover(table, hoverRow, table.rowAtPoint(e.getPoint())); }
            // When the mouse leaves the table, clear the hover highlight.
            @Override public void mouseExited(MouseEvent e) { setHover(table, hoverRow, -1); }
        };
        // Register the listener for mouse movement and for entering/leaving.
        table.addMouseMotionListener(hover);
        table.addMouseListener(hover);
        // Return the styled table.
        return table;
    }

    /** Wraps a table in a scroll pane with a light border. */
    public static JScrollPane scroll(JTable table) {
        // Scroll pane adds scroll bars and shows the table header.
        JScrollPane sp = new JScrollPane(table);
        // Thin border in the theme color.
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        // White area behind the table when there are few rows.
        sp.getViewport().setBackground(Color.WHITE);
        // Return the scroll pane.
        return sp;
    }

    // Updates the hovered row and repaints only when it actually changed.
    private static void setHover(JTable table, int[] hoverRow, int row) {
        // Only act if the row is different from before.
        if (hoverRow[0] != row) {
            // Remember the new hovered row.
            hoverRow[0] = row;
            // Redraw so the highlight moves.
            table.repaint();
        }
    }

    /** Summary rows (TOTAL, NET INCOME, ...) are shown in bold. */
    private static boolean isTotalRow(JTable t, int viewRow) {
        // Need at least 2 columns because we check column index 1 for the label.
        if (t.getColumnCount() < 2) return false;
        // Read the text in the second column of this row.
        Object v = t.getValueAt(viewRow, 1);
        // Empty cell: not a total row.
        if (v == null) return false;
        // Work with it as text.
        String s = v.toString();
        // It is a summary row if the label starts with one of these known words.
        return s.startsWith("TOTAL") || s.startsWith("GRAND TOTAL") || s.startsWith("NET INCOME") || s.startsWith("NET LOSS")
                || s.startsWith("ENDING") || s.startsWith("BALANCE") || s.startsWith("Total /");
    }
}
