package tala.ui.components;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Icons
 * ------------------------------------------------------------
 * Small line icons drawn with Java2D, so the project needs no image files.
 * Each icon is drawn on a 24 x 24 grid and then scaled to the size you ask for.
 *
 * Usage:  new JLabel(Icons.get(Icons.PEN, 18, Theme.PURPLE))
 *
 * (You do not need to understand every line here; it only draws pictures.)
 */
public final class Icons {

    public static final String DASHBOARD = "dashboard", PEN = "pen", BOOK = "book", LIST = "list",
            LAYERS = "layers", TABLE = "table", REFRESH = "refresh", TRASH = "trash", PLUS = "plus",
            DOWNLOAD = "download", TREND_UP = "trendUp", TREND_DOWN = "trendDown", PESO = "peso",
            FILE = "file", SLIDERS = "sliders", BULB = "bulb", CHECK = "check", CIRCLE = "circle",
            CHEVRON = "chevron", CLOSE = "close", SCALE = "scale", ARROW = "arrow";

    private Icons() { }

    public static Icon get(String name, int size, Color color) {
        return new LineIcon(name, size, color);
    }

    /** An Icon that draws itself with lines */
    private static class LineIcon implements Icon {
        private final String name;
        private final int size;
        private final Color color;

        LineIcon(String name, int size, Color color) {
            this.name = name;
            this.size = size;
            this.color = color;
        }

        @Override public int getIconWidth()  { return size; }
        @Override public int getIconHeight() { return size; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            g2.scale(size / 24.0, size / 24.0);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            draw(g2);
            g2.dispose();
        }

