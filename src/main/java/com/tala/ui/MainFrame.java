package com.tala.ui;

// AWT classes for layout, colors, sizes and fonts.
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
// Collections used to remember the sidebar buttons by name.
import java.util.HashMap;
import java.util.Map;

// Swing components used to build the window.
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

// The custom sidebar button and all the pages that the sidebar can open.
import com.tala.ui.components.NavButton;
import com.tala.ui.pages.ChartOfAccountsPage;
import com.tala.ui.pages.DashboardPage;
import com.tala.ui.pages.GeneralJournalPage;
import com.tala.ui.pages.GeneralLedgerPage;
import com.tala.ui.pages.JournalEntryPage;
import com.tala.ui.pages.TwelveColumnWorksheetPage;

/**
 * Application window: sidebar navigation, header, content area and status bar.
 * Pages (Dashboard, Journal Entry, etc.) are swapped into the content area when a sidebar button is clicked.
 */
public class MainFrame extends JFrame {
    // The area in the middle where the current page is displayed (12 pixel gaps between its regions).
    private final JPanel content = new JPanel(new BorderLayout(12, 12));
    // Status bar text at the bottom (shows e.g. database connection state).
    private final JLabel status = new JLabel(" Ready");
    // Remembers each sidebar button by the page key so we can highlight the active one.
    private final Map<String, NavButton> navButtons = new HashMap<>();

    // Constructor: sets up the window and shows the Dashboard first.
    public MainFrame() {
        // Text in the window's title bar.
        setTitle("TALA");
        // Closing the window ends the whole program.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Starting size of the window in pixels (width, height).
        setSize(1280, 760);
        // The user cannot shrink the window below this size (keeps the layout readable).
        setMinimumSize(new Dimension(960, 620));
        // Passing null centers the window on the screen.
        setLocationRelativeTo(null);
        // The window uses BorderLayout: sidebar on the west, main area in the center.
        setLayout(new BorderLayout());

        // Add the left sidebar menu.
        add(buildSidebar(), BorderLayout.WEST);
        // Add the right side (header, content, status bar).
        add(buildRightPanel(), BorderLayout.CENTER);
        // Show the Dashboard as the opening page.
        new DashboardPage(this).show();
    }

    // Builds the dark green navigation menu on the left.
    private JPanel buildSidebar() {
        // Create the panel.
        JPanel sidebar = new JPanel();
        // Dark green background from the theme.
        sidebar.setBackground(Theme.GREEN_DARK);
        // Fixed width of 220; height 0 because the layout stretches it to full height.
        sidebar.setPreferredSize(new Dimension(220, 0));
        // Stack the items vertically (Y axis).
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        // Empty space inside the panel edges (top, left, bottom, right).
        sidebar.setBorder(new EmptyBorder(18, 12, 18, 12));

        // The app name label shown at the top of the sidebar.
        JLabel brand = new JLabel("  TALA");
        // White text, bold 22 point font.
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("SansSerif", Font.BOLD, 22));
        // Extra space below the brand name.
        brand.setBorder(new EmptyBorder(4, 0, 24, 0));
        // Put the brand in the sidebar.
        sidebar.add(brand);

        // Each addNav call creates one menu button: label, page key, tooltip, and what happens on click.
        // Dashboard: totals of assets, liabilities, revenue, expenses.
        addNav(sidebar, "Dashboard", "DashboardPage", "Totals at a glance",
                () -> new DashboardPage(this).show());
        // Journal Entry: form for recording a new transaction (debits and credits).
        addNav(sidebar, "Journal Entry", "JournalEntryPage", "Record a transaction",
                () -> new JournalEntryPage(this).show());
        // General Journal: the book of original entry, listing all entries by date.
        addNav(sidebar, "General Journal", "GeneralJournalPage", "Every entry in date order, with totals",
                () -> new GeneralJournalPage(this).show());
        // Chart of Accounts: the list of accounts that can be used.
        addNav(sidebar, "Chart of Accounts", "ChartOfAccountsPage", "The list of all accounts",
                () -> new ChartOfAccountsPage(this).show());
        // General Ledger: transactions grouped per account.
        addNav(sidebar, "General Ledger", "GeneralLedgerPage", "A separate ledger for each account you used",
                () -> new GeneralLedgerPage(this).show());
        // 12 Columnar: the 12-column worksheet (trial balances, adjustments, financial statements).
        addNav(sidebar, "12 Columnar", "TwelveColumnWorksheetPage", "Trial balances, adjustments, income statement and balance sheet",
                () -> new TwelveColumnWorksheetPage(this).show());
        // Flexible empty space that pushes the footer to the bottom.
        sidebar.add(Box.createVerticalGlue());

