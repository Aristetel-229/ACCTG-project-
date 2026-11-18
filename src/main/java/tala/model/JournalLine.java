package tala.model;

import java.math.BigDecimal;

/**
 * JournalLine
 * ------------------------------------------------------------
 * One debit or credit line of a journal entry.
 * A line has a debit amount OR a credit amount (the other is 0).
 */
public class JournalLine {

    private Account account;
    private BigDecimal debit;
    private BigDecimal credit;

    public JournalLine(Account account, BigDecimal debit, BigDecimal credit) {
        this.account = account;
        this.debit = debit;
        this.credit = credit;
    }

    /** Shortcut: a debit line */
    public static JournalLine debit(Account account, BigDecimal amount) {
        return new JournalLine(account, amount, BigDecimal.ZERO);
    }

    /** Shortcut: a credit line */
    public static JournalLine credit(Account account, BigDecimal amount) {
        return new JournalLine(account, BigDecimal.ZERO, amount);
    }

    public Account getAccount()   { return account; }
    public BigDecimal getDebit()  { return debit; }
    public BigDecimal getCredit() { return credit; }

    public boolean isDebit() {
        return debit.compareTo(BigDecimal.ZERO) > 0;
    }
}
