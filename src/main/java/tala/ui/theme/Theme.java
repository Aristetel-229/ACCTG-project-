package tala.ui.theme;

import tala.util.Money;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Arrays;
import java.util.List;

/**
 * Theme
 * ------------------------------------------------------------
 * All colors and fonts of the app in one place (purple + yellow).
 * Want a different look? Change the values here only.
 */
public final class Theme {

    private Theme() { }

    // ---------------- Purple & yellow brand colors ----------------
    public static final Color NAV_BG        = new Color(0x3b1a6e);  // top bar
    public static final Color NAV_TEXT      = new Color(0xe4dcfa);
    public static final Color NAV_MUTED     = new Color(0xc9bdf0);
    public static final Color YELLOW        = new Color(0xfacc15);  // active tab, icon boxes
    public static final Color YELLOW_SOFT   = new Color(0xfde047);
    public static final Color PURPLE        = new Color(0x5b21b6);  // primary buttons, links
    public static final Color PURPLE_DARK   = new Color(0x4c1d95);
    public static final Color PURPLE_DEEP   = new Color(0x3b1a6e);
    public static final Color PURPLE_BRIGHT = new Color(0x6d28d9);
    public static final Color PURPLE_TINT   = new Color(0xf3eefc);  // table header, chips
    public static final Color PURPLE_HOVER  = new Color(0xf7f4fd);
    public static final Color FOCUS         = new Color(0xa78bfa);

    // ---------------- Surfaces ----------------
    public static final Color PAGE_BG       = new Color(0xebeaf0);  // light grey, so white cards stand out
    public static final Color CARD_BG       = Color.WHITE;
    public static final Color CARD_BORDER   = new Color(0xe2ddee);
    public static final Color READ_ONLY_BG  = new Color(0xf4f3f8);  // fields you cannot type in
    public static final Color DIVIDER       = new Color(0xf0ebf7);
    public static final Color ROW_LINE      = new Color(0xf3f0f8);
    public static final Color FIELD_BORDER  = new Color(0xddd5ec);
    public static final Color FOOTER_BG     = new Color(0xfcfbfe);
    public static final Color CREAM         = new Color(0xfef9e3);  // total rows, tip card
    public static final Color CREAM_BORDER  = new Color(0xf3e3a3);
    public static final Color CREAM_LINE    = new Color(0xead98c);
    public static final Color HEADER_GLOW   = new Color(0xfff6d1);  // page header gradient end

    // ---------------- Text ----------------
    public static final Color TITLE         = new Color(0x2e1065);
    public static final Color TEXT          = new Color(0x2a2140);
    public static final Color TEXT_SOFT     = new Color(0x5a5070);
    public static final Color MUTED         = new Color(0x7c6f99);
    public static final Color TABLE_HEAD    = new Color(0x4c1d95);

    // ---------------- Status colors (background, text) ----------------
    public static final Color GREEN_BG = new Color(0xe6f6ec), GREEN = new Color(0x17663d);
    public static final Color AMBER_BG = new Color(0xfef3c7), AMBER = new Color(0x7a4f01);
    public static final Color RED_BG   = new Color(0xfdeeec), RED   = new Color(0xb42318);
    public static final Color BLUE_BG  = new Color(0xe8f0fd), BLUE  = new Color(0x1e4fc2);
    public static final Color RED_BORDER = new Color(0xf3c9c4);
    public static final Color GREEN_BAR  = new Color(0x22c55e);
    public static final Color PURPLE_BAR = new Color(0x7c3aed);

    // ---------------- Font ----------------
    /** Times New Roman if the computer has it, otherwise the closest serif font */
    public static final String FONT_FAMILY = pickFontFamily();

    private static String pickFontFamily() {
        List<String> installed = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String wanted : new String[] {"Times New Roman", "Liberation Serif", "DejaVu Serif"}) {
            if (installed.contains(wanted)) {
                return wanted;
            }
        }
        return Font.SERIF;
    }

    public static Font font(int style, float size) {
        return new Font(FONT_FAMILY, style, 1).deriveFont(style, size);
    }

    public static Font regular(float size) { return font(Font.PLAIN, size); }
    public static Font bold(float size)    { return font(Font.BOLD, size); }

    /** Call once at start-up: uses "PHP " if the font has no peso sign. */
    public static void checkPesoSign() {
        if (!regular(15).canDisplay('\u20B1')) {
            Money.PESO_SIGN = "PHP ";
        }
    }

    // ---------------- Form field styling ----------------

    /** Rounded border with inner padding for text fields */
    public static Border fieldBorder(Color color) {
        return BorderFactory.createCompoundBorder(
                new RoundedLineBorder(color, 11, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12));
    }

    /** Gives a text field the TALA look (and a purple border while typing) */
    public static void styleField(JTextField field) {
        field.setFont(regular(16));
        field.setForeground(TEXT);
        field.setBackground(Color.WHITE);
        field.setCaretColor(PURPLE);
        field.setBorder(fieldBorder(FIELD_BORDER));
        field.setPreferredSize(new Dimension(160, 46));
        addFocusColor(field);
    }

    public static void styleCombo(JComboBox<?> combo) {
        combo.setFont(regular(16));
        combo.setForeground(TEXT);
        combo.setBackground(Color.WHITE);
        combo.setPreferredSize(new Dimension(160, 46));
    }

    private static void addFocusColor(final JComponent field) {
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { field.setBorder(fieldBorder(PURPLE_BAR)); }
            @Override public void focusLost(FocusEvent e)   { field.setBorder(fieldBorder(FIELD_BORDER)); }
        });
    }
}
