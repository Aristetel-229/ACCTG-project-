package tala.ui.components;

import tala.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;

/**
 * PageHeader
 * ------------------------------------------------------------
 * The banner at the top of every page:
 *   [yellow icon]  TALA / Journal Entry
 *                  Journal Entry                      [buttons]
 *                  One-line explanation of the page
 * White fading into soft yellow on the right.
 */
public class PageHeader extends RoundedPanel {

    private final WrapLabel description = new WrapLabel("");
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

    public PageHeader(String iconName, String title, String descriptionText) {
        super(new BorderLayout(20, 0));
        setGradient(Theme.HEADER_GLOW, 0.55f);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 22, 24));

        // left: big yellow icon box
        IconBox icon = new IconBox(iconName, 54, Theme.YELLOW, Theme.PURPLE_DEEP);
        JPanel iconHolder = new JPanel(new GridBagLayout());   // centers the icon vertically
        iconHolder.setOpaque(false);
        iconHolder.add(icon);
        add(iconHolder, BorderLayout.WEST);

        // middle: breadcrumb, title, description
        JLabel crumb = new JLabel("TALA / " + title);
        crumb.setFont(Theme.regular(13.5f));
        crumb.setForeground(Theme.MUTED);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.bold(31));
        titleLabel.setForeground(Theme.TITLE);

        description.setFont(Theme.regular(15.5f));
        description.setForeground(Theme.TEXT_SOFT);
        setDescription(descriptionText);

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        texts.add(crumb);
        texts.add(titleLabel);
        texts.add(description);
        add(texts, BorderLayout.CENTER);

        // right: action buttons
        actions.setOpaque(false);
        JPanel actionHolder = new JPanel(new GridBagLayout());
        actionHolder.setOpaque(false);
        actionHolder.add(actions);
        add(actionHolder, BorderLayout.EAST);
    }

    public void setDescription(String text) {
        description.setText(text);   // WrapLabel wraps long text onto more lines
    }

    public void addAction(JComponent button) {
        actions.add(button);
    }
}
