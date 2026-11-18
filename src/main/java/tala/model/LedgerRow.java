package tala.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * LedgerRow
 * ------------------------------------------------------------
 * One line in an account's General Ledger:
 * date, description, reference, debit, credit and the running balance.
 *
 * runningBalance is measured on the account's NORMAL side:
 *   positive = normal balance (e.g. Cash with a debit balance)
 *   negative = abnormal balance (e.g. Cash with a credit balance)
 */
public class LedgerRow {

    private LocalDate date;
    private String description;
    private String referenceNo;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal runningBalance;

    public LedgerRow(LocalDate date, String description, String referenceNo,
                     BigDecimal debit, BigDecimal credit, BigDecimal runningBalance) {
        this.date = date;
        this.description = description;
        this.referenceNo = referenceNo;
        this.debit = debit;
        this.credit = credit;
        this.runningBalance = runningBalance;
    }

    public LocalDate getDate()            { return date; }
    public String getDescription()        { return description; }
    public String getReferenceNo()        { return referenceNo; }
    public BigDecimal getDebit()          { return debit; }
    public BigDecimal getCredit()         { return credit; }
    public BigDecimal getRunningBalance() { return runningBalance; }
}
