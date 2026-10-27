// Package for reusable UI building blocks.
package com.tala.ui.components;

import com.tala.ui.Theme;

import javax.swing.*;
import java.awt.*;

/** Rounded button with hover and pressed colors and a hand cursor. */
public class RoundedButton extends JButton {
    // The three button styles. An enum is a fixed list of allowed values.
    //   PRIMARY = main green action, SECONDARY = light gray-green (Refresh), DANGER = red (Reset Data)
    public enum Kind { PRIMARY, SECONDARY, DANGER }

    // Which style this button uses; set once at creation.
    private final Kind kind;

    // Shortcut constructor: a button without a style argument is a PRIMARY button.
    public RoundedButton(String text) {
        this(text, Kind.PRIMARY);
    }

    // Main constructor: button text + style.
    public RoundedButton(String text, Kind kind) {
        super(text);
        this.kind = kind;
        // Turn off the default Swing button look (we paint the background ourselves).
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setFont(Theme.BOLD);
        // Secondary buttons have a light background so they need dark text; the others use white text.
        setForeground(kind == Kind.SECONDARY ? Theme.TEXT : Color.WHITE);
        // Padding: 9px top/bottom, 18px left/right.
        setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // Swing calls this to draw the button.
    @Override
    protected void paintComponent(Graphics g) {
        // Copy of the graphics context so our settings stay local.
        Graphics2D g2 = (Graphics2D) g.create();
        // Smooth edges.
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Pick the color for the current state (normal / hover / pressed / disabled).
        g2.setColor(currentColor());
        // Draw the rounded background (corner size 12) over the full button area.
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g2.dispose();
        // Draw the text on top.
        super.paintComponent(g);
    }

    // Decides the background color based on the button's style and state.
    private Color currentColor() {
        // ButtonModel stores the button's live state (is the mouse over it? is it pressed?).
        ButtonModel m = getModel();
        // A disabled button is drawn in the faint border color.
        if (!isEnabled()) return Theme.BORDER;
        // true when the mouse pointer is over the button.
        boolean hover = m.isRollover();
        // true while the mouse button is held down on it.
        boolean pressed = m.isPressed();
        // A "switch expression": picks a result depending on the button style.
        return switch (kind) {
            // PRIMARY has 3 shades: pressed (darkest), hover, normal.
            case PRIMARY -> pressed ? Theme.PRIMARY_PRESSED : hover ? Theme.PRIMARY_HOVER : Theme.PRIMARY;
            // DANGER: dark red when hovered or pressed, otherwise normal red.
            case DANGER -> pressed || hover ? Theme.DANGER_HOVER : Theme.DANGER;
            // SECONDARY: slightly darker when hovered or pressed, otherwise normal.
            case SECONDARY -> pressed || hover ? Theme.SECONDARY_HOVER : Theme.SECONDARY;
        };
    }
}
