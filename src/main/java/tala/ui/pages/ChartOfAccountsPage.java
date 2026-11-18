package tala.ui.pages;

import tala.dao.AccountDAO;
import tala.dao.JournalDAO;
import tala.model.Account;
import tala.model.AccountType;
import tala.ui.MainFrame;
import tala.ui.components.AppButton;
import tala.ui.components.Badge;
import tala.ui.components.Card;
import tala.ui.components.FormField;
import tala.ui.components.IconBox;
import tala.ui.components.Icons;
import tala.ui.components.PageHeader;
import tala.ui.components.ReadOnlyTableModel;
import tala.ui.components.TalaTable;
import tala.ui.components.WrapLabel;
import tala.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ChartOfAccountsPage
 * ------------------------------------------------------------
 * Lists every account, filtered by type (chips) and by a search box.
 * The form on the right adds a new account, with these rules:
 *   - the code has 4 digits
 *   - the first digit matches the type (1 Asset ... 5 Expense)
 *   - the code and the name are not used yet
 */
public class ChartOfAccountsPage extends BasePage {

    private final ReadOnlyTableModel model = new ReadOnlyTableModel("Code", "Account name", "Type", "Normal balance", "Status");
    private final TalaTable table;
    private final List<Account> allAccounts = new ArrayList<>();
    private final List<Account> shownAccounts = new ArrayList<>();
    private final Set<Integer> addedThisSession = new HashSet<>();   // shown as "New"

    private final Card listCard = new Card("Accounts", "");
    private final JTextField searchField = new JTextField();
    private final JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final List<Chip> chips = new ArrayList<>();
    private String typeFilter = "All";
    private final JLabel emptyLabel = new JLabel("No accounts match your search. Try a different code or name.", JLabel.CENTER);

    // form
    private final JTextField codeField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JComboBox<AccountType> typeCombo = new JComboBox<>(AccountType.values());
    private final JComboBox<String> normalCombo = new JComboBox<>(new String[] {Account.DEBIT, Account.CREDIT});
    private final WrapLabel messageLabel = new WrapLabel("");

    public ChartOfAccountsPage(MainFrame mainFrame, AccountDAO accountDAO, JournalDAO journalDAO) {
        super(mainFrame, accountDAO, journalDAO);

        table = new TalaTable(model) {
            @Override protected Color rowBackground(int row, boolean selected) {
                boolean isNew = row < shownAccounts.size() && addedThisSession.contains(shownAccounts.get(row).getId());
                return isNew || selected ? Theme.CREAM : Color.WHITE;
            }
            @Override protected boolean rowBold(int row) {
                return false;
            }
        };
        table.setPillColumns(2, 4);
        table.setColumnWidths(90, 0, 140, 150, 110);

        PageHeader header = new PageHeader(Icons.LIST, "Chart of Accounts",
                "All the accounts your business can use, grouped by type. Add a new one from the panel on the right.");
        AppButton refresh = new AppButton("Refresh", Icons.get(Icons.REFRESH, 16, Theme.PURPLE_DEEP), AppButton.Style.SECONDARY);
        refresh.addActionListener(e -> refreshData());
        header.addAction(refresh);
        addSection(header);

        chipRow.setOpaque(false);
        String[][] chipInfo = {{"All", "All accounts"}, {"Asset", "Assets"}, {"Liability", "Liabilities"},
                {"Equity", "Equity"}, {"Revenue", "Revenue"}, {"Expense", "Expenses"}};
        for (String[] info : chipInfo) {
            Chip chip = new Chip(info[0], info[1]);
            chip.addActionListener(e -> {
                typeFilter = chip.type;
                fillTable();
            });
            if (!chips.isEmpty()) {
                chipRow.add(Box.createHorizontalStrut(8));   // gap between chips
            }
            chips.add(chip);
            chipRow.add(chip);
        }
        addSection(chipRow);

        addSection(twoColumns(createListCard(), 0.66, stack(createFormCard(), createGuideCard()), 0.34));
        finishSections();
    }

    // =================================================================
    //  Building the cards
    // =================================================================

