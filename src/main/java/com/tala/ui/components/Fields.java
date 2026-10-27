// Package for reusable UI building blocks.
package com.tala.ui.components;

import com.tala.ui.Theme;

import javax.swing.*;
import javax.swing.border.Border;
// FocusAdapter/FocusEvent let us react when a field gains or loses the cursor.
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/** Text fields that highlight their border when focused. */
public final class Fields {
    // Static-only class: never instantiated.
    private Fields() {}

    // Creates an empty styled text field (calls the version below with "").
    public static JTextField text() {
        return text("");
    }

    // Creates a styled text field that starts with the given text (e.g. today's date).
    public static JTextField text(String initial) {
        JTextField field = new JTextField(initial);
        field.setFont(Theme.BASE);
        // Default look: light gray-green border.
        field.setBorder(border(Theme.BORDER));
        // Change the border color when the user clicks in (focus gained) and back when they leave (focus lost).
        // The anonymous class overrides only the two methods we care about.
        field.addFocusListener(new FocusAdapter() {
            // Cursor is in the field -> highlight with the green PRIMARY color.
            @Override public void focusGained(FocusEvent e) { field.setBorder(border(Theme.PRIMARY)); }
            // Cursor left the field -> back to the normal border color.
            @Override public void focusLost(FocusEvent e) { field.setBorder(border(Theme.BORDER)); }
        });
        return field;
    }

    // Builds a border: a 1px colored line on the outside plus 6px/8px empty padding inside (so text isn't glued to the edge).
    // Fully qualified name java.awt.Color is used here instead of an import.
    private static Border border(java.awt.Color color) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8));
    }
}
