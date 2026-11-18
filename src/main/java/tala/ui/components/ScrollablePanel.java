package tala.ui.components;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;

/**
 * ScrollablePanel
 * ------------------------------------------------------------
 * A JPanel for inside a JScrollPane that always fits the window's
 * WIDTH (so it never scrolls sideways) and only scrolls up and down.
 */
public class ScrollablePanel extends JPanel implements Scrollable {

    public ScrollablePanel(LayoutManager layout) {
        super(layout);
    }

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return 24; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return r.height - 48; }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        // stretch to the window height when the content is shorter than the window
        return getParent() != null && getPreferredSize().height < getParent().getHeight();
    }
}
