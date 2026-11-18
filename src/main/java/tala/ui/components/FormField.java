package tala.ui.components;

import tala.ui.theme.Theme;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;

/**
 * FormField
 * ------------------------------------------------------------
 * A label on top, the input under it, and an optional small hint:
 *
 *   Amount (P)
 *   [ 0.00            ]
 *   Must be above zero
 */
public class FormField extends JPanel {

    private final WrapLabel hintLabel = new WrapLabel("");

    public FormField(String labelText, JComponent input, String hint) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JLabel label = new JLabel(labelText);
        label.setFont(Theme.bold(15));
        label.setForeground(Theme.TITLE);
        label.setLabelFor(input);              // screen readers link the label to the input
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        input.setAlignmentX(Component.LEFT_ALIGNMENT);
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        hintLabel.setFont(Theme.regular(13.5f));
        hintLabel.setForeground(Theme.MUTED);
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        setHint(hint);

        add(label);
        add(Box.createVerticalStrut(6));
        add(input);
        add(Box.createVerticalStrut(4));
        add(hintLabel);
    }

    public void setHint(String hint) {
        hintLabel.setText(hint);
        hintLabel.setVisible(hint != null);   // no hint = no empty line
    }
}
