package tala.ui.components;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import java.awt.Dimension;
import java.awt.Insets;

/**
 * WrapLabel
 * ------------------------------------------------------------
 * A JLabel whose text wraps onto more lines when the window is narrow,
 * and which grows taller to show every line.
 * (A normal JLabel with <html> wraps, but keeps its one-line height
 * and cuts the extra lines off.)
 */
public class WrapLabel extends JLabel {

    private int lastWidth = -1;

    public WrapLabel(String text) {
        setText(text);
    }

    @Override
    public void setText(String text) {
        super.setText(text == null ? "" : "<html>" + text + "</html>");
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension normal = super.getPreferredSize();
        View view = (View) getClientProperty(BasicHTML.propertyKey);
        int width = getWidth();
        if (view == null || width <= 0) {
            return new Dimension(Math.min(normal.width, 300), normal.height);
        }
        // How tall is the text when it has to fit in our current width?
        Insets insets = getInsets();
        view.setSize(width - insets.left - insets.right, 0);
        int height = (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS)) + insets.top + insets.bottom;
        return new Dimension(Math.min(normal.width, 300), height);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(10, getPreferredSize().height);
    }

    /** When our width changes, ask the layout to measure us again */
    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (width != lastWidth) {
            lastWidth = width;
            SwingUtilities.invokeLater(this::revalidate);
        }
    }
}
