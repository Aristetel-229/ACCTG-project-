package tala.ui.pages;

import tala.dao.AccountDAO;
import tala.dao.JournalDAO;
import tala.dao.SettingsDAO;
import tala.model.AccountingPeriod;
import tala.model.Account;
import tala.model.JournalEntry;
import tala.model.JournalLine;
import tala.ui.MainFrame;
import tala.ui.components.AppButton;
import tala.ui.components.Badge;
import tala.ui.components.Card;
import tala.ui.components.FormField;
import tala.ui.components.Icons;
import tala.ui.components.PageHeader;
import tala.ui.components.ReadOnlyTableModel;
import tala.ui.components.RoundedPanel;
import tala.ui.components.TalaTable;
import tala.ui.components.WrapLabel;
import tala.ui.theme.Theme;
import tala.util.DateUtil;
import tala.util.Money;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * JournalEntryPage
 * ------------------------------------------------------------
 * Record a transaction: choose the account to DEBIT and the account
 * to CREDIT, the amount, a date and a description. The preview shows
 * how it will post, and the checklist turns green as you fill it in.
 * Below, the posted entries can be deleted one at a time.
 *
 * The ACCOUNTING PERIOD (From month - To month) sets which dates are allowed:
 * an entry dated outside the period is refused. The period is saved in
 * MySQL (app_settings) so it is remembered next time.
 */
public class JournalEntryPage extends BasePage {

    private final SettingsDAO settingsDAO;

    // ---------- accounting period ----------
    private final JComboBox<YearMonth> fromCombo = new JComboBox<>();
    private final JComboBox<YearMonth> toCombo = new JComboBox<>();
    private final Badge periodBadge = Badge.info("Period");
    private AccountingPeriod period = AccountingPeriod.ofMonth(YearMonth.now());
    private boolean fillingPeriod = false;   // true while we fill the month lists (ignore their events)

    // ---------- form inputs ----------
    private final JTextField dateField = new JTextField();
    private final JTextField referenceField = new JTextField();
    private final JTextField amountField = new JTextField();
    private final JTextField descriptionField = new JTextField();
    private final JComboBox<Account> debitCombo = new JComboBox<>();
    private final JComboBox<Account> creditCombo = new JComboBox<>();

    // ---------- preview ----------
    private final TAccount debitPreview = new TAccount(true);
    private final TAccount creditPreview = new TAccount(false);
    private final CheckItem checkDate = new CheckItem("Date is valid");
    private final CheckItem checkPeriod = new CheckItem("Date is inside the accounting period");
    private final CheckItem checkAmount = new CheckItem("Amount is above zero");
    private final CheckItem checkAccounts = new CheckItem("Debit and credit accounts are different");
    private final CheckItem checkDescription = new CheckItem("Description is filled in");

    // ---------- posted entries ----------
    private final ReadOnlyTableModel entriesModel = new ReadOnlyTableModel(
            "Date", "Reference", "Type", "Description", "Debit", "Credit", "Amount");
    private final TalaTable entriesTable = new TalaTable(entriesModel);
    private final List<JournalEntry> shownEntries = new ArrayList<>();
    private final Badge countBadge = Badge.info("0");

