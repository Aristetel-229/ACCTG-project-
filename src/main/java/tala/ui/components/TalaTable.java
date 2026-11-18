package tala.ui.components;

import tala.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.HashSet;
import java.util.Set;

/**
 * TalaTable
 * ------------------------------------------------------------
 * A JTable with the TALA look: tall rows, purple header,
 * right-aligned money columns and colored "pill" columns.
 *
 * Pages can override the small "hook" methods below to color
 * or bold special rows (for example a Total row).
 */
public class TalaTable extends JTable {

    private final Set<Integer> rightAligned = new HashSet<>();
    private final Set<Integer> pillColumns = new HashSet<>();
    private float fontSize = 15.5f;
    private int cellPadding = 16;

    public TalaTable(TableModel model) {
        super(model);
        setFont(Theme.regular(15.5f));
        setForeground(Theme.TEXT);
        setRowHeight(42);
        setShowVerticalLines(false);
        setShowHorizontalLines(false);          // our renderer draws the row lines
        setIntercellSpacing(new Dimension(0, 0));
        setFillsViewportHeight(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setDefaultRenderer(Object.class, new Renderer());

        JTableHeader header = getTableHeader();
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer());
        header.setPreferredSize(new Dimension(10, 42));
    }

    // ------------------------------------------------------------ setup helpers

    /** Money columns: right-aligned numbers */
    public void setRightAligned(int... columns) {
        for (int c : columns) {
            rightAligned.add(c);
        }
    }

    /** Columns whose text is drawn as a colored pill (types, references, status) */
    public void setPillColumns(int... columns) {
        for (int c : columns) {
            pillColumns.add(c);
        }
    }

    /** Smaller text and padding, for wide tables like the 12 Columnar worksheet */
    public void setCompact(float fontSize, int cellPadding) {
        this.fontSize = fontSize;
        this.cellPadding = cellPadding;
        setFont(Theme.regular(fontSize));
    }

    /** Preferred widths in pixels; use 0 to leave a column flexible */
    public void setColumnWidths(int... widths) {
        for (int i = 0; i < widths.length && i < getColumnCount(); i++) {
            if (widths[i] > 0) {
                TableColumn col = getColumnModel().getColumn(i);
                col.setPreferredWidth(widths[i]);
                col.setMaxWidth(widths[i] + 60);
            }
        }
    }

    /** The table plus its header in one panel, without a scroll bar (the page scrolls instead) */
    public JComponent inPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(getTableHeader(), BorderLayout.NORTH);
        panel.add(this, BorderLayout.CENTER);
        return panel;
    }

    // ------------------------------------------------------------ hooks (override in pages)

    /** Background of a row. Default: white, or cream when selected. */
    protected Color rowBackground(int row, boolean selected) {
        return selected ? Theme.CREAM : Color.WHITE;
    }

    /** Background of one cell. Default: the row's background. */
    protected Color cellBackground(int row, int column, boolean selected) {
        return rowBackground(row, selected);
    }

    /** Should this row be bold? */
    protected boolean rowBold(int row) {
        return false;
    }

    /** Text color of one cell */
    protected Color cellForeground(int row, int column) {
        return Theme.TEXT;
    }

    /** Extra left padding for one cell (used to indent credit lines) */
    protected int cellIndent(int row, int column) {
        return 0;
    }

    /** Pill colors for a text: { background, text color } */
    protected Color[] pillColors(String text) {
        switch (text) {
            case "Asset":     return new Color[] {Theme.PURPLE_TINT, Theme.PURPLE};
            case "Liability": return new Color[] {Theme.AMBER_BG, Theme.AMBER};
            case "Equity":    return new Color[] {Theme.BLUE_BG, Theme.BLUE};
            case "Revenue":   return new Color[] {Theme.GREEN_BG, Theme.GREEN};
            case "Expense":   return new Color[] {Theme.RED_BG, Theme.RED};
            case "Adjusting":
            case "New":       return new Color[] {Theme.AMBER_BG, Theme.AMBER};
            case "Active":    return new Color[] {Theme.GREEN_BG, Theme.GREEN};
            default:          return new Color[] {Theme.PURPLE_TINT, Theme.PURPLE};   // references, "Regular"
        }
    }

    // ------------------------------------------------------------ renderers

    /** Draws normal cells and pill cells */
    private class Renderer extends DefaultTableCellRenderer {
        private final PillLabel pill = new PillLabel();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            String text = value == null ? "" : value.toString();
            int modelColumn = convertColumnIndexToModel(column);
            Color background = cellBackground(row, modelColumn, selected);

            if (pillColumns.contains(modelColumn) && !text.isEmpty()) {
                Color[] colors = pillColors(text);
                pill.setup(text, colors[0], colors[1], background);
                return pill;
            }

            super.getTableCellRendererComponent(table, text, false, false, row, column);
            setFont(rowBold(row) ? Theme.bold(fontSize) : Theme.regular(fontSize));
            setForeground(cellForeground(row, modelColumn));
            setBackground(background);
            setHorizontalAlignment(rightAligned.contains(modelColumn) ? SwingConstants.RIGHT : SwingConstants.LEFT);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.ROW_LINE),
                    BorderFactory.createEmptyBorder(0, cellPadding + cellIndent(row, modelColumn), 0, cellPadding)));
            return this;
        }
    }

    /** A small rounded label used inside table cells */
    private static class PillLabel extends JLabel {
        private Color pillColor;
        private Color cellColor;

        PillLabel() {
            setFont(Theme.bold(13.5f));
            setOpaque(false);
        }

        void setup(String text, Color pillColor, Color textColor, Color cellColor) {
            setText(text);
            this.pillColor = pillColor;
            this.cellColor = cellColor;
            setForeground(textColor);
            setBorder(BorderFactory.createEmptyBorder(0, 26, 0, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(cellColor);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Theme.ROW_LINE);
            g2.fillRect(0, getHeight() - 1, getWidth(), 1);

            int textWidth = g2.getFontMetrics(getFont()).stringWidth(getText());
            int pillHeight = 24;
            int y = (getHeight() - pillHeight) / 2;
            g2.setColor(pillColor);
            g2.fillRoundRect(16, y, textWidth + 20, pillHeight, pillHeight, pillHeight);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Purple-tinted column headers */
    private class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setFont(Theme.bold(14.5f));
            setForeground(Theme.TABLE_HEAD);
            setBackground(Theme.PURPLE_TINT);
            setOpaque(true);
            int modelColumn = convertColumnIndexToModel(column);
            setHorizontalAlignment(rightAligned.contains(modelColumn) ? SwingConstants.RIGHT : SwingConstants.LEFT);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xe6def5)),
                    BorderFactory.createEmptyBorder(0, 16, 0, 16)));
            return this;
        }
    }
}
