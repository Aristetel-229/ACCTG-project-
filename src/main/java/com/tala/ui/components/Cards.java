// This class lives in the reusable UI components package.
package com.tala.ui.components;

// Theme holds the app's shared colors and fonts.
import com.tala.ui.Theme;

// Swing, border and AWT (colors, fonts, layouts) classes.
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Dashboard metric card with a colored accent bar. */
public final class Cards {
    // Private constructor: only static methods are used.
    private Cards() {}

    // Builds one dashboard card: a title, a big value, a small hint, and a colored bar on the left.
    public static JPanel metric(String title, String hint, JLabel value, Color accent) {
        // The card container; BorderLayout lets us place the bar (west) and text (center).
        JPanel card = new JPanel(new BorderLayout());
        // White background.
        card.setBackground(Color.WHITE);
        // Thin 1px outline using the theme border color.
        card.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));

        // The thin colored accent bar on the left side of the card.
        JPanel bar = new JPanel();
        // Color it with the accent color given by the caller.
        bar.setBackground(accent);
        // 6 pixels wide; height 0 means "let the layout decide".
        bar.setPreferredSize(new Dimension(6, 0));

        // Title label (e.g. "Total Assets") with theme text color and bold font.
        JLabel t = new JLabel(title);
        t.setForeground(Theme.TEXT);
        t.setFont(Theme.BOLD);
        // The value label is created by the caller (so it can be updated later); here we make it large and accent-colored.
        value.setFont(new Font("SansSerif", Font.BOLD, 26));
        value.setForeground(accent);
        // Small muted hint text under the value.
        JLabel h = new JLabel(hint);
        h.setForeground(Theme.MUTED);
        h.setFont(Theme.HINT);

        // Panel that stacks title, value and hint vertically.
        JPanel text = new JPanel();
        // Transparent so the card's white shows through.
        text.setOpaque(false);
        // BoxLayout on the Y axis = vertical stack.
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        // Padding inside the card (top, left, bottom, right).
        text.setBorder(new EmptyBorder(14, 18, 14, 18));
        // Align all three labels to the left edge.
        for (JComponent c : new JComponent[]{t, value, h}) c.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Add title, then a gap, value, gap, and hint.
        text.add(t);
        text.add(Box.createVerticalStrut(6));
        text.add(value);
        text.add(Box.createVerticalStrut(4));
        text.add(h);

        // Put the bar on the left and the text in the remaining space.
        card.add(bar, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        // Return the finished card.
        return card;
    }
}