        private void draw(Graphics2D g) {
            switch (name) {
                case DASHBOARD:
                    g.draw(new RoundRectangle2D.Double(3, 3, 8, 8, 2, 2));
                    g.draw(new RoundRectangle2D.Double(13, 3, 8, 5, 2, 2));
                    g.draw(new RoundRectangle2D.Double(13, 10, 8, 11, 2, 2));
                    g.draw(new RoundRectangle2D.Double(3, 13, 8, 8, 2, 2));
                    break;
                case PEN:
                    g.draw(new Line2D.Double(12, 20, 21, 20));
                    g.draw(path(16.5, 3.5, 19.5, 6.5, 7, 19, 3, 20, 4, 16, 16.5, 3.5));
                    break;
                case BOOK:
                    g.draw(path(4, 19.5, 4, 5.5, 6.5, 3, 20, 3, 20, 17, 6.5, 17, 4, 19.5, 6.5, 21, 20, 21, 20, 17));
                    break;
                case LIST:
                    for (int i = 0; i < 3; i++) {
                        g.draw(new Line2D.Double(9, 6 + i * 6, 21, 6 + i * 6));
                        g.fill(new Ellipse2D.Double(3, 5 + i * 6, 2.2, 2.2));
                    }
                    break;
                case LAYERS:
                    g.draw(path(2, 7, 12, 2, 22, 7, 12, 12, 2, 7));
                    g.draw(path(2, 12, 12, 17, 22, 12));
                    g.draw(path(2, 17, 12, 22, 22, 17));
                    break;
                case TABLE:
                    g.draw(new RoundRectangle2D.Double(3, 3, 18, 18, 2, 2));
                    g.draw(new Line2D.Double(3, 9, 21, 9));
                    g.draw(new Line2D.Double(9, 3, 9, 21));
                    g.draw(new Line2D.Double(15, 3, 15, 21));
                    break;
                case REFRESH:
                    g.draw(new Arc2D.Double(3, 3, 18, 18, 40, 290, Arc2D.OPEN));
                    g.draw(path(21, 3, 21, 8, 16, 8));
                    break;
                case TRASH:
                    g.draw(new Line2D.Double(3, 6, 21, 6));
                    g.draw(path(8, 6, 8, 4, 16, 4, 16, 6));
                    g.draw(path(6, 6, 7, 21, 17, 21, 18, 6));
                    break;
                case PLUS:
                    g.draw(new Line2D.Double(12, 5, 12, 19));
                    g.draw(new Line2D.Double(5, 12, 19, 12));
                    break;
                case DOWNLOAD:
                    g.draw(new Line2D.Double(12, 4, 12, 15));
                    g.draw(path(7, 10, 12, 15, 17, 10));
                    g.draw(new Line2D.Double(5, 20, 19, 20));
                    break;
                case TREND_UP:
                    g.draw(path(3, 17, 9, 11, 13, 15, 21, 7));
                    g.draw(path(15, 7, 21, 7, 21, 13));
                    break;
                case TREND_DOWN:
                    g.draw(path(3, 7, 9, 13, 13, 9, 21, 17));
                    g.draw(path(15, 17, 21, 17, 21, 11));
                    break;
                case PESO:
                    g.draw(path(8, 21, 8, 3, 13.5, 3));
                    g.draw(new Arc2D.Double(8.5, 3, 10, 10, -90, 180, Arc2D.OPEN));
                    g.draw(new Line2D.Double(8, 13, 13.5, 13));
                    g.draw(new Line2D.Double(4, 7, 20, 7));
                    g.draw(new Line2D.Double(4, 10, 20, 10));
                    break;
                case FILE:
                    g.draw(path(6, 2, 15, 2, 20, 7, 20, 22, 6, 22, 6, 2));
                    g.draw(path(14, 2, 14, 8, 20, 8));
                    g.draw(new Line2D.Double(9, 13, 15, 13));
                    g.draw(new Line2D.Double(9, 17, 15, 17));
                    break;
                case SLIDERS:
                    g.draw(new Line2D.Double(4, 21, 4, 14)); g.draw(new Line2D.Double(4, 10, 4, 3));
                    g.draw(new Line2D.Double(12, 21, 12, 12)); g.draw(new Line2D.Double(12, 8, 12, 3));
                    g.draw(new Line2D.Double(20, 21, 20, 16)); g.draw(new Line2D.Double(20, 12, 20, 3));
                    g.draw(new Line2D.Double(1, 14, 7, 14)); g.draw(new Line2D.Double(9, 8, 15, 8));
                    g.draw(new Line2D.Double(17, 16, 23, 16));
                    break;
                case BULB:
                    g.draw(new Line2D.Double(9, 18, 15, 18));
                    g.draw(new Line2D.Double(10, 22, 14, 22));
                    g.draw(new Arc2D.Double(5, 2, 14, 14, -40, 260, Arc2D.OPEN));
                    g.draw(path(8, 13.5, 8, 15.5, 16, 15.5, 16, 13.5));
                    break;
                case CHECK:
                    g.draw(new Ellipse2D.Double(3, 3, 18, 18));
                    g.draw(path(8, 12, 11, 15, 16, 9));
                    break;
                case CIRCLE:
                    g.draw(new Ellipse2D.Double(3, 3, 18, 18));
                    break;
                case CHEVRON:
                    g.draw(path(9, 6, 15, 12, 9, 18));
                    break;
                case CLOSE:
                    g.draw(new Line2D.Double(6, 6, 18, 18));
                    g.draw(new Line2D.Double(18, 6, 6, 18));
                    break;
                case SCALE:
                    g.draw(new Line2D.Double(12, 3, 12, 21));
                    g.draw(new Line2D.Double(5, 21, 19, 21));
                    g.draw(new Line2D.Double(3, 7, 21, 7));
                    g.draw(path(6, 7, 3, 14, 9, 14, 6, 7));
                    g.draw(path(18, 7, 15, 14, 21, 14, 18, 7));
                    break;
                case ARROW:
                    g.draw(new Line2D.Double(4, 12, 20, 12));
                    g.draw(path(14, 6, 20, 12, 14, 18));
                    break;
                default:
                    g.draw(new Ellipse2D.Double(4, 4, 16, 16));
            }
        }

        /** Connects the points (x1,y1) (x2,y2) ... with straight lines */
        private Path2D path(double... xy) {
            Path2D p = new Path2D.Double();
            p.moveTo(xy[0], xy[1]);
            for (int i = 2; i < xy.length; i += 2) {
                p.lineTo(xy[i], xy[i + 1]);
            }
            return p;
        }
    }
}
