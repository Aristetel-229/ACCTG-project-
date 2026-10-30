// This file belongs to the base package "com.tala".
package com.tala;

// Swing helpers: SwingUtilities runs code on the UI thread, UIManager controls the look and feel.
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

// The main application window (sidebar + pages) that we open at startup.
import com.tala.ui.MainFrame;

/**
 * Entry point of the TALA accounting app.
 * It sets the native look and feel, then creates and shows the main window.
 */
public class Main {
    // Java starts running the program here.
    public static void main(String[] args) {
        // Swing is not thread-safe, so we build the UI on the Event Dispatch Thread (the UI thread).
        SwingUtilities.invokeLater(() -> {
            try {
                // Make the app look like the user's operating system (Windows, Mac, etc.).
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // If the look and feel fails to load, just ignore it and use Java's default look.
            } catch (Exception ignored) {}
            // Create the main window object (this builds all pages and menus).
            MainFrame frame = new MainFrame();
            // Make the window visible on screen.
            frame.setVisible(true);
        });
    }
}
