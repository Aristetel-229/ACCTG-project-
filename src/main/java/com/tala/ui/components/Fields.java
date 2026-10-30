// This class lives in the reusable UI components package.
package com.tala.ui.components;

// Theme holds the app's shared colors and fonts.
import com.tala.ui.Theme;

// Swing, border and focus-event classes.
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/** Text fields that highlight their border when focused. */
public final class Fields {
    // Private constructor: only static methods are used.
    private Fields() {}

    // Creates an empty styled text field.
    public static JTextField text() {
        // Reuse the other version with an empty starting text.
        return text("");
    }

    // Creates a styled text field with some starting text.
    public static JTextField text(String initial) {
        // Create the Swing text field.
        JTextField field = new JTextField(initial);
        // Use the app's normal font.
        field.setFont(Theme.BASE);
        // Start with the normal (gray) border.
        field.setBorder(border(Theme.BORDER));
        // Listen for focus changes to change the border color.
        field.addFocusListener(new FocusAdapter() {
            // When the user clicks into the field, show the highlighted (primary color) border.
            @Override public void focusGained(FocusEvent e) { field.setBorder(border(Theme.PRIMARY)); }
            // When the user leaves the field, go back to the normal border.
            @Override public void focusLost(FocusEvent e) { field.setBorder(border(Theme.BORDER)); }
        });
        // Return the field.
        return field;
    }

    // Builds a border: a 1px colored outline plus empty padding inside.
    private static Border border(java.awt.Color color) {
        // Compound border = outer line + inner padding (top, left, bottom, right).
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8));
    }
}
