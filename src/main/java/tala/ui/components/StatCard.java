package tala.ui.components;

import tala.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

/**
 * StatCard
 * ------------------------------------------------------------
 * A Dashboard tile:
 *   [icon]  Total assets
 *   P15,000.00
 *   What the business owns
 *
 * Three looks: NORMAL (white), HIGHLIGHT (purple gradient, yellow number)
 * and CREAM (soft yellow).
 *
 * Point at a tile with the mouse: it lifts up, its border takes the icon's
 * color, and a tooltip explains how the number was worked out.
 */
public class StatCard extends RoundedPanel {

    public enum Look { NORMAL, HIGHLIGHT, CREAM }

    private final JLabel valueLabel = new JLabel("-");
    private final JLabel noteLabel = new JLabel();

    public StatCard(String iconName, Color iconBg, Color iconColor, String title, String note, Look look) {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(18, 22, 20, 22));

        Color titleColor = Theme.TEXT_SOFT;
        Color valueColor = Theme.TITLE;
        Color noteColor = Theme.MUTED;

        if (look == Look.HIGHLIGHT) {
            setFill(Theme.PURPLE_DARK).setGradient(Theme.PURPLE_BRIGHT, 0f).setBorderColor(Theme.PURPLE_DARK);
            titleColor = new Color(0xede4ff);
            valueColor = Theme.YELLOW_SOFT;
            noteColor = new Color(0xd9ccf7);
        } else if (look == Look.CREAM) {
            setFill(Theme.CREAM).setBorderColor(Theme.CREAM_BORDER);
        }
        setHoverable(look == Look.HIGHLIGHT ? Theme.YELLOW : iconColor);

        // top row: icon + title
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.bold(16));
        titleLabel.setForeground(titleColor);
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.X_AXIS));
        top.setOpaque(false);
        top.add(new IconBox(iconName, 40, iconBg, iconColor));
        top.add(javax.swing.Box.createHorizontalStrut(12));
        top.add(titleLabel);

        valueLabel.setFont(Theme.bold(32));
        valueLabel.setForeground(valueColor);
        valueLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 4, 0));

        noteLabel.setText(note);
        noteLabel.setFont(Theme.regular(14.5f));
        noteLabel.setForeground(noteColor);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        top.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        noteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(top);
        column.add(valueLabel);
        column.add(noteLabel);
        add(column, BorderLayout.CENTER);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setNote(String note) {
        noteLabel.setText(note);
    }

    /** The explanation shown when the mouse rests on the tile (simple HTML allowed) */
    public void setTip(String html) {
        setToolTipText("<html>" + html + "</html>");
    }
}
