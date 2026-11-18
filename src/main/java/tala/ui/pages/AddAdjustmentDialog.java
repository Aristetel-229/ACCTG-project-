package tala.ui.pages;

import tala.dao.AccountDAO;
import tala.dao.JournalDAO;
import tala.model.Account;
import tala.model.JournalEntry;
import tala.model.JournalLine;
import tala.ui.components.AppButton;
import tala.ui.components.Badge;
import tala.ui.components.FormField;
import tala.ui.components.IconBox;
import tala.ui.components.Icons;
import tala.ui.components.RoundedPanel;
import tala.ui.components.WrapLabel;
import tala.ui.theme.Theme;
import tala.util.DateUtil;
import tala.util.Money;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * AddAdjustmentDialog
 * ------------------------------------------------------------
 * The pop-up for an ADJUSTING entry (supplies used, insurance expired,
 * depreciation, accrued salaries ...). It is saved like a journal entry
 * but with type "Adjusting", so it lands in the Adjustments columns
 * of the 12 Columnar worksheet. Reference numbers: AJE-000001, ...
 */
public class AddAdjustmentDialog extends JDialog {

    private final JournalDAO journalDAO;
    private boolean posted = false;

    private final JTextField dateField = new JTextField();
    private final JTextField amountField = new JTextField();
    private final JTextField descriptionField = new JTextField();
    private final JComboBox<Account> debitCombo = new JComboBox<>();
    private final JComboBox<Account> creditCombo = new JComboBox<>();
    private final JLabel previewLabel = new JLabel();
    private final Badge statusBadge = Badge.warn("Enter an amount above zero");

    public AddAdjustmentDialog(JFrame owner, AccountDAO accountDAO, JournalDAO journalDAO) throws SQLException {
        super(owner, "Add adjustment", true);   // true = modal (blocks the window behind it)
        this.journalDAO = journalDAO;

        // fill the account lists; start on an expense (Dr) and an asset (Cr), the most common adjustment
        List<Account> accounts = accountDAO.getAllAccounts();
        for (Account a : accounts) {
            debitCombo.addItem(a);
            creditCombo.addItem(a);
        }
        selectFirstCode(debitCombo, '5');
        selectFirstCode(creditCombo, '1');

        // adjustments are usually dated on the last day of the month
        dateField.setText(DateUtil.toInput(YearMonth.now().atEndOfMonth()));

        RoundedPanel panel = new RoundedPanel(new BorderLayout());
        panel.setRadius(20).setShadow(false);
        panel.add(createTop(), BorderLayout.NORTH);
        panel.add(createForm(), BorderLayout.CENTER);
        panel.add(createButtons(), BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);
        content.add(panel);
        setContentPane(content);

        // live preview
        DocumentListener onType = new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { updatePreview(); }
            public void removeUpdate(DocumentEvent e)  { updatePreview(); }
            public void changedUpdate(DocumentEvent e) { updatePreview(); }
        };
        amountField.getDocument().addDocumentListener(onType);
        debitCombo.addActionListener(e -> updatePreview());
        creditCombo.addActionListener(e -> updatePreview());
        updatePreview();

        // Esc closes the dialog
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setSize(620, 560);
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /** True if the user saved an adjustment (so the worksheet should reload) */
    public boolean wasPosted() {
        return posted;
    }

    // ------------------------------------------------------------ parts

