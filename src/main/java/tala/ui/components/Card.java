package tala.ui.components;

import tala.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * Card
 * ------------------------------------------------------------
 * A white rounded card with three parts:
 *
 *   +-------------------------------------------+
 *   | Title                        [right side] |   <- header
 *   | subtitle                                  |
 *   |-------------------------------------------|
 *   | body (a table, a form, ...)               |   <- body
 *   |-------------------------------------------|
 *   | footer (optional totals)                  |   <- footer
 *   +-------------------------------------------+
 */
public class Card extends RoundedPanel {

    private final JLabel titleLabel = new JLabel();
    private final JLabel subtitleLabel = new JLabel();
    private final JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    private final JPanel headerLeft = new JPanel(new BorderLayout(12, 0));
    private final JPanel header = new JPanel(new BorderLayout(12, 0));

    public Card(String title, String subtitle) {
        super(new BorderLayout());

        titleLabel.setText(title);
        titleLabel.setFont(Theme.bold(20));
        titleLabel.setForeground(Theme.TITLE);
        subtitleLabel.setFont(Theme.regular(14));
        subtitleLabel.setForeground(Theme.MUTED);
        setSubtitle(subtitle);

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);
        titles.add(titleLabel);
        titles.add(subtitleLabel);

        headerLeft.setOpaque(false);
        headerLeft.add(titles, BorderLayout.CENTER);
        headerRight.setOpaque(false);

        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.DIVIDER),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)));
        header.add(headerLeft, BorderLayout.CENTER);
        header.add(headerRight, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    /** A card without a header (for small info cards) */
    public static Card plain() {
        Card card = new Card("", null);
        card.remove(card.header);
        return card;
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle == null ? "" : subtitle);
        subtitleLabel.setVisible(subtitle != null && !subtitle.isEmpty());
    }

    /** Puts an icon square to the left of the title */
    public void setHeaderIcon(JComponent iconBox) {
        headerLeft.add(iconBox, BorderLayout.WEST);
    }

    /** Adds buttons, badges or search boxes to the right side of the header */
    public void addToHeader(JComponent component) {
        headerRight.add(component);
    }

    /** Adds something right after the title text (e.g. a count badge) */
    public void addNextToTitle(JComponent component) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        JPanel titles = (JPanel) titleLabel.getParent();
        titles.remove(titleLabel);
        row.add(titleLabel);
        row.add(Box.createHorizontalStrut(10));
        row.add(component);
        row.setAlignmentX(LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(LEFT_ALIGNMENT);
        titles.add(row, 0);
    }

    public void setBody(JComponent body) {
        add(body, BorderLayout.CENTER);
    }

    /** A grey strip at the bottom of the card (for totals and status badges) */
    public void setFooter(JComponent footer) {
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.DIVIDER),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)));
        add(footer, BorderLayout.SOUTH);
    }
}