        // Small footer label at the bottom of the sidebar.
        JLabel footer = new JLabel("Student Edition");
        // Light text color made for the dark background.
        footer.setForeground(Theme.SIDEBAR_TEXT);
        // Small spacing around the footer text.
        footer.setBorder(new EmptyBorder(10, 4, 0, 4));
        // Add the footer.
        sidebar.add(footer);
        // Return the finished sidebar.
        return sidebar;
    }

    // Builds the right side: top header, page content area and bottom status bar.
    private JPanel buildRightPanel() {
        // Container with BorderLayout (north, center, south).
        JPanel right = new JPanel(new BorderLayout());
        // Light background color from the theme.
        right.setBackground(Theme.BACKGROUND);

        // Top header label.
        JLabel top = new JLabel("  TALA");
        // Small bold font.
        top.setFont(new Font("SansSerif", Font.BOLD, 13));
        // Padding around the text.
        top.setBorder(new EmptyBorder(15, 12, 15, 12));
        // Opaque is required so the white background is actually painted.
        top.setOpaque(true);
        // White header with dark green text.
        top.setBackground(Color.WHITE);
        top.setForeground(Theme.GREEN_DARK);
        // Place the header at the top.
        right.add(top, BorderLayout.NORTH);

        // Padding around the page content area.
        content.setBorder(new EmptyBorder(18, 18, 18, 18));
        // Same background as the rest of the app.
        content.setBackground(Theme.BACKGROUND);
        // The content fills the center; pages are placed here by setPage.
        right.add(content, BorderLayout.CENTER);

        // Padding for the status bar text.
        status.setBorder(new EmptyBorder(8, 12, 8, 12));
        // Opaque and white so it looks like a bar.
        status.setOpaque(true);
        status.setBackground(Color.WHITE);
        // Place the status bar at the bottom.
        right.add(status, BorderLayout.SOUTH);
        // Return the finished right panel.
        return right;
    }

    // Creates one sidebar button, registers it, and adds it to the sidebar.
    private void addNav(JPanel sidebar, String label, String key, String tooltip, Runnable action) {
        // Custom button with the given text.
        NavButton button = new NavButton(label);
        // Text shown when the mouse rests on the button.
        button.setToolTipText(tooltip);
        // When clicked, run the action (open the page).
        button.addActionListener(e -> action.run());
        // Store the button under its key so setPage can highlight it later.
        navButtons.put(key, button);
        // Add it to the sidebar.
        sidebar.add(button);
        // 4 pixel gap before the next button.
        sidebar.add(Box.createVerticalStrut(4));
    }

    /** Replaces the current page content (called by {@link BasePage#show()}). */
    public void setPage(String navKey, String title, String hint, JComponent component) {
        // Mark only the button whose key matches the opened page as active (highlighted).
        navButtons.forEach((key, button) -> button.setActive(key.equals(navKey)));
        // Remove the previous page from the content area.
        content.removeAll();

        // Panel that holds the page title and the hint text.
        JPanel heading = new JPanel();
        // Transparent so the background shows through.
        heading.setOpaque(false);
        // Stack title and hint vertically.
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        // The big page title.
        JLabel label = new JLabel(title);
        // Title font and color from the theme.
        label.setFont(Theme.TITLE);
        label.setForeground(Theme.GREEN_DARK);
        // Align to the left edge.
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Add the title to the heading.
        heading.add(label);
        // Only show a hint line if the page provided a non-empty hint.
        if (!hint.isBlank()) {
            // The small explanation under the title.
            JLabel sub = new JLabel(hint);
            // Smaller, muted style.
            sub.setFont(Theme.HINT);
            sub.setForeground(Theme.MUTED);
            // Small gap above the hint.
            sub.setBorder(new EmptyBorder(2, 0, 0, 0));
            // Align left like the title.
            sub.setAlignmentX(Component.LEFT_ALIGNMENT);
            // Add the hint under the title.
            heading.add(sub);
        }
        // Title and hint go at the top of the content area.
        content.add(heading, BorderLayout.NORTH);
        // The page itself fills the rest.
        content.add(component, BorderLayout.CENTER);
        // Tell Swing to recalculate the layout because components changed.
        content.revalidate();
        // Redraw the area so the new page appears.
        content.repaint();
    }

    // Lets pages change the status bar text (e.g. " Connected to MySQL").
    public void setStatus(String text) {
        status.setText(text);
    }
}
