package tala.ui.components;

import javax.swing.table.DefaultTableModel;

/**
 * ReadOnlyTableModel
 * ------------------------------------------------------------
 * A normal DefaultTableModel, except the user cannot type into the cells.
 * (Changes go through forms and buttons, never by editing the table.)
 */
public class ReadOnlyTableModel extends DefaultTableModel {

    public ReadOnlyTableModel(String... columnNames) {
        super(columnNames, 0);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    /** Removes every row (used before reloading the data) */
    public void clear() {
        setRowCount(0);
    }
}
