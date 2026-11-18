package tala.ui.components;

import tala.ui.theme.Theme;

import javax.swing.Icon;
import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * AppButton
 * ------------------------------------------------------------
 * A rounded button in one of three styles:
 *   PRIMARY   - purple, for the main action of a screen
 *   SECONDARY - white with a purple label
 *   DANGER    - white with a red label (delete / reset)
 */
public class AppButton extends JButton {

    public enum Style { PRIMARY, SECONDARY, DANGER }

    private final Style style;

    public AppButton(String text, Style style) {
        this(text, null, style);
    }

    public AppButton(String text, Icon icon, Style style) {
        super(text, icon);
        this.style = style;
        setFont(Theme.bold(15.5f));
        setIconTextGap(8);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setMargin(new Insets(0, 18, 0, 18));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setForeground(style == Style.PRIMARY ? Color.WHITE : style == Style.DANGER ? Theme.RED : Theme.PURPLE_DEEP);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(size.width + 22, Math.max(44, size.height));   // at least 44px tall (easy to click)
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean hover = getModel().isRollover() || getModel().isPressed();
        int w = getWidth() - 1, h = getHeight() - 1, arc = 22;

        Color fill, border;
        switch (style) {
            case PRIMARY:
                fill = hover ? Theme.PURPLE_DARK : Theme.PURPLE;
                border = fill;
                break;
            case DANGER:
                fill = hover ? Theme.RED_BG : Color.WHITE;
                border = Theme.RED_BORDER;
                break;
            default:
                fill = hover ? new Color(0xf5f1fc) : Color.WHITE;
                border = Theme.FIELD_BORDER;
        }
        if (!isEnabled()) {
            fill = new Color(0xf1eef6);
            border = Theme.FIELD_BORDER;
        }

        g2.setColor(fill);
        g2.fillRoundRect(0, 0, w, h, arc, arc);
        g2.setColor(border);
        g2.drawRoundRect(0, 0, w, h, arc, arc);

        // keyboard focus ring
        if (isFocusOwner()) {
            g2.setColor(Theme.FOCUS);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, w - 2, h - 2, arc, arc);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
