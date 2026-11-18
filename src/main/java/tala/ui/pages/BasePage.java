package tala.ui.pages;

import tala.dao.AccountDAO;
import tala.dao.JournalDAO;
import tala.service.AccountingService;
import tala.ui.MainFrame;
import tala.ui.components.ScrollablePanel;
import tala.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * BasePage
 * ------------------------------------------------------------
 * What every page (Dashboard, Journal Entry, ...) has in common:
 *   - access to the DAOs and the AccountingService
 *   - a scrolling column where sections are stacked top to bottom
 *   - refreshData(), called every time the page is opened
 *   - helpers for showing messages
 */
public abstract class BasePage extends JPanel {

    protected final MainFrame mainFrame;
    protected final AccountDAO accountDAO;
    protected final JournalDAO journalDAO;
    protected final AccountingService accounting = new AccountingService();

    private final ScrollablePanel column = new ScrollablePanel(new GridBagLayout());
    private int nextRow = 0;

    protected BasePage(MainFrame mainFrame, AccountDAO accountDAO, JournalDAO journalDAO) {
        super(new BorderLayout());
        this.mainFrame = mainFrame;
        this.accountDAO = accountDAO;
        this.journalDAO = journalDAO;

        column.setBackground(Theme.PAGE_BG);
        column.setBorder(BorderFactory.createEmptyBorder(24, 28, 48, 28));

        JScrollPane scroll = new JScrollPane(column);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.PAGE_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        add(scroll, BorderLayout.CENTER);
    }

    /** Reloads the page's data from MySQL. Called each time the page is shown. */
    public abstract void refreshData();

    /** Adds a section (header, card, row of cards...) under the previous one */
    protected void addSection(JComponent section) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = nextRow++;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.insets = new Insets(0, 0, 20, 0);
        column.add(shareWidth(section), c);
    }

    /** Call once after the last addSection: pushes everything to the top */
    protected void finishSections() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = nextRow++;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        column.add(filler, c);
    }

    /**
     * Two columns side by side (e.g. a table on the left and a form on the right).
     * leftWeight / rightWeight decide how the width is shared.
     */
    protected JPanel twoColumns(JComponent left, double leftWeight, JComponent right, double rightWeight) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;

        c.gridx = 0;
        c.weightx = leftWeight;
        c.insets = new Insets(0, 0, 0, 10);
        row.add(shareWidth(left), c);

        c.gridx = 1;
        c.weightx = rightWeight;
        c.insets = new Insets(0, 10, 0, 0);
        row.add(shareWidth(right), c);
        return row;
    }

    /**
     * Wraps a component so it asks for (almost) no width of its own.
     * GridBagLayout then shares the width ONLY by the weights, e.g. 60% / 40%.
     */
    private JComponent shareWidth(JComponent child) {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            public java.awt.Dimension getPreferredSize() {
                return new java.awt.Dimension(1, super.getPreferredSize().height);
            }

            @Override
            public java.awt.Dimension getMinimumSize() {
                return getPreferredSize();
            }
        };
        wrapper.setOpaque(false);
        wrapper.add(child, BorderLayout.CENTER);
        return wrapper;
    }

    /** A column of components with a gap between them (used inside twoColumns) */
    protected JPanel stack(JComponent... parts) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        for (int i = 0; i < parts.length; i++) {
            c.gridy = i;
            c.insets = new Insets(0, 0, i < parts.length - 1 ? 20 : 0, 0);
            panel.add(parts[i], c);
        }
        // an invisible filler at the bottom keeps the parts at the top
        c.gridy = parts.length;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 0);
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        panel.add(filler, c);
        return panel;
    }

    // ------------------------------------------------------------ messages

    protected void showError(String message, Exception e) {
        String details = e == null ? "" : "\n\nDetails: " + e.getMessage();
        JOptionPane.showMessageDialog(this, message + details, "Something went wrong", JOptionPane.ERROR_MESSAGE);
    }

    protected void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "TALA", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Yes/No question. Returns true if the user picked the first (yes) button. */
    protected boolean confirm(String title, String message, String yesText, String noText) {
        Object[] options = {yesText, noText};
        int choice = JOptionPane.showOptionDialog(this, message, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        return choice == 0;
    }
}
