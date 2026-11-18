package tala.ui.components;

import tala.ui.theme.Theme;

import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Badge
 * ------------------------------------------------------------
 * A small rounded "pill" label, e.g.  [ Balanced ]  or  [ Asset ].
 */
public class Badge extends JLabel {

    private Color fill;

    public Badge(String text, Color fill, Color textColor) {
        super(text);
        setFont(Theme.bold(13.5f));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 11, 3, 11));
        setColors(fill, textColor);
        setOpaque(false);
    }

    public void setColors(Color fill, Color textColor) {
        this.fill = fill;
        setForeground(textColor);
        repaint();
    }

    // ----- ready-made colors -----
    public static Badge ok(String text)      { return new Badge(text, Theme.GREEN_BG, Theme.GREEN); }
    public static Badge warn(String text)    { return new Badge(text, Theme.AMBER_BG, Theme.AMBER); }
    public static Badge bad(String text)     { return new Badge(text, Theme.RED_BG, Theme.RED); }
    public static Badge info(String text)    { return new Badge(text, Theme.PURPLE_TINT, Theme.PURPLE); }

    public void showOk(String text)   { setText(text); setColors(Theme.GREEN_BG, Theme.GREEN); }
    public void showWarn(String text) { setText(text); setColors(Theme.AMBER_BG, Theme.AMBER); }
    public void showBad(String text)  { setText(text); setColors(Theme.RED_BG, Theme.RED); }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }
}
