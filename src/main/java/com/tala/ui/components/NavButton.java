// This class lives in the reusable UI components package.
package com.tala.ui.components;

// Theme holds the app's shared colors and fonts.
import com.tala.ui.Theme;

// Swing and AWT (drawing, colors, cursor) classes.
import javax.swing.*;
import java.awt.*;

/** Sidebar menu button: highlights on hover and shows an accent bar when it is the active page. */
public class NavButton extends JButton {
    // True when this button's page is the one currently open.
    private boolean active;

    // Creates a sidebar button with the given label.
    public NavButton(String text) {
        // Let JButton store the text.
        super(text);
        // Turn off Swing's default drawing because we paint the highlight ourselves.
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        // Normal font and white text (the sidebar is dark green).
        setFont(Theme.BASE);
        setForeground(Color.WHITE);
        // Align the text to the left side of the button.
        setHorizontalAlignment(SwingConstants.LEFT);
        // Align the button to the left inside the vertical sidebar layout.
        setAlignmentX(Component.LEFT_ALIGNMENT);
        // Full available width, but at most 42 pixels tall.
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        // Padding around the text (top, left, bottom, right).
        setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 8));
        // Show a hand cursor when hovering.
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // Marks this button as the active page (or not) and refreshes how it looks.
    public void setActive(boolean active) {
        // Store the new state.
        this.active = active;
        // Bold text for the active page, normal text otherwise.
        setFont(active ? Theme.BOLD : Theme.BASE);
        // Redraw the button now.
        repaint();
    }

    // Swing calls this to draw the button; we draw the highlight and the accent bar.
    @Override
    protected void paintComponent(Graphics g) {
        // Work on a copy of the graphics so we do not disturb Swing's settings.
        Graphics2D g2 = (Graphics2D) g.create();
        // Smooth edges on rounded shapes.
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Draw a background only if the page is active or the mouse is hovering.
        if (active || getModel().isRollover()) {
            // Active color if active, lighter hover color otherwise.
            g2.setColor(active ? Theme.GREEN_ACTIVE : Theme.GREEN_HOVER);
            // Rounded background over the whole button (10px corner arcs).
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        }
        // The active button also gets a small white bar on its left edge.
        if (active) {
            g2.setColor(Color.WHITE);
            // 4px wide bar, 8px inset from top and bottom.
            g2.fillRoundRect(0, 8, 4, getHeight() - 16, 4, 4);
        }
        // Release the graphics copy.
        g2.dispose();
        // Draw the button text on top.
        super.paintComponent(g);
    }
}
