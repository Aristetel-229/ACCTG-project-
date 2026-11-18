package tala.ui;

import tala.dao.AccountDAO;
import tala.dao.JournalDAO;
import tala.dao.SettingsDAO;
import tala.ui.components.Icons;
import tala.ui.components.TopNavBar;
import tala.ui.pages.BasePage;
import tala.ui.pages.ChartOfAccountsPage;
import tala.ui.pages.ColumnarPage;
import tala.ui.pages.DashboardPage;
import tala.ui.pages.GeneralJournalPage;
import tala.ui.pages.GeneralLedgerPage;
import tala.ui.pages.JournalEntryPage;
import tala.ui.theme.AppImages;
import tala.ui.theme.Theme;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MainFrame
 * ------------------------------------------------------------
 * The main window. It holds:
 *   - the purple TopNavBar
 *   - all pages, stacked in a CardLayout (only one is visible at a time)
 *
 * showPage("Journal Entry") switches pages and reloads that page's data.
 */
public class MainFrame extends JFrame {

    // Page names (also used as the tab labels)
    public static final String DASHBOARD = "Dashboard";
    public static final String JOURNAL_ENTRY = "Journal Entry";
    public static final String GENERAL_JOURNAL = "General Journal";
    public static final String CHART_OF_ACCOUNTS = "Chart of Accounts";
    public static final String GENERAL_LEDGER = "General Ledger";
    public static final String COLUMNAR = "12 Columnar";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel pageArea = new JPanel(cardLayout);
    private final TopNavBar navBar = new TopNavBar();
    private final Map<String, BasePage> pages = new LinkedHashMap<>();

    public MainFrame(AccountDAO accountDAO, JournalDAO journalDAO, SettingsDAO settingsDAO) {
        super("TALA - Accounting System (Student Edition)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720));
        setSize(1366, 860);
        setLocationRelativeTo(null);   // center on screen
        setIconImages(AppImages.getWindowIcons());   // the TALA logo in the title bar and taskbar

        // 1) create every page once
        addPage(DASHBOARD, Icons.DASHBOARD, new DashboardPage(this, accountDAO, journalDAO));
        addPage(JOURNAL_ENTRY, Icons.PEN, new JournalEntryPage(this, accountDAO, journalDAO, settingsDAO));
        addPage(GENERAL_JOURNAL, Icons.BOOK, new GeneralJournalPage(this, accountDAO, journalDAO));
        addPage(CHART_OF_ACCOUNTS, Icons.LIST, new ChartOfAccountsPage(this, accountDAO, journalDAO));
        addPage(GENERAL_LEDGER, Icons.LAYERS, new GeneralLedgerPage(this, accountDAO, journalDAO));
        addPage(COLUMNAR, Icons.TABLE, new ColumnarPage(this, accountDAO, journalDAO));

        // 2) put the window together
        pageArea.setBackground(Theme.PAGE_BG);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(navBar, BorderLayout.NORTH);
        getContentPane().add(pageArea, BorderLayout.CENTER);

        // 3) start on the Dashboard
        showPage(DASHBOARD);
    }

    private void addPage(final String name, String iconName, BasePage page) {
        pages.put(name, page);
        pageArea.add(page, name);
        navBar.addTab(name, iconName, e -> showPage(name));
    }

    /** Switches to a page and reloads its data */
    public void showPage(String name) {
        BasePage page = pages.get(name);
        if (page == null) {
            return;
        }
        page.refreshData();
        cardLayout.show(pageArea, name);
        navBar.setActive(name);
    }

    /** Opens the 12 Columnar page and its Add adjustment pop-up */
    public void openAddAdjustment() {
        showPage(COLUMNAR);
        ((ColumnarPage) pages.get(COLUMNAR)).openAdjustmentDialog();
    }

    public void setDatabaseStatus(boolean connected) {
        navBar.setConnected(connected);
    }
}
