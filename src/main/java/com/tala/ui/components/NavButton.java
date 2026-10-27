// Package for reusable UI building blocks.
package com.tala.ui.components;

import com.tala.ui.Theme;

import javax.swing.*;
import java.awt.*;

/** Sidebar menu button: highlights on hover and shows an accent bar when it is the active page. */
// Extends JButton and customizes how it is drawn.
public class NavButton extends JButton {
    // true when this button's page is the one currently displayed.
    private boolean active;

    public NavButton(String text) {
        // Call JButton's constructor with the button text.
        super(text);
        // Turn off Swing's default button look so we can paint our own background:
        // no filled background, no border, no dotted focus rectangle, not opaque.
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setFont(Theme.BASE);
        // White text on the dark green sidebar.
        setForeground(Color.WHITE);
        // Text is aligned to the left inside the button.
        setHorizontalAlignment(SwingConstants.LEFT);
        // Align the button itself to the left edge in the sidebar's vertical layout.
        setAlignmentX(Component.LEFT_ALIGNMENT);
        // Button stretches to the full sidebar width but is at most 42px tall.
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        // Padding inside the button: 10px top/bottom, 16px left, 8px right.
        setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 8));
        // Show a hand cursor when hovering.
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // Called by MainFrame to mark this button as the current page (bold text) or not.
    public void setActive(boolean active) {
        this.active = active;
        // Active page = bold font, others = normal font.
        setFont(active ? Theme.BOLD : Theme.BASE);
        // Redraw the button so the new look appears immediately.
        repaint();
    }

    // Swing calls this whenever the button must be drawn. We draw our own background first, then let the normal drawing add the text.
    @Override
    protected void paintComponent(Graphics g) {
        // Make a copy of the graphics object so our changes don't affect anything else.
        Graphics2D g2 = (Graphics2D) g.create();
        // Smooth (anti-aliased) edges for the rounded shapes.
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Draw the rounded background only if this is the active page or the mouse is over it (rollover).
        if (active || getModel().isRollover()) {
            // Active = brighter green; hover = medium green.
            g2.setColor(active ? Theme.GREEN_ACTIVE : Theme.GREEN_HOVER);
            // Rounded rectangle covering the whole button (corner size 10).
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        }
        // For the active page only: a small white vertical accent bar on the left edge.
        if (active) {
            g2.setColor(Color.WHITE);
            // x=0, y=8, width=4, height = button height minus 16 (8px margin on top and bottom).
            g2.fillRoundRect(0, 8, 4, getHeight() - 16, 4, 4);
        }
        // Free the graphics copy.
        g2.dispose();
        // Let JButton draw the text on top of our background.
        super.paintComponent(g);
    }
}
