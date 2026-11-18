package tala.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JournalEntry
 * ------------------------------------------------------------
 * One transaction: a date, a reference number, a description
 * and its debit/credit lines.
 *
 * Golden rule of double-entry: total debits = total credits.
 */
public class JournalEntry {

    public static final String REGULAR = "Regular";
    public static final String ADJUSTING = "Adjusting";

    private int id;
    private LocalDate date;
    private String referenceNo;
    private String description;
    private String entryType;              // REGULAR or ADJUSTING
    private List<JournalLine> lines = new ArrayList<>();

    public JournalEntry(int id, LocalDate date, String referenceNo, String description, String entryType) {
        this.id = id;
        this.date = date;
        this.referenceNo = referenceNo;
        this.description = description;
        this.entryType = entryType;
    }

    public int getId()                { return id; }
    public LocalDate getDate()        { return date; }
    public String getReferenceNo()    { return referenceNo; }
    public String getDescription()    { return description; }
    public String getEntryType()      { return entryType; }
    public List<JournalLine> getLines() { return lines; }

    public void setId(int id)         { this.id = id; }

    public void addLine(JournalLine line) {
        lines.add(line);
    }

    public boolean isAdjusting() {
        return ADJUSTING.equals(entryType);
    }

    /** Adds up every debit amount */
    public BigDecimal getTotalDebit() {
        BigDecimal total = BigDecimal.ZERO;
        for (JournalLine line : lines) {
            total = total.add(line.getDebit());
        }
        return total;
    }

    /** Adds up every credit amount */
    public BigDecimal getTotalCredit() {
        BigDecimal total = BigDecimal.ZERO;
        for (JournalLine line : lines) {
            total = total.add(line.getCredit());
        }
        return total;
    }

    /** Balanced = has lines, debits are above zero, and debits equal credits */
    public boolean isBalanced() {
        return !lines.isEmpty()
                && getTotalDebit().compareTo(BigDecimal.ZERO) > 0
                && getTotalDebit().compareTo(getTotalCredit()) == 0;
    }

    /** Names of the debited accounts, e.g. "Cash" (or "Cash, Supplies") */
    public String getDebitAccountNames() {
        return joinNames(true);
    }

    /** Names of the credited accounts */
    public String getCreditAccountNames() {
        return joinNames(false);
    }

    private String joinNames(boolean debitSide) {
        StringBuilder names = new StringBuilder();
        for (JournalLine line : lines) {
            if (line.isDebit() == debitSide) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(line.getAccount().getName());
            }
        }
        return names.toString();
    }
}
