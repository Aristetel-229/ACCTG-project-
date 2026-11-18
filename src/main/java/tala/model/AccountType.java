package tala.model;

/**
 * AccountType
 * ------------------------------------------------------------
 * The five kinds of accounts in accounting.
 * Each type knows:
 *   - its label, exactly as stored in MySQL ("Asset", ...)
 *   - the first digit of its account codes (1 for Asset, ...)
 *   - its usual normal balance (Debit or Credit)
 */
public enum AccountType {

    ASSET    ("Asset",     '1', Account.DEBIT),
    LIABILITY("Liability", '2', Account.CREDIT),
    EQUITY   ("Equity",    '3', Account.CREDIT),
    REVENUE  ("Revenue",   '4', Account.CREDIT),
    EXPENSE  ("Expense",   '5', Account.DEBIT);

    private final String label;
    private final char codePrefix;
    private final String usualNormalBalance;

    AccountType(String label, char codePrefix, String usualNormalBalance) {
        this.label = label;
        this.codePrefix = codePrefix;
        this.usualNormalBalance = usualNormalBalance;
    }

    public String getLabel()              { return label; }
    public char getCodePrefix()           { return codePrefix; }
    public String getUsualNormalBalance() { return usualNormalBalance; }

    /** Turns the text from the database ("Asset") back into AccountType.ASSET */
    public static AccountType fromLabel(String text) {
        for (AccountType type : values()) {
            if (type.label.equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown account type: " + text);
    }

    /** Shown in combo boxes */
    @Override
    public String toString() {
        return label;
    }
}