    public JournalEntryPage(MainFrame mainFrame, AccountDAO accountDAO, JournalDAO journalDAO, SettingsDAO settingsDAO) {
        super(mainFrame, accountDAO, journalDAO);
        this.settingsDAO = settingsDAO;

        addSection(new PageHeader(Icons.PEN, "Journal Entry",
                "Record a transaction by choosing what is debited and what is credited. "
                + "Made a mistake? Select the entry below and delete just that one."));

        addSection(twoColumns(createFormCard(), 0.6, createPreviewCard(), 0.4));
        addSection(createEntriesCard());
        finishSections();

        // update the preview and checklist while the user types
        DocumentListener onType = new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { updatePreview(); }
            public void removeUpdate(DocumentEvent e)  { updatePreview(); }
            public void changedUpdate(DocumentEvent e) { updatePreview(); }
        };
        dateField.getDocument().addDocumentListener(onType);
        amountField.getDocument().addDocumentListener(onType);
        descriptionField.getDocument().addDocumentListener(onType);
        debitCombo.addActionListener(e -> updatePreview());
        creditCombo.addActionListener(e -> updatePreview());
        fromCombo.addActionListener(e -> periodChanged(true));
        toCombo.addActionListener(e -> periodChanged(false));
    }

    // =================================================================
    //  Building the cards
    // =================================================================

    private Card createFormCard() {
        Card card = new Card("New entry", "Fill in the details, then post");
        card.addToHeader(periodBadge);

        Theme.styleField(dateField);
        Theme.styleField(referenceField);
        Theme.styleField(amountField);
        Theme.styleField(descriptionField);
        Theme.styleCombo(debitCombo);
        Theme.styleCombo(creditCombo);
        referenceField.setEditable(false);
        referenceField.setBackground(Theme.READ_ONLY_BG);
        amountField.setToolTipText("e.g. 15000 or 15,000.00");

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(20, 20, 8, 20));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 12, 16);

        // row 0: the accounting period box
        c.gridy = 0; c.gridx = 0; c.gridwidth = 3; c.insets = new Insets(0, 0, 16, 0);
        grid.add(createPeriodBox(), c);
        c.gridwidth = 1; c.insets = new Insets(0, 0, 12, 16);

        c.gridy = 1;
        c.gridx = 0; grid.add(new FormField("Transaction date", dateField, "YYYY-MM-DD"), c);
        c.gridx = 1; grid.add(new FormField("Reference number", referenceField, "Filled in for you"), c);
        c.gridx = 2; c.insets = new Insets(0, 0, 12, 0);
        grid.add(new FormField("Amount (" + Money.PESO_SIGN.trim() + ")", amountField, "Must be above zero"), c);

        c.gridy = 2; c.gridx = 0; c.gridwidth = 3;
        grid.add(new FormField("Description", descriptionField, "What was this transaction for?"), c);

        JPanel accounts = new JPanel(new GridLayout(1, 2, 16, 0));
        accounts.setOpaque(false);
        accounts.add(new FormField("Debit account", debitCombo, "The account that receives (Dr)"));
        accounts.add(new FormField("Credit account", creditCombo, "The account that gives (Cr)"));
        c.gridy = 3;
        grid.add(accounts, c);
        card.setBody(grid);

        // footer: reminder + Post button
        AppButton post = new AppButton("Validate and post entry", AppButton.Style.PRIMARY);
        post.addActionListener(e -> postEntry());
        JLabel reminder = new JLabel("Debits must equal credits.");
        reminder.setFont(Theme.regular(14.5f));
        reminder.setForeground(Theme.TEXT_SOFT);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(reminder, BorderLayout.WEST);
        footer.add(post, BorderLayout.EAST);
        card.setFooter(footer);
        return card;
    }

    /**
     * The soft purple box:   Accounting period
     *                        From [November 2026]  to  [December 2026]   hint
     */
    private JPanel createPeriodBox() {
        RoundedPanel box = new RoundedPanel(new GridBagLayout());
        box.setRadius(14).setFill(new Color(0xf5f1fd)).setBorderColor(new Color(0xe4dbf7)).setShadow(false);
        box.setBorder(BorderFactory.createEmptyBorder(12, 16, 14, 16));

        // months are shown as "November 2026" instead of "2026-11"
        DefaultListCellRenderer monthRenderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focused) {
                Object text = value instanceof YearMonth ? AccountingPeriod.longMonth((YearMonth) value) : value;
                return super.getListCellRendererComponent(list, text, index, selected, focused);
            }
        };
        for (JComboBox<YearMonth> combo : java.util.Arrays.asList(fromCombo, toCombo)) {
            Theme.styleCombo(combo);
            combo.setRenderer(monthRenderer);
        }

        JLabel title = new JLabel("Accounting period");
        title.setFont(Theme.bold(15.5f));
        title.setForeground(Theme.TITLE);

        JLabel to = new JLabel("to");
        to.setFont(Theme.bold(14));
        to.setForeground(Theme.MUTED);

        WrapLabel hint = new WrapLabel("The transaction date must fall inside this period.");
        hint.setFont(Theme.regular(13.5f));
        hint.setForeground(Theme.MUTED);

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0; c.gridx = 0; c.gridwidth = 4; c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 8, 0);
        box.add(title, c);

        c.gridy = 1; c.gridwidth = 1; c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0; c.weightx = 1;
        box.add(new FormField("From", fromCombo, null), c);
        c.gridx = 1; c.weightx = 0; c.fill = GridBagConstraints.NONE; c.anchor = GridBagConstraints.SOUTH;
        c.insets = new Insets(0, 12, 14, 12);
        box.add(to, c);
        c.gridx = 2; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(0, 0, 0, 0);
        box.add(new FormField("To", toCombo, null), c);
        c.gridx = 3; c.weightx = 1.1; c.insets = new Insets(0, 16, 6, 0);
        box.add(hint, c);
        return box;
    }

    private Card createPreviewCard() {
        Card card = new Card("Entry preview", "How this entry will post");

        JPanel tAccounts = new JPanel(new GridLayout(1, 2, 12, 0));
        tAccounts.setOpaque(false);
        tAccounts.add(debitPreview);
        tAccounts.add(creditPreview);
        tAccounts.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel before = new JLabel("Before you post");
        before.setFont(Theme.bold(15.5f));
        before.setForeground(Theme.TITLE);
        before.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(18, 20, 20, 20));
        body.add(tAccounts);
        body.add(Box.createVerticalStrut(18));
        body.add(before);
        body.add(Box.createVerticalStrut(8));
        for (CheckItem item : new CheckItem[] {checkDate, checkPeriod, checkAmount, checkAccounts, checkDescription}) {
            item.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(item);
            body.add(Box.createVerticalStrut(6));
        }
        card.setBody(body);
        return card;
    }

    private Card createEntriesCard() {
        Card card = new Card("Posted entries", "Select an entry in the table, then delete just that one");
        card.addNextToTitle(countBadge);

        AppButton delete = new AppButton("Delete selected entry", Icons.get(Icons.TRASH, 16, Theme.RED), AppButton.Style.DANGER);
        delete.addActionListener(e -> deleteSelected());
        card.addToHeader(delete);

        entriesTable.setRightAligned(6);
        entriesTable.setPillColumns(1, 2);
        entriesTable.setColumnWidths(130, 140, 120, 0, 0, 0, 140);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        body.add(entriesTable.inPanel(), BorderLayout.CENTER);
        card.setBody(body);
        return card;
    }

    // =================================================================
    //  Data
    // =================================================================

    @Override
    public void refreshData() {
        try {
            // remember what was picked, so a refresh does not reset the combos
            Account oldDebit = (Account) debitCombo.getSelectedItem();
            Account oldCredit = (Account) creditCombo.getSelectedItem();

            List<Account> accounts = accountDAO.getAllAccounts();
            debitCombo.removeAllItems();
            creditCombo.removeAllItems();
            for (Account a : accounts) {
                debitCombo.addItem(a);
                creditCombo.addItem(a);
            }
            selectById(debitCombo, oldDebit, 0);
            selectById(creditCombo, oldCredit, Math.min(1, accounts.size() - 1));

            // posted entries, newest first
            List<JournalEntry> sorted = accounting.sortByDate(journalDAO.getAllEntries());

            loadPeriod(sorted);
            if (dateField.getText().trim().isEmpty()) {
                // today if it is inside the period, otherwise the period's first day
                LocalDate today = LocalDate.now();
                dateField.setText(DateUtil.toInput(period.contains(today) ? today : period.getFirstDay()));
            }
            referenceField.setText(journalDAO.nextReferenceNo("JE"));

            shownEntries.clear();
            entriesModel.clear();
            for (int i = sorted.size() - 1; i >= 0; i--) {
                JournalEntry e = sorted.get(i);
                shownEntries.add(e);
                entriesModel.addRow(new Object[] {
                        DateUtil.toLong(e.getDate()), e.getReferenceNo(), e.getEntryType(), e.getDescription(),
                        e.getDebitAccountNames(), e.getCreditAccountNames(), Money.format(e.getTotalDebit())});
            }
            countBadge.setText(String.valueOf(sorted.size()));

            updatePreview();
            revalidate();
        } catch (SQLException e) {
            showError("Could not load the accounts and entries from MySQL.", e);
        }
    }

    /** Reads the form, checks it, and saves the entry */
    private void postEntry() {
        List<String> problems = new ArrayList<>();

        LocalDate date = DateUtil.parse(dateField.getText());
        if (date == null) {
            problems.add("Enter a real date as YYYY-MM-DD, for example 2026-11-03.");
        } else if (!period.contains(date)) {
            problems.add("The date " + DateUtil.toLong(date) + " is outside the accounting period ("
                    + period.getLongLabel() + "). Change the date or the period.");
        }

        BigDecimal amount = null;
        try {
            amount = Money.parse(amountField.getText());
            if (amount.signum() <= 0) {
                problems.add("The amount must be above zero.");
            }
        } catch (IllegalArgumentException ex) {
            problems.add(ex.getMessage());
        }

        Account debit = (Account) debitCombo.getSelectedItem();
        Account credit = (Account) creditCombo.getSelectedItem();
        if (debit == null || credit == null) {
            problems.add("Choose a debit account and a credit account.");
        } else if (debit.getId() == credit.getId()) {
            problems.add("The debit and credit accounts must be different.");
        }

        String description = descriptionField.getText().trim();
        if (description.isEmpty()) {
            problems.add("Write a short description of the transaction.");
        } else if (description.length() > 255) {
            problems.add("Keep the description under 255 characters.");
        }

        if (!problems.isEmpty()) {
            showError("Please fix this before posting:\n- " + String.join("\n- ", problems), null);
            return;
        }

        // Build the entry: one debit line and one credit line for the same amount
        try {
            String reference = journalDAO.nextReferenceNo("JE");
            JournalEntry entry = new JournalEntry(0, date, reference, description, JournalEntry.REGULAR);
            entry.addLine(JournalLine.debit(debit, amount));
            entry.addLine(JournalLine.credit(credit, amount));
            journalDAO.postEntry(entry);

            amountField.setText("");
            descriptionField.setText("");
            refreshData();
            showInfo("Posted " + reference + ":  Dr " + debit.getName() + " / Cr " + credit.getName()
                    + "  " + Money.peso(amount));
        } catch (SQLException e) {
            showError("The entry was NOT saved (nothing was changed).", e);
        }
    }

    /** Deletes the selected row's entry after asking */
    private void deleteSelected() {
        int row = entriesTable.getSelectedRow();
        if (row < 0) {
            showInfo("First click an entry in the table, then press Delete selected entry.");
            return;
        }
        JournalEntry entry = shownEntries.get(entriesTable.convertRowIndexToModel(row));
        boolean yes = confirm("Delete entry",
                "Delete " + entry.getReferenceNo() + " (" + entry.getDescription() + ")?\n"
                + "It is removed from the journal, the ledger and the reports.",
                "Delete " + entry.getReferenceNo(), "Keep it");
        if (!yes) {
            return;
        }
        try {
            journalDAO.deleteEntry(entry.getId());
            refreshData();
        } catch (SQLException e) {
            showError("Could not delete the entry.", e);
        }
    }

    /** Keeps the T-accounts and the checklist up to date */
    private void updatePreview() {
        Account debit = (Account) debitCombo.getSelectedItem();
        Account credit = (Account) creditCombo.getSelectedItem();

        BigDecimal amount = null;
        try {
            amount = Money.parse(amountField.getText());
        } catch (IllegalArgumentException ignored) {
            // not a number yet: the checklist shows it
        }
        BigDecimal shown = amount == null ? BigDecimal.ZERO : amount;

        debitPreview.show(debit == null ? "-" : debit.getName(), shown);
        creditPreview.show(credit == null ? "-" : credit.getName(), shown);

        LocalDate date = DateUtil.parse(dateField.getText());
        checkDate.setDone(date != null);
        checkPeriod.setDone(period.contains(date));
        checkAmount.setDone(amount != null && amount.signum() > 0);
        checkAccounts.setDone(debit != null && credit != null && debit.getId() != credit.getId());
        checkDescription.setDone(!descriptionField.getText().trim().isEmpty());
    }

    // =================================================================
    //  Accounting period
    // =================================================================

    /** Reads the saved period and fills both month lists */
    private void loadPeriod(List<JournalEntry> entries) throws SQLException {
        period = settingsDAO.getAccountingPeriod();

        // the lists run from 2 years back (or the oldest entry) to 1 year ahead
        YearMonth first = YearMonth.now().minusMonths(24);
        YearMonth last = YearMonth.now().plusMonths(12);
        if (!entries.isEmpty()) {
            YearMonth oldest = YearMonth.from(entries.get(0).getDate());
            if (oldest.isBefore(first)) first = oldest;
        }
        if (period.getFrom().isBefore(first)) first = period.getFrom();
        if (period.getTo().isAfter(last)) last = period.getTo();

        fillingPeriod = true;
        fromCombo.removeAllItems();
        toCombo.removeAllItems();
        for (YearMonth m = first; !m.isAfter(last); m = m.plusMonths(1)) {
            fromCombo.addItem(m);
            toCombo.addItem(m);
        }
        fromCombo.setSelectedItem(period.getFrom());
        toCombo.setSelectedItem(period.getTo());
        fillingPeriod = false;
        periodBadge.setText("Period: " + period.getLabel());
    }

    /**
     * Runs when the user picks a month. Keeps From <= To
     * (moves the other month if needed), then saves the period.
     */
    private void periodChanged(boolean fromWasChanged) {
        if (fillingPeriod) {
            return;
        }
        YearMonth from = (YearMonth) fromCombo.getSelectedItem();
        YearMonth to = (YearMonth) toCombo.getSelectedItem();
        if (from == null || to == null) {
            return;
        }
        if (to.isBefore(from)) {
            fillingPeriod = true;
            if (fromWasChanged) {
                toCombo.setSelectedItem(from);
                to = from;
            } else {
                fromCombo.setSelectedItem(to);
                from = to;
            }
            fillingPeriod = false;
        }
        period = new AccountingPeriod(from, to);
        periodBadge.setText("Period: " + period.getLabel());
        updatePreview();
        revalidate();
        try {
            settingsDAO.saveAccountingPeriod(period);
        } catch (SQLException e) {
            showError("The period is used now, but could not be saved for next time.", e);
        }
    }

    private void selectById(JComboBox<Account> combo, Account wanted, int fallbackIndex) {
        if (wanted != null) {
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (combo.getItemAt(i).getId() == wanted.getId()) {
                    combo.setSelectedIndex(i);
                    return;
                }
            }
        }
        if (fallbackIndex >= 0 && fallbackIndex < combo.getItemCount()) {
            combo.setSelectedIndex(fallbackIndex);
        }
    }

    // =================================================================
    //  Small inner components
    // =================================================================

    /** A mini T-account:  name on top, Debit | Credit underneath */
    private static class TAccount extends RoundedPanel {
        private final boolean debitSide;
        private final JLabel name = new JLabel("", JLabel.CENTER);
        private final JLabel left = new JLabel();
        private final JLabel right = new JLabel();

        TAccount(boolean debitSide) {
            super(new BorderLayout());
            this.debitSide = debitSide;
            setRadius(14).setShadow(false);

            name.setFont(Theme.bold(16));
            name.setForeground(Theme.TITLE);
            name.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.TITLE),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            add(name, BorderLayout.NORTH);

            JPanel columns = new JPanel(new GridLayout(1, 2));
            columns.setOpaque(false);
            styleSide(left, "Debit");
            styleSide(right, "Credit");
            right.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 2, 0, 0, Theme.TITLE),
                    BorderFactory.createEmptyBorder(10, 12, 12, 12)));
            columns.add(left);
            columns.add(right);
            add(columns, BorderLayout.CENTER);
        }

        private void styleSide(JLabel label, String side) {
            label.setFont(Theme.regular(15));
            label.setForeground(Theme.TEXT);
            label.setVerticalAlignment(JLabel.TOP);
            label.setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));
        }

        void show(String accountName, BigDecimal amount) {
            name.setText(accountName);
            String amountText = Money.peso(amount);
            String small = "<span style='color:#7c6f99;font-size:10px'>";
            left.setText("<html>" + small + "Debit</span><br>" + (debitSide ? amountText : "&nbsp;") + "</html>");
            right.setText("<html>" + small + "Credit</span><br>" + (debitSide ? "&nbsp;" : amountText) + "</html>");
        }
    }

    /** One line of the "Before you post" checklist */
    private static class CheckItem extends JPanel {
        private final JLabel label = new JLabel();

        CheckItem(String text) {
            super(new FlowLayout(FlowLayout.LEFT, 0, 0));
            setOpaque(false);
            label.setText(text);
            label.setFont(Theme.regular(15));
            label.setIconTextGap(10);
            add(label);
            setDone(false);
        }

        void setDone(boolean done) {
            Color color = done ? Theme.GREEN : new Color(0x8a7fa3);
            label.setForeground(color);
            label.setIcon(Icons.get(done ? Icons.CHECK : Icons.CIRCLE, 18, color));
        }
    }
}
