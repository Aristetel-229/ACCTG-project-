// Package for reusable UI building blocks.
package com.tala.ui.components;

// Our shared colors and fonts.
import com.tala.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Dashboard metric card with a colored accent bar. */
public final class Cards {
    // Static-only class: never instantiated.
    private Cards() {}

    // Builds one dashboard card (e.g. "TOTAL ASSETS").
    //   title  = small bold heading
    //   hint   = small gray explanation under the number
    //   value  = the label that shows the big number (passed in so the page can update its text)
    //   accent = the color of the left bar and of the big number
    public static JPanel metric(String title, String hint, JLabel value, Color accent) {
        // The card itself: white background with a thin border.
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));

        // The thin colored vertical bar on the left side of the card (6px wide).
        JPanel bar = new JPanel();
        bar.setBackground(accent);
        bar.setPreferredSize(new Dimension(6, 0));

        // The card title (bold, dark text).
        JLabel t = new JLabel(title);
        t.setForeground(Theme.TEXT);
        t.setFont(Theme.BOLD);
        // The big number: size 26 bold, in the accent color.
        value.setFont(new Font("SansSerif", Font.BOLD, 26));
        value.setForeground(accent);
        // The small hint text (gray).
        JLabel h = new JLabel(hint);
        h.setForeground(Theme.MUTED);
        h.setFont(Theme.HINT);

        // A panel that stacks title, number and hint vertically.
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        // Inner padding: 14px top/bottom, 18px left/right.
        text.setBorder(new EmptyBorder(14, 18, 14, 18));
        // Left-align all three labels (BoxLayout would center them otherwise).
        for (JComponent c : new JComponent[]{t, value, h}) c.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Add them in order, with small gaps (6px and 4px) between.
        text.add(t);
        text.add(Box.createVerticalStrut(6));
        text.add(value);
        text.add(Box.createVerticalStrut(4));
        text.add(h);

        // Assemble the card: accent bar on the left, text on the right.
        card.add(bar, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
    }
}
