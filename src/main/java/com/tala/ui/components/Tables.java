// Package for reusable UI building blocks.
package com.tala.ui.components;

import com.tala.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
// Table-related classes: TableModel, JTableHeader, DefaultTableCellRenderer, ...
import javax.swing.table.*;
import java.awt.*;
// Mouse event classes used for the row hover effect.
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Creates tables with a styled header, striped rows, row hover, and bold total rows. */
public final class Tables {
    // Static-only class: never instantiated.
    private Tables() {}

    // Builds a styled JTable from a data model. Every page uses this for its tables.
    public static JTable create(TableModel model) {
        // Stores the row number under the mouse (-1 = none). It is a one-item array because
        // a variable used inside the anonymous classes below must be final, but the number inside an array can still change.
        final int[] hoverRow = {-1};
        // Create the table that displays the model's data.
        JTable table = new JTable(model);
        // Basic table style:
        // each row 28px tall; no vertical grid lines; light grid color; selected row colors; font;
        // fill the whole scroll area even with few rows; 1px vertical gap between rows.
        table.setRowHeight(28);
        table.setShowVerticalLines(false);
        table.setGridColor(Theme.BORDER);
        table.setSelectionBackground(Theme.ROW_SELECTED);
        table.setSelectionForeground(Theme.TEXT);
        table.setFont(Theme.BASE);
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new Dimension(0, 1));

        // The header is the row of column titles at the top.
        JTableHeader header = table.getTableHeader();
        // Don't let the user drag columns to reorder them.
        header.setReorderingAllowed(false);
        // A "renderer" decides how each header cell is drawn. We replace the default one.
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                // Let the parent class set up the text; pass false/false so no selection/focus styling is applied.
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                // Dark green background, white bold text, padding.
                setBackground(Theme.GREEN_DARK);
                setForeground(Color.WHITE);
                setFont(Theme.BOLD);
                setBorder(new EmptyBorder(8, 10, 8, 10));
                return this;
            }
        });
        // Header height: 36px (width 0 = automatic).
        header.setPreferredSize(new Dimension(0, 36));

        // The renderer for normal data cells (all cell types derive from Object).
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                // Set up the text and selection color using the parent's default behavior.
                super.getTableCellRendererComponent(t, v, sel, false, r, c);
                // 10px of padding on the left and right inside each cell.
                setBorder(new EmptyBorder(0, 10, 0, 10));
                // Text of the cell ("" if empty).
                String text = v == null ? "" : v.toString();
                // Treat the cell as a number if it is a Number object or already formatted as pesos (starts with ₱).
                boolean number = v instanceof Number || text.startsWith("₱");
                // Numbers are right-aligned (so digits line up); everything else is left-aligned.
                setHorizontalAlignment(number ? SwingConstants.RIGHT : SwingConstants.LEFT);
                // Row coloring (only when the row is NOT selected - selected rows keep the selection color):
                if (!sel) {
                    // Priority: total row color > hovered row color > alternating white / light stripe.
                    setBackground(isTotalRow(t, r) ? Theme.ROW_TOTAL
                            : r == hoverRow[0] ? Theme.ROW_HOVER : (r % 2 == 0 ? Color.WHITE : Theme.ROW_ALT));
                    setForeground(Theme.TEXT);
                }
                // Total rows (TOTAL, NET INCOME, ...) are bold.
                setFont(isTotalRow(t, r) ? Theme.BOLD : Theme.BASE);
                return this;
            }
        });

        // Mouse listener that tracks which row the mouse is over, for the hover highlight.
        MouseAdapter hover = new MouseAdapter() {
            // Mouse moved inside the table: find the row under the pointer and remember it.
            @Override public void mouseMoved(MouseEvent e) { setHover(table, hoverRow, table.rowAtPoint(e.getPoint())); }
            // Mouse left the table: clear the highlight (-1 = no row).
            @Override public void mouseExited(MouseEvent e) { setHover(table, hoverRow, -1); }
        };
        // Register the same listener for movement events and for enter/exit events.
        table.addMouseMotionListener(hover);
        table.addMouseListener(hover);
        return table;
    }

    /** Wraps a table in a scroll pane with a light border. */
    // A JTable does not scroll by itself or show its header unless it sits inside a JScrollPane.
    public static JScrollPane scroll(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        // White background in the empty area below the rows.
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    // Updates the remembered hover row and redraws the table, but only if the row actually changed (avoids needless repainting).
    private static void setHover(JTable table, int[] hoverRow, int row) {
        if (hoverRow[0] != row) {
            hoverRow[0] = row;
            table.repaint();
        }
    }

    /** Summary rows (TOTAL, NET INCOME, ...) are shown in bold. */
    // Decides whether a row is a summary row by looking at the text in its 2nd column (index 1).
    // In the 12-column worksheet that column holds the account name or labels like "GRAND TOTAL".
    private static boolean isTotalRow(JTable t, int viewRow) {
        // Tables with fewer than 2 columns have no label column to check.
        if (t.getColumnCount() < 2) return false;
        // The value in column index 1 of this row.
        Object v = t.getValueAt(viewRow, 1);
        if (v == null) return false;
        String s = v.toString();
        // True if the label begins with one of the special summary words.
        // (GRAND TOTAL already starts with "TOTAL"? No - it starts with "GRAND", hence the separate check.)
        return s.startsWith("TOTAL") || s.startsWith("GRAND TOTAL") || s.startsWith("NET INCOME") || s.startsWith("BALANCE DIFFERENCE");
    }
}
