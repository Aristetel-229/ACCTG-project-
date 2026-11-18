package tala.ui.components;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

/**
 * IconBox
 * ------------------------------------------------------------
 * A colored rounded square with a line icon in the middle
 * (the little squares next to card titles and on the stat tiles).
 */
public class IconBox extends JComponent {

    private final int boxSize;
    private final Color background;
    private final Icon icon;

    public IconBox(String iconName, int boxSize, Color background, Color iconColor) {
        this.boxSize = boxSize;
        this.background = background;
        this.icon = Icons.get(iconName, boxSize <= 40 ? 18 : 26, iconColor);
        Dimension size = new Dimension(boxSize, boxSize);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(background);
        int arc = boxSize <= 40 ? 24 : 30;
        g2.fillRoundRect(0, 0, boxSize, boxSize, arc, arc);
        icon.paintIcon(this, g2, (boxSize - icon.getIconWidth()) / 2, (boxSize - icon.getIconHeight()) / 2);
        g2.dispose();
    }
}