    private Card createListCard() {
        Theme.styleField(searchField);
        searchField.setPreferredSize(new Dimension(230, 46));
        searchField.setToolTipText("Type a code or part of a name");
        JPanel search = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        search.setOpaque(false);
        JLabel searchLabel = new JLabel("Search");
        searchLabel.setFont(Theme.bold(15));
        searchLabel.setForeground(Theme.TITLE);
        searchLabel.setLabelFor(searchField);
        search.add(searchLabel);
        search.add(searchField);
        listCard.addToHeader(search);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { fillTable(); }
            public void removeUpdate(DocumentEvent e)  { fillTable(); }
            public void changedUpdate(DocumentEvent e) { fillTable(); }
        });

        emptyLabel.setFont(Theme.regular(15));
        emptyLabel.setForeground(Theme.MUTED);
        emptyLabel.setBorder(BorderFactory.createEmptyBorder(26, 20, 26, 20));

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        body.add(table.inPanel(), BorderLayout.CENTER);
        body.add(emptyLabel, BorderLayout.SOUTH);
        listCard.setBody(body);
        return listCard;
    }

    private Card createFormCard() {
        Card card = new Card("Add account", "It appears in the table right away");
        card.setHeaderIcon(new IconBox(Icons.PLUS, 40, Theme.YELLOW, Theme.PURPLE_DEEP));

        Theme.styleField(codeField);
        Theme.styleField(nameField);
        Theme.styleCombo(typeCombo);
        Theme.styleCombo(normalCombo);

        // choosing a type sets the usual normal balance for you
        typeCombo.addActionListener(e -> {
            AccountType type = (AccountType) typeCombo.getSelectedItem();
            normalCombo.setSelectedItem(type.getUsualNormalBalance());
            codeField.setToolTipText(type.getLabel() + " codes start with " + type.getCodePrefix());
        });

        messageLabel.setFont(Theme.bold(14.5f));
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        AppButton add = new AppButton("Add account", Icons.get(Icons.PLUS, 16, Color.WHITE), AppButton.Style.PRIMARY);
        add.addActionListener(e -> addAccount());
        add.setAlignmentX(Component.LEFT_ALIGNMENT);
        add.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(18, 20, 20, 20));
        FormField[] fields = {
            new FormField("Account code", codeField, "4 digits, e.g. 1340"),
            new FormField("Account name", nameField, "e.g. Prepaid Advertising"),
            new FormField("Type", typeCombo, null),
            new FormField("Normal balance", normalCombo,
                    "Set for you from the type. Change it only for contra accounts, like Accumulated Depreciation.")
        };
        for (FormField f : fields) {
            f.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(f);
            form.add(Box.createVerticalStrut(10));
        }
        form.add(messageLabel);
        form.add(Box.createVerticalStrut(10));
        form.add(add);
        card.setBody(form);
        return card;
    }

    private Card createGuideCard() {
        Card card = new Card("Code guide", null);
        JPanel rows = new JPanel(new GridLayout(5, 1));
        rows.setOpaque(false);
        rows.setBorder(BorderFactory.createEmptyBorder(6, 20, 10, 20));
        Color[][] colors = {
            {Theme.PURPLE_TINT, Theme.PURPLE}, {Theme.AMBER_BG, Theme.AMBER}, {Theme.BLUE_BG, Theme.BLUE},
            {Theme.GREEN_BG, Theme.GREEN}, {Theme.RED_BG, Theme.RED}};
        AccountType[] types = AccountType.values();
        for (int i = 0; i < types.length; i++) {
            JLabel range = new JLabel(types[i].getCodePrefix() + "000 - " + types[i].getCodePrefix() + "999");
            range.setFont(Theme.regular(15));
            range.setForeground(Theme.TEXT);
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, i < types.length - 1 ? 1 : 0, 0, Theme.ROW_LINE),
                    BorderFactory.createEmptyBorder(8, 0, 8, 0)));
            row.add(range, BorderLayout.WEST);
            row.add(new Badge(types[i].getLabel(), colors[i][0], colors[i][1]), BorderLayout.EAST);
            rows.add(row);
        }
        card.setBody(rows);
        return card;
    }

    // =================================================================
    //  Data
    // =================================================================

    @Override
    public void refreshData() {
        try {
            allAccounts.clear();
            allAccounts.addAll(accountDAO.getAllAccounts());
            fillTable();
        } catch (SQLException e) {
            showError("Could not load the chart of accounts from MySQL.", e);
        }
    }

    /** Shows the accounts that match the chip and the search box */
    private void fillTable() {
        String search = searchField.getText().trim().toLowerCase();
        model.clear();
        shownAccounts.clear();

        for (Account a : allAccounts) {
            boolean typeMatches = typeFilter.equals("All") || a.getType().getLabel().equals(typeFilter);
            boolean searchMatches = search.isEmpty()
                    || String.valueOf(a.getCode()).startsWith(search)
                    || a.getName().toLowerCase().contains(search);
            if (typeMatches && searchMatches) {
                shownAccounts.add(a);
                String status = addedThisSession.contains(a.getId()) ? "New" : "Active";
                model.addRow(new Object[] {a.getCode(), a.getName(), a.getType().getLabel(), a.getNormalBalance(), status});
            }
        }

        // chip labels with counts, e.g. "Assets 16"
        for (Chip chip : chips) {
            int count = 0;
            for (Account a : allAccounts) {
                if (chip.type.equals("All") || a.getType().getLabel().equals(chip.type)) {
                    count++;
                }
            }
            chip.setCount(count);
            chip.setSelected(chip.type.equals(typeFilter));
        }

        listCard.setSubtitle("Showing " + shownAccounts.size() + " of " + allAccounts.size() + " accounts");
        emptyLabel.setVisible(shownAccounts.isEmpty());
        revalidate();
        repaint();
    }

    /** Checks the form and saves the new account */
    private void addAccount() {
        String codeText = codeField.getText().trim();
        String name = nameField.getText().trim();
        AccountType type = (AccountType) typeCombo.getSelectedItem();
        String normal = (String) normalCombo.getSelectedItem();

        String error = null;
        try {
            if (!codeText.matches("\\d{4}")) {
                error = "Use a 4-digit account code.";
            } else if (codeText.charAt(0) != type.getCodePrefix()) {
                error = type.getLabel() + " codes start with " + type.getCodePrefix() + ". See the code guide below.";
            } else if (accountDAO.isCodeTaken(Integer.parseInt(codeText))) {
                error = "Code " + codeText + " is already used.";
            } else if (name.isEmpty()) {
                error = "Give the account a name.";
            } else if (name.length() > 100) {
                error = "Keep the name under 100 characters.";
            } else if (accountDAO.isNameTaken(name)) {
                error = "An account named " + name + " already exists.";
            }

            if (error != null) {
                showMessage(error, false);
                return;
            }

            Account account = new Account(0, Integer.parseInt(codeText), name, type, normal);
            accountDAO.addAccount(account);
            addedThisSession.add(account.getId());

            codeField.setText("");
            nameField.setText("");
            typeFilter = "All";
            searchField.setText("");
            refreshData();
            showMessage("Added " + account.getCode() + " - " + account.getName() + ".", true);
        } catch (SQLException e) {
            showError("Could not save the account.", e);
        }
    }

    private void showMessage(String text, boolean ok) {
        messageLabel.setText(text);
        messageLabel.setForeground(ok ? Theme.GREEN : Theme.RED);
    }

    // =================================================================
    //  Filter chip
    // =================================================================

    /** A rounded filter button, e.g.  ( Assets 16 ) ; purple when selected */
    private static class Chip extends JButton {
        final String type;
        private final String label;
        private int count;
        private boolean selectedChip;

        Chip(String type, String label) {
            this.type = type;
            this.label = label;
            setFont(Theme.bold(15));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setText(label);
        }

        void setCount(int count) {
            this.count = count;
            repaint();
        }

        @Override
        public void setSelected(boolean selected) {
            this.selectedChip = selected;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            int textW = getFontMetrics(getFont()).stringWidth(label);
            int countW = getFontMetrics(Theme.bold(13.5f)).stringWidth(String.valueOf(count)) + 16;
            return new Dimension(14 + textW + 8 + countW + 14, 44);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth() - 1, h = getHeight() - 1;

            g2.setColor(selectedChip ? Theme.PURPLE : (getModel().isRollover() ? Theme.PURPLE_HOVER : Color.WHITE));
            g2.fillRoundRect(0, 0, w, h, h, h);
            g2.setColor(selectedChip ? Theme.PURPLE : Theme.FIELD_BORDER);
            g2.drawRoundRect(0, 0, w, h, h, h);
            if (isFocusOwner()) {
                g2.setColor(Theme.FOCUS);
                g2.drawRoundRect(1, 1, w - 2, h - 2, h, h);
            }

            // label
            g2.setFont(getFont());
            int baseline = (h + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2;
            g2.setColor(selectedChip ? Color.WHITE : Theme.PURPLE_DEEP);
            g2.drawString(label, 14, baseline);

            // count bubble
            int x = 14 + g2.getFontMetrics().stringWidth(label) + 8;
            g2.setFont(Theme.bold(13.5f));
            String countText = String.valueOf(count);
            int bubbleW = g2.getFontMetrics().stringWidth(countText) + 16;
            g2.setColor(selectedChip ? Theme.YELLOW : Theme.PURPLE_TINT);
            g2.fillRoundRect(x, (h - 22) / 2, bubbleW, 22, 22, 22);
            g2.setColor(selectedChip ? Theme.PURPLE_DEEP : Theme.PURPLE);
            int countBase = (h + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2;
            g2.drawString(countText, x + 8, countBase);
            g2.dispose();
        }
    }
}
