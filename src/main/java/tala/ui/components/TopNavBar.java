package tala.ui.components;

import tala.ui.theme.AppImages;
import tala.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TopNavBar
 * ------------------------------------------------------------
 * The purple bar at the top:
 *   [logo] TALA          Dashboard | Journal Entry | ...        o MySQL connected
 *
 * The active page's tab is yellow.
 */
public class TopNavBar extends JPanel {

    private final Map<String, NavTab> tabs = new LinkedHashMap<>();
    private final JPanel tabRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 11));
    private final JLabel statusLabel = new JLabel();
    private final StatusDot statusDot = new StatusDot();

    public TopNavBar() {
        super(new BorderLayout(20, 0));
        setBackground(Theme.NAV_BG);
        setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));
        setPreferredSize(new Dimension(100, 66));

        add(createBrand(), BorderLayout.WEST);

        tabRow.setOpaque(false);
        add(tabRow, BorderLayout.CENTER);

        add(createStatus(), BorderLayout.EAST);
    }

    /** Adds one tab. When clicked, it runs the given action. */
    public void addTab(String pageName, String iconName, ActionListener onClick) {
        NavTab tab = new NavTab(pageName, iconName);
        tab.addActionListener(onClick);
        tabs.put(pageName, tab);
        tabRow.add(tab);
    }

    /** Paints the tab of the page we are on in yellow */
    public void setActive(String pageName) {
        for (Map.Entry<String, NavTab> entry : tabs.entrySet()) {
            entry.getValue().setActive(entry.getKey().equals(pageName));
        }
    }

    /** Green dot "MySQL connected" or red dot "MySQL offline" */
    public void setConnected(boolean connected) {
        statusLabel.setText(connected ? "MySQL connected" : "MySQL offline");
        statusDot.color = connected ? new Color(0x4ade80) : new Color(0xf87171);
        statusDot.repaint();
    }

    // ------------------------------------------------------------ parts

    private JComponent createBrand() {
        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));

        brand.add(new Logo());
        brand.add(Box.createHorizontalStrut(10));

        JLabel name = new JLabel("TALA");
        name.setFont(Theme.bold(20));
        name.setForeground(Color.WHITE);
        JLabel edition = new JLabel("Student Edition");
        edition.setFont(Theme.regular(12.5f));
        edition.setForeground(Theme.NAV_MUTED);

        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        words.add(name);
        words.add(edition);
        brand.add(words);

        JPanel holder = new JPanel(new GridBagLayout());   // centers vertically
        holder.setOpaque(false);
        holder.add(brand);
        return holder;
    }

    private JComponent createStatus() {
        JPanel pill = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 6)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 20));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
            }
        };
        pill.setOpaque(false);
        pill.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 8));
        statusLabel.setFont(Theme.regular(13.5f));
        statusLabel.setForeground(new Color(0xd9ccf7));
        pill.add(statusDot);
        pill.add(statusLabel);
        setConnected(true);

        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        holder.add(pill);
        return holder;
    }

    // ------------------------------------------------------------ small inner components

    /** One tab button. Yellow with purple text when active. */
    private static class NavTab extends JButton {
        private boolean active;
        private final String iconName;

        NavTab(String text, String iconName) {
            super(text);
            this.iconName = iconName;
            setFont(Theme.regular(15));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setIconTextGap(7);
            setMargin(new java.awt.Insets(0, 0, 0, 0));
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setActive(false);
        }

        /** Width measured with the BOLD font, so tabs don't jump when one becomes active */
        @Override
        public Dimension getPreferredSize() {
            int textWidth = getFontMetrics(Theme.bold(15)).stringWidth(getText());
            return new Dimension(textWidth + 16 + 7 + 24 + 4, 44);   // text + icon + gap + padding
        }

        void setActive(boolean active) {
            this.active = active;
            Color color = active ? Theme.PURPLE_DEEP : Theme.NAV_TEXT;
            setForeground(color);
            setFont(active ? Theme.bold(15) : Theme.regular(15));
            setIcon(Icons.get(iconName, 16, color));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active) {
                g2.setColor(Theme.YELLOW);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            } else if (getModel().isRollover()) {
                g2.setColor(new Color(255, 255, 255, 26));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            }
            if (isFocusOwner() && !active) {   // keyboard focus ring (Tab key)
                g2.setColor(Theme.FOCUS);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 20, 20);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Small round status dot */
    private static class StatusDot extends JComponent {
        Color color = new Color(0x4ade80);

        StatusDot() {
            setPreferredSize(new Dimension(8, 8));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(0, 0, 8, 8);
            g2.dispose();
        }
    }

    /**
     * The TALA logo (src/main/resources/images/logo.png) in a rounded square.
     * If the picture is missing, a yellow "T" badge is drawn instead.
     */
    private static class Logo extends JComponent {
        // twice the size we show it at, so it stays sharp on high-resolution screens
        private final BufferedImage picture = AppImages.getLogo(80);

        Logo() {
            setPreferredSize(new Dimension(40, 40));
            setMaximumSize(new Dimension(40, 40));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            if (picture != null) {
                // fill a rounded square with the picture (smooth, rounded corners)
                g2.setPaint(new TexturePaint(picture, new Rectangle(0, 0, 40, 40)));
                g2.fillRoundRect(0, 0, 40, 40, 22, 22);
            } else {
                g2.setColor(Theme.YELLOW);
                g2.fillRoundRect(0, 0, 40, 40, 22, 22);
                g2.setColor(Theme.PURPLE_DEEP);
                g2.setFont(Theme.bold(24));
                int w = g2.getFontMetrics().stringWidth("T");
                g2.drawString("T", (40 - w) / 2, 29);
            }
            g2.setColor(new Color(250, 204, 21, 140));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, 38, 38, 22, 22);
            g2.dispose();
        }
    }
}
