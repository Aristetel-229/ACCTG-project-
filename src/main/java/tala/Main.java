package tala;

import tala.dao.AccountDAO;
import tala.dao.JournalDAO;
import tala.dao.SettingsDAO;
import tala.database.DBConnection;
import tala.database.DatabaseSetup;
import tala.ui.MainFrame;
import tala.ui.theme.Theme;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;
import java.sql.SQLException;

/**
 * Main  -  START THE PROGRAM HERE
 * ------------------------------------------------------------
 *   1. Use the computer's normal window style
 *   2. Prepare the MySQL database (creates it the first time)
 *   3. Open the main window
 *
 * Project layout (where to look for what):
 *   tala.database  -> connecting to MySQL, creating the tables
 *   tala.model     -> plain data classes (Account, JournalEntry, ...)
 *   tala.dao       -> all the SQL (read/save accounts and entries)
 *   tala.service   -> all the accounting math (ledger, worksheet, totals)
 *   tala.util      -> money and date helpers
 *   tala.ui        -> the screens (pages) and reusable UI pieces
 */
public class Main {

    public static void main(String[] args) {
        // Everything that touches Swing should run on the "Event Dispatch Thread"
        SwingUtilities.invokeLater(Main::start);
    }

    private static void start() {
        useSystemLookAndFeel();
        Theme.checkPesoSign();

        try {
            DatabaseSetup.prepareDatabase();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "TALA could not connect to MySQL.\n\n"
                    + "Please check:\n"
                    + "  1. MySQL is running (in XAMPP, press Start on MySQL).\n"
                    + "  2. The username and password in tala/database/DBConnection.java\n"
                    + "     (now: user \"" + DBConnection.USERNAME + "\" on " + DBConnection.HOST + ":" + DBConnection.PORT + ").\n"
                    + "  3. mysql-connector-j-<version>.jar is in the lib folder.\n\n"
                    + "Details: " + e.getMessage(),
                    "Cannot connect to the database", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }

        MainFrame frame = new MainFrame(new AccountDAO(), new JournalDAO(), new SettingsDAO());
        frame.setVisible(true);
    }

    /** Windows look on Windows, Mac look on Mac; our fonts on top */
    private static void useSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // the default Java look is fine too
        }
        Font font = Theme.regular(15);
        UIManager.put("OptionPane.messageFont", font);
        UIManager.put("OptionPane.buttonFont", Theme.bold(14));
        UIManager.put("ComboBox.font", Theme.regular(16));
        // tooltips: dark purple box with white text
        UIManager.put("ToolTip.font", Theme.regular(14));
        UIManager.put("ToolTip.background", Theme.TITLE);
        UIManager.put("ToolTip.foreground", java.awt.Color.WHITE);
        UIManager.put("ToolTip.border", javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(Theme.TITLE),
                javax.swing.BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        javax.swing.ToolTipManager.sharedInstance().setInitialDelay(250);   // show tips quickly
        javax.swing.ToolTipManager.sharedInstance().setDismissDelay(15000);
    }
}
