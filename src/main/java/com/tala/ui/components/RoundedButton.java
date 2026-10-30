// This class lives in the reusable UI components package.
package com.tala.ui.components;

// Theme holds the app's shared colors and fonts.
import com.tala.ui.Theme;

// Swing and AWT (drawing, colors, cursor) classes.
import javax.swing.*;
import java.awt.*;

/** Rounded button with hover and pressed colors and a hand cursor. */
public class RoundedButton extends JButton {
    // The three button styles: main action, neutral action, and delete/danger action.
    public enum Kind { PRIMARY, SECONDARY, DANGER }

    // Which style this button uses (decides its colors).
    private final Kind kind;

    // Short constructor: a normal (PRIMARY) button with the given text.
    public RoundedButton(String text) {
        // Call the other constructor using PRIMARY.
        this(text, Kind.PRIMARY);
    }

    // Main constructor: button text plus style.
    public RoundedButton(String text, Kind kind) {
        // Let JButton store the text.
        super(text);
        // Remember the style.
        this.kind = kind;
        // Turn off Swing's default drawing (fill, border, focus box) because we paint the button ourselves.
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        // Bold text.
        setFont(Theme.BOLD);
        // Dark text on the light secondary button, white text on the others.
        setForeground(kind == Kind.SECONDARY ? Theme.TEXT : Color.WHITE);
        // Padding around the text (top, left, bottom, right).
        setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        // Show a hand cursor when the mouse is over the button.
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // Swing calls this to draw the button; we draw our own rounded background.
    @Override
    protected void paintComponent(Graphics g) {
        // Work on a copy of the graphics so we do not disturb Swing's settings.
        Graphics2D g2 = (Graphics2D) g.create();
        // Smooth edges on the rounded rectangle.
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Pick the color for the current state (normal, hover, pressed, disabled).
        g2.setColor(currentColor());
        // Draw the filled rounded rectangle with 12px corner arcs across the whole button.
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        // Release the graphics copy.
        g2.dispose();
        // Draw the text on top of our background.
        super.paintComponent(g);
    }

    // Chooses the background color depending on state and style.
    private Color currentColor() {
        // The model knows if the mouse is over or pressing the button.
        ButtonModel m = getModel();
        // Disabled buttons look gray.
        if (!isEnabled()) return Theme.BORDER;
        // Is the mouse hovering over the button?
        boolean hover = m.isRollover();
        // Is the button being pressed?
        boolean pressed = m.isPressed();
        // Select the color by style: pressed/hover get a different shade.
        return switch (kind) {
            case PRIMARY -> pressed ? Theme.PRIMARY_PRESSED : hover ? Theme.PRIMARY_HOVER : Theme.PRIMARY;
            case DANGER -> pressed || hover ? Theme.DANGER_HOVER : Theme.DANGER;
            case SECONDARY -> pressed || hover ? Theme.SECONDARY_HOVER : Theme.SECONDARY;
        };
    }
}
