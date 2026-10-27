// Package for the UI framework classes.
package com.tala.ui;

// The sidebar button class.
import com.tala.ui.components.NavButton;
// All five page classes (DashboardPage, JournalEntryPage, ...).
import com.tala.ui.pages.*;

// Swing and AWT classes.
import javax.swing.*;
// EmptyBorder = invisible padding around a component.
import javax.swing.border.EmptyBorder;
import java.awt.*;
// A Map stores key -> value pairs; HashMap is the usual implementation.
import java.util.HashMap;
import java.util.Map;

/** Application window: sidebar navigation, header, content area and status bar. */
// JFrame = a normal desktop window. MainFrame IS a window, so we extend it.
public class MainFrame extends JFrame {
    // The big area on the right where the current page is displayed. 12px gaps between regions.
    private final JPanel content = new JPanel(new BorderLayout(12, 12));
    // The text at the very bottom of the window (e.g. "Connected to MySQL").
    private final JLabel status = new JLabel(" Ready");
    // Remembers each sidebar button by the page class name, so we can highlight the active one.
    private final Map<String, NavButton> navButtons = new HashMap<>();

    // Constructor: builds the whole window when "new MainFrame()" is called in Main.
    public MainFrame() {
        // Text shown in the window's title bar.
        setTitle("TALA");
        // Clicking the X closes the entire program (not just hides the window).
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Starting size: 1180 wide x 760 tall pixels.
        setSize(1180, 760);
        // The user cannot shrink the window smaller than this.
        setMinimumSize(new Dimension(960, 620));
        // Passing null centers the window on the screen.
        setLocationRelativeTo(null);
        // The window is split into regions (west, center, ...).
        setLayout(new BorderLayout());

        // Left side: the green navigation sidebar.
        add(buildSidebar(), BorderLayout.WEST);
        // Everything else (header, page content, status bar) fills the center.
        add(buildRightPanel(), BorderLayout.CENTER);
        // Show the Dashboard as the first page when the app opens.
        new DashboardPage(this).show();
    }

    // Builds the dark green sidebar with the menu buttons.
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Theme.GREEN_DARK);
        // Fixed width of 220px; height 0 means "let the layout decide".
        sidebar.setPreferredSize(new Dimension(220, 0));
        // BoxLayout.Y_AXIS = stack the items vertically, top to bottom.
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        // Padding inside the sidebar: top 18, left 12, bottom 18, right 12.
        sidebar.setBorder(new EmptyBorder(18, 12, 18, 12));

        // The "TALA" logo text at the top of the sidebar.
        JLabel brand = new JLabel("  TALA");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("SansSerif", Font.BOLD, 22));
        // Extra space (24px) below the logo.
        brand.setBorder(new EmptyBorder(4, 0, 24, 0));
        sidebar.add(brand);

        // One addNav(...) call per menu item. Arguments:
        //   sidebar, button text, page class name (the key), tooltip text, and what to do when clicked.
        // "() -> new XPage(this).show()" is a lambda: it creates the page and displays it on click.
        addNav(sidebar, "Dashboard", "DashboardPage", "Totals at a glance",
                () -> new DashboardPage(this).show());
        addNav(sidebar, "Journal Entry", "JournalEntryPage", "Record a transaction",
                () -> new JournalEntryPage(this).show());
        addNav(sidebar, "Chart of Accounts", "ChartOfAccountsPage", "The list of all accounts",
                () -> new ChartOfAccountsPage(this).show());
        addNav(sidebar, "Ledger", "GeneralLedgerPage", "Every posted debit and credit",
                () -> new GeneralLedgerPage(this).show());
        addNav(sidebar, "12 Columnar", "TwelveColumnWorksheetPage", "Trial balances, adjustments, income statement and balance sheet",
                () -> new TwelveColumnWorksheetPage(this).show());
        // "Glue" is flexible empty space that pushes the footer to the bottom of the sidebar.
        sidebar.add(Box.createVerticalGlue());

        // Small "Student Edition" label at the bottom.
        JLabel footer = new JLabel("Student Edition");
        footer.setForeground(Theme.SIDEBAR_TEXT);
        footer.setBorder(new EmptyBorder(10, 4, 0, 4));
        sidebar.add(footer);
        return sidebar;
    }

    // Builds the right side: white header strip (top), page content (middle), status bar (bottom).
    private JPanel buildRightPanel() {
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(Theme.BACKGROUND);

        // The thin white header bar showing "TALA" in green.
        JLabel top = new JLabel("  TALA");
        top.setFont(new Font("SansSerif", Font.BOLD, 13));
        top.setBorder(new EmptyBorder(15, 12, 15, 12));
        // A JLabel is transparent by default; setOpaque(true) is needed for the background color to show.
        top.setOpaque(true);
        top.setBackground(Color.WHITE);
        top.setForeground(Theme.GREEN_DARK);
        right.add(top, BorderLayout.NORTH);

        // The page area: padded 18px on every side, with the soft background color.
        content.setBorder(new EmptyBorder(18, 18, 18, 18));
        content.setBackground(Theme.BACKGROUND);
        right.add(content, BorderLayout.CENTER);

        // The status bar at the bottom (white background, small padding).
        status.setBorder(new EmptyBorder(8, 12, 8, 12));
        status.setOpaque(true);
        status.setBackground(Color.WHITE);
        right.add(status, BorderLayout.SOUTH);
        return right;
    }

    // Creates one sidebar button, wires its click action, and adds it to the sidebar.
    private void addNav(JPanel sidebar, String label, String key, String tooltip, Runnable action) {
        NavButton button = new NavButton(label);
        // The little popup text shown when hovering over the button.
        button.setToolTipText(tooltip);
        // On click, run the action that was passed in (opens the page).
        button.addActionListener(e -> action.run());
        // Remember the button under its page name so setPage() can highlight it later.
        navButtons.put(key, button);
        sidebar.add(button);
        // 4px of vertical space between buttons.
        sidebar.add(Box.createVerticalStrut(4));
    }

    /** Replaces the current page content (called by {@link BasePage#show()}). */
    public void setPage(String navKey, String title, String hint, JComponent component) {
        // Highlight only the button whose key matches the page being shown; un-highlight the others.
        navButtons.forEach((key, button) -> button.setActive(key.equals(navKey)));
        // Remove the previous page from the screen.
        content.removeAll();

        // The heading block (title + optional subtitle) stacked vertically.
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        // The big page title.
        JLabel label = new JLabel(title);
        label.setFont(Theme.TITLE);
        label.setForeground(Theme.GREEN_DARK);
        // Align to the left edge (BoxLayout centers by default).
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(label);
        // Only show the subtitle when the page provided a non-empty hint.
        if (!hint.isBlank()) {
            JLabel sub = new JLabel(hint);
            sub.setFont(Theme.HINT);
            sub.setForeground(Theme.MUTED);
            sub.setBorder(new EmptyBorder(2, 0, 0, 0));
            sub.setAlignmentX(Component.LEFT_ALIGNMENT);
            heading.add(sub);
        }
        // Put the heading on top and the page component below it.
        content.add(heading, BorderLayout.NORTH);
        content.add(component, BorderLayout.CENTER);
        // Tell Swing the layout changed (revalidate) and to redraw the screen (repaint).
        content.revalidate();
        content.repaint();
    }

    // Changes the text in the bottom status bar. Called by pages after loading data or on errors.
    public void setStatus(String text) {
        status.setText(text);
    }
}
