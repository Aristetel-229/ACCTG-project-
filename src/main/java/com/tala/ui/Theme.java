package com.tala.ui;

// Color and Font classes from AWT are used to define the look of the app.
import java.awt.Color;
import java.awt.Font;

/**
 * Theme holds all shared colors and fonts as constants so every screen looks the same.
 * Change a value here and the whole application is re-skinned. It has no accounting logic.
 */
public final class Theme {
    // Private constructor: this is a utility class, so nobody should create a Theme object.
    private Theme() {}

    // Core green palette
    // Darkest green: used for the sidebar background and titles.
    public static final Color GREEN_DARK = new Color(20, 83, 57);
    // Slightly lighter green: shown when the mouse hovers over a sidebar button.
    public static final Color GREEN_HOVER = new Color(31, 111, 76);
    // Even lighter green: shown for the currently selected sidebar button.
    public static final Color GREEN_ACTIVE = new Color(40, 130, 87);
    // Very light green: used for soft highlights.
    public static final Color GREEN_LIGHT = new Color(232, 244, 235);
    // Page background color behind the content.
    public static final Color BACKGROUND = new Color(243, 248, 244);
    // Color for thin borders and lines.
    public static final Color BORDER = new Color(218, 231, 220);
    // Main text color (dark green-gray, easy to read).
    public static final Color TEXT = new Color(37, 56, 43);
    // Gray-green for hints and less important text.
    public static final Color MUTED = new Color(103, 125, 108);
    // Light text color used on the dark sidebar.
    public static final Color SIDEBAR_TEXT = new Color(204, 228, 211);

    // Actions
    // Normal color of main buttons (e.g. Save, Post).
    public static final Color PRIMARY = new Color(34, 125, 79);
    // Main button color on mouse hover.
    public static final Color PRIMARY_HOVER = new Color(27, 101, 63);
    // Main button color while being pressed.
    public static final Color PRIMARY_PRESSED = new Color(20, 83, 57);
    // Normal color of secondary buttons (e.g. Refresh).
    public static final Color SECONDARY = new Color(224, 237, 226);
    // Secondary button color on hover.
    public static final Color SECONDARY_HOVER = new Color(204, 224, 207);
    // Red used for dangerous actions (Reset Data) and for errors/net loss.
    public static final Color DANGER = new Color(185, 40, 40);
    // Darker red on hover for danger buttons.
    public static final Color DANGER_HOVER = new Color(155, 28, 28);

    // Table colors
    // Background for alternating (zebra) table rows so rows are easy to follow.
    public static final Color ROW_ALT = new Color(246, 250, 247);
    // Row color when the mouse is over it.
    public static final Color ROW_HOVER = new Color(230, 242, 232);
    // Row color when the user selects it.
    public static final Color ROW_SELECTED = new Color(198, 226, 203);
    // Background for total rows (e.g. totals of debit and credit columns).
    public static final Color ROW_TOTAL = new Color(214, 234, 219);

    // Fonts
    // Regular text font, size 14.
    public static final Font BASE = new Font("SansSerif", Font.PLAIN, 14);
    // Bold text font, size 14 (labels, totals).
    public static final Font BOLD = new Font("SansSerif", Font.BOLD, 14);
    // Large bold font for page titles.
    public static final Font TITLE = new Font("SansSerif", Font.BOLD, 24);
    // Smaller font for the hint line under the title.
    public static final Font HINT = new Font("SansSerif", Font.PLAIN, 13);
}