    private JComponent createTop() {
        RoundedPanel top = new RoundedPanel(new BorderLayout(14, 0));
        top.setRadius(0).setShadow(false).setBorderColor(null).setGradient(Theme.HEADER_GLOW, 0.55f);
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.DIVIDER),
                BorderFactory.createEmptyBorder(20, 24, 16, 20)));

        JLabel title = new JLabel("Add adjustment");
        title.setFont(Theme.bold(22));
        title.setForeground(Theme.TITLE);
        WrapLabel sub = new WrapLabel("Adjusting entries fill the Adjustments column and update the adjusted balances.");
        sub.setFont(Theme.regular(14.5f));
        sub.setForeground(Theme.TEXT_SOFT);
        JPanel texts = new JPanel(new BorderLayout(0, 2));
        texts.setOpaque(false);
        texts.add(title, BorderLayout.NORTH);
        texts.add(sub, BorderLayout.CENTER);

        JButton close = new JButton(Icons.get(Icons.CLOSE, 18, Theme.PURPLE));
        close.setToolTipText("Close");
        close.getAccessibleContext().setAccessibleName("Close");
        close.setContentAreaFilled(false);
        close.setBorderPainted(false);
        close.setFocusPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addActionListener(e -> dispose());

        JPanel iconHolder = new JPanel(new BorderLayout());
        iconHolder.setOpaque(false);
        iconHolder.add(new IconBox(Icons.SLIDERS, 40, Theme.YELLOW, Theme.PURPLE_DEEP), BorderLayout.NORTH);

        top.add(iconHolder, BorderLayout.WEST);
        top.add(texts, BorderLayout.CENTER);
        top.add(close, BorderLayout.EAST);
        return top;
    }

    private JComponent createForm() {
        Theme.styleField(dateField);
        Theme.styleField(amountField);
        Theme.styleField(descriptionField);
        Theme.styleCombo(debitCombo);
        Theme.styleCombo(creditCombo);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(18, 24, 10, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        c.gridy = 0;
        c.gridx = 0; c.insets = new Insets(0, 0, 10, 8);
        form.add(new FormField("Date", dateField, "YYYY-MM-DD"), c);
        c.gridx = 1; c.insets = new Insets(0, 8, 10, 0);
        form.add(new FormField("Amount (" + Money.PESO_SIGN.trim() + ")", amountField, "Must be above zero"), c);

        c.gridy = 1; c.gridx = 0; c.gridwidth = 2; c.insets = new Insets(0, 0, 10, 0);
        form.add(new FormField("Description", descriptionField, "e.g. Supplies used during the month"), c);

        c.gridy = 2; c.gridwidth = 1;
        c.gridx = 0; c.insets = new Insets(0, 0, 10, 8);
        form.add(new FormField("Debit account", debitCombo, null), c);
        c.gridx = 1; c.insets = new Insets(0, 8, 10, 0);
        form.add(new FormField("Credit account", creditCombo, null), c);

        // preview strip
        RoundedPanel strip = new RoundedPanel(new BorderLayout(10, 0));
        strip.setRadius(12).setShadow(false).setFill(new Color(0xfaf8fe)).setBorderColor(Theme.DIVIDER);
        strip.setBorder(BorderFactory.createEmptyBorder(10, 14, 12, 14));
        previewLabel.setFont(Theme.regular(15));
        previewLabel.setForeground(Theme.TEXT);
        strip.add(previewLabel, BorderLayout.CENTER);
        JPanel badgeHolder = new JPanel(new GridBagLayout());
        badgeHolder.setOpaque(false);
        badgeHolder.add(statusBadge);
        strip.add(badgeHolder, BorderLayout.EAST);

        c.gridy = 3; c.gridx = 0; c.gridwidth = 2; c.insets = new Insets(2, 0, 0, 0);
        form.add(strip, c);
        return form;
    }

    private JComponent createButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.DIVIDER),
                BorderFactory.createEmptyBorder(14, 16, 16, 16)));
        AppButton cancel = new AppButton("Cancel", AppButton.Style.SECONDARY);
        cancel.addActionListener(e -> dispose());
        AppButton add = new AppButton("Add adjustment", Icons.get(Icons.PLUS, 16, Color.WHITE), AppButton.Style.PRIMARY);
        add.addActionListener(e -> save());
        buttons.add(cancel);
        buttons.add(add);
        getRootPane().setDefaultButton(add);   // Enter = Add adjustment
        return buttons;
    }

    // ------------------------------------------------------------ logic

    private void updatePreview() {
        Account debit = (Account) debitCombo.getSelectedItem();
        Account credit = (Account) creditCombo.getSelectedItem();
        BigDecimal amount = null;
        try {
            amount = Money.parse(amountField.getText());
        } catch (IllegalArgumentException ignored) {
            // not a valid number yet
        }
        String amountText = Money.peso(amount == null ? BigDecimal.ZERO : amount);
        previewLabel.setText("<html><b style='color:#5b21b6'>Debit</b> " + (debit == null ? "-" : debit.getName())
                + " &middot; " + amountText + " &nbsp;&rarr;&nbsp; <b style='color:#7a4f01'>Credit</b> "
                + (credit == null ? "-" : credit.getName()) + " &middot; " + amountText + "</html>");

        if (amount == null || amount.signum() <= 0) {
            statusBadge.showWarn("Enter an amount above zero");
        } else if (debit != null && credit != null && debit.getId() == credit.getId()) {
            statusBadge.showWarn("Pick two different accounts");
        } else {
            statusBadge.showOk("Ready to add");
        }
    }

    private void save() {
        LocalDate date = DateUtil.parse(dateField.getText());
        Account debit = (Account) debitCombo.getSelectedItem();
        Account credit = (Account) creditCombo.getSelectedItem();
        String description = descriptionField.getText().trim();

        String problem = null;
        BigDecimal amount = null;
        try {
            amount = Money.parse(amountField.getText());
        } catch (IllegalArgumentException e) {
            problem = e.getMessage();
        }
        if (problem == null) {
            if (date == null) {
                problem = "Enter a real date as YYYY-MM-DD.";
            } else if (amount.signum() <= 0) {
                problem = "The amount must be above zero.";
            } else if (debit == null || credit == null) {
                problem = "Choose a debit account and a credit account.";
            } else if (debit.getId() == credit.getId()) {
                problem = "The debit and credit accounts must be different.";
            } else if (description.isEmpty()) {
                problem = "Describe the adjustment, e.g. Supplies used during the month.";
            }
        }
        if (problem != null) {
            JOptionPane.showMessageDialog(this, problem, "Check the adjustment", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String reference = journalDAO.nextReferenceNo("AJE");
            JournalEntry entry = new JournalEntry(0, date, reference, description, JournalEntry.ADJUSTING);
            entry.addLine(JournalLine.debit(debit, amount));
            entry.addLine(JournalLine.credit(credit, amount));
            journalDAO.postEntry(entry);
            posted = true;
            dispose();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "The adjustment was NOT saved.\n\nDetails: " + e.getMessage(),
                    "Something went wrong", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Selects the first account whose code starts with the given digit */
    private void selectFirstCode(JComboBox<Account> combo, char firstDigit) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (String.valueOf(combo.getItemAt(i).getCode()).charAt(0) == firstDigit) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }
}
