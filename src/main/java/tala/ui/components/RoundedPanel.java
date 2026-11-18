package tala.ui.components;

import tala.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * RoundedPanel
 * ------------------------------------------------------------
 * A JPanel with rounded corners, a thin border, a soft shadow
 * and (optionally) a gradient background.
 * Every "card" in the app is one of these.
 *
 * setHoverable(color) makes it react to the mouse: when you point at it,
 * it lifts up a little, its shadow grows and its border takes that color.
 */
public class RoundedPanel extends JPanel {

    private static final int LIFT = 3;          // how many pixels a hovered card rises

    private int radius = 18;
    private Color fill = Theme.CARD_BG;
    private Color gradientEnd = null;          // null = plain color
    private float gradientStart = 0f;          // 0..1, where the gradient begins (left to right)
    private Color borderColor = Theme.CARD_BORDER;
    private boolean shadow = true;

    private boolean hoverable = false;
    private boolean hovered = false;
    private Color hoverBorder = Theme.PURPLE_BAR;

    public RoundedPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);   // we paint the background ourselves
        // keep the content inside the border and above the shadow
        setBorder(BorderFactory.createEmptyBorder(1, 1, 3, 1));
    }

    public RoundedPanel setRadius(int radius)       { this.radius = radius; repaint(); return this; }
    public RoundedPanel setFill(Color fill)         { this.fill = fill; repaint(); return this; }
    public RoundedPanel setBorderColor(Color color)  { this.borderColor = color; repaint(); return this; }
    public RoundedPanel setShadow(boolean shadow)   { this.shadow = shadow; repaint(); return this; }

    /** Gradient from fill (left) to end (right), starting at 'startFraction' of the width */
    public RoundedPanel setGradient(Color end, float startFraction) {
        this.gradientEnd = end;
        this.gradientStart = startFraction;
        repaint();
        return this;
    }

    /**
     * Turns on the hover effect. borderWhenHovered is the border color
     * while the mouse is over the panel.
     */
    public RoundedPanel setHoverable(Color borderWhenHovered) {
        this.hoverBorder = borderWhenHovered;
        if (!hoverable) {
            hoverable = true;
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    // moving onto a child (a label inside the card) also counts as "exited",
                    // so only stop hovering when the mouse really left the card
                    if (getMousePosition(true) == null) {
                        hovered = false;
                        repaint();
                    }
                }
            });
        }
        return this;
    }

    // ---------------------------------------------------------------- painting

    /** Space kept free at the top (to lift into) and at the bottom (for the shadow) */
    private int topSpace()    { return hoverable ? LIFT : 0; }
    private int bottomSpace() { return hoverable ? 7 : 3; }

    /**
     * paint() draws the whole panel: background + children.
     * While hovered we draw a big shadow first, then move EVERYTHING up by LIFT pixels.
     */
    @Override
    public void paint(Graphics g) {
        if (hoverable && hovered) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 1, arc = radius * 2;
            int bodyHeight = getHeight() - topSpace() - bottomSpace();
            // a soft, wide shadow: several see-through layers, each a bit lower
            for (int i = 1; i <= 6; i++) {
                g2.setColor(new Color(46, 16, 101, 9));
                g2.fillRoundRect(2, i, w - 4, bodyHeight + 1, arc, arc);
            }
            g2.dispose();

            Graphics2D lifted = (Graphics2D) g.create();
            lifted.translate(0, -LIFT);
            super.paint(lifted);
            lifted.dispose();
        } else {
            super.paint(g);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int x = 0, y = topSpace();
        int w = getWidth() - 1;
        int h = getHeight() - topSpace() - bottomSpace();
        int arc = radius * 2;

        // 1) soft shadow under the card (the hover shadow is drawn in paint())
        if (shadow && !hovered) {
            g2.setColor(new Color(46, 16, 101, 12));
            g2.fillRoundRect(x + 1, y + 3, w - 1, h, arc, arc);
            g2.setColor(new Color(46, 16, 101, 18));
            g2.fillRoundRect(x, y + 1, w, h + 1, arc, arc);
        }

        // 2) background: plain color or gradient
        if (gradientEnd != null) {
            float startX = getWidth() * gradientStart;
            g2.setPaint(new GradientPaint(startX, 0, fill, getWidth(), getHeight(), gradientEnd));
        } else {
            g2.setColor(fill);
        }
        g2.fillRoundRect(x, y, w, h, arc, arc);

        // 3) thin border (thicker and colored while hovered)
        if (hovered) {
            g2.setColor(hoverBorder);
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawRoundRect(x + 1, y + 1, w - 2, h - 2, arc, arc);
        } else if (borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRoundRect(x, y, w, h, arc, arc);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
