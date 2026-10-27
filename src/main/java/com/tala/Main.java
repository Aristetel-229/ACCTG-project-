// Declares the package (folder) this class belongs to: com.tala
package com.tala;

// Import the main application window class so we can create it below.
import com.tala.ui.MainFrame;

// Import all Swing classes (SwingUtilities, UIManager, etc.) used for the desktop GUI.
import javax.swing.*;

/**
 * ENTRY POINT of the whole application.
 * When you run the program, Java starts here (the main method).
 * Its only job is to set the look of the app and open the main window.
 */
public class Main {
    // Every Java program starts at "public static void main(String[] args)".
    public static void main(String[] args) {
        // Swing is NOT thread-safe: all GUI work must run on the special "Event Dispatch Thread".
        // invokeLater schedules the code inside the lambda () -> { ... } to run on that thread.
        SwingUtilities.invokeLater(() -> {
            try {
                // Make the app look like a native Windows app (buttons, dialogs, scrollbars)
                // instead of Java's older default "Metal" look.
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            // If setting the look fails for any reason, we ignore it and keep the default look.

            // Create the main window (sidebar + pages). This also loads the Dashboard.
            MainFrame frame = new MainFrame();
            // Windows are hidden by default; this makes it appear on screen.
            frame.setVisible(true);
        });
    }
}
