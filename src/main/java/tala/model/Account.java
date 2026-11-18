package tala.model;

/**
 * Account
 * ------------------------------------------------------------
 * One row of the "accounts" table (one line of the Chart of Accounts).
 * Example: 1010 - Cash, Asset, normal balance Debit.
 */
public class Account {

    public static final String DEBIT = "Debit";
    public static final String CREDIT = "Credit";

    private int id;
    private int code;
    private String name;
    private AccountType type;
    private String normalBalance;   // "Debit" or "Credit"

    public Account(int id, int code, String name, AccountType type, String normalBalance) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.type = type;
        this.normalBalance = normalBalance;
    }

    public int getId()                { return id; }
    public int getCode()              { return code; }
    public String getName()           { return name; }
    public AccountType getType()      { return type; }
    public String getNormalBalance()  { return normalBalance; }

    public void setId(int id)         { this.id = id; }

    /** True when the account grows on the debit side (Assets, Expenses, Drawing). */
    public boolean isDebitNormal() {
        return DEBIT.equals(normalBalance);
    }

    /** Used by combo boxes, e.g. "1010 - Cash" */
    @Override
    public String toString() {
        return code + " - " + name;
    }
}
