// Package for everything related to the user interface (windows, pages, colors).
package com.tala.ui;

// Color = an RGB color; Font = a text style (family, weight, size).
import java.awt.Color;
import java.awt.Font;

/** Shared UI colors and fonts. Change them here to re-skin the whole app. */
public final class Theme {
    // Static-only class: just a holder for constants.
    private Theme() {}

    // Core green palette
    // new Color(red, green, blue) - each value goes from 0 to 255.
    // Darkest green: sidebar background, table headers, page titles.
    public static final Color GREEN_DARK = new Color(20, 83, 57);
    // Sidebar button color while the mouse is over it.
    public static final Color GREEN_HOVER = new Color(31, 111, 76);
    // Sidebar button color for the page you are currently on.
    public static final Color GREEN_ACTIVE = new Color(40, 130, 87);
    // Very light green: background of the tip box on the Journal Entry page.
    public static final Color GREEN_LIGHT = new Color(232, 244, 235);
    // Overall window background (a pale green-white).
    public static final Color BACKGROUND = new Color(243, 248, 244);
    // Thin border lines around tables, cards and text fields.
    public static final Color BORDER = new Color(218, 231, 220);
    // Main text color (dark green-gray, softer than pure black).
    public static final Color TEXT = new Color(37, 56, 43);
    // Gray-green for less important text (hints, subtitles).
    public static final Color MUTED = new Color(103, 125, 108);
    // Light text color used in the sidebar footer.
    public static final Color SIDEBAR_TEXT = new Color(204, 228, 211);

    // Actions
    // Normal color of the main green buttons.
    public static final Color PRIMARY = new Color(34, 125, 79);
    // Main button color when hovered.
    public static final Color PRIMARY_HOVER = new Color(27, 101, 63);
    // Main button color while being pressed.
    public static final Color PRIMARY_PRESSED = new Color(20, 83, 57);
    // Light gray-green used by secondary buttons such as "Refresh".
    public static final Color SECONDARY = new Color(224, 237, 226);
    // Secondary button color when hovered or pressed.
    public static final Color SECONDARY_HOVER = new Color(204, 224, 207);
    // Red used for dangerous actions (Reset Data) and negative numbers.
    public static final Color DANGER = new Color(185, 40, 40);
    // Darker red when the danger button is hovered or pressed.
    public static final Color DANGER_HOVER = new Color(155, 28, 28);

    // Table colors
    // Background of every second table row (zebra stripes).
    public static final Color ROW_ALT = new Color(246, 250, 247);
    // Row color while the mouse is over it.
    public static final Color ROW_HOVER = new Color(230, 242, 232);
    // Row color when it is clicked/selected.
    public static final Color ROW_SELECTED = new Color(198, 226, 203);
    // Background for TOTAL / NET INCOME style summary rows.
    public static final Color ROW_TOTAL = new Color(214, 234, 219);

    // Fonts
    // new Font(family, style, size). "SansSerif" is a safe font that exists on every system.
    // Normal body text, size 14.
    public static final Font BASE = new Font("SansSerif", Font.PLAIN, 14);
    // Bold version of body text (labels, buttons, table headers).
    public static final Font BOLD = new Font("SansSerif", Font.BOLD, 14);
    // Big bold font for page titles.
    public static final Font TITLE = new Font("SansSerif", Font.BOLD, 24);
    // Slightly smaller font for hints and sub-titles.
    public static final Font HINT = new Font("SansSerif", Font.PLAIN, 13);
}
