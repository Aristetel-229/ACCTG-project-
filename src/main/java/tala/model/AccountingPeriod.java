package tala.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * AccountingPeriod
 * ------------------------------------------------------------
 * The span of months you are recording, e.g. November 2026 to December 2026.
 * A journal entry's date must fall inside it:
 *   first day of the "from" month  ...  last day of the "to" month
 */
public class AccountingPeriod {

    private final YearMonth from;
    private final YearMonth to;

    public AccountingPeriod(YearMonth from, YearMonth to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("The period cannot end before it starts.");
        }
        this.from = from;
        this.to = to;
    }

    /** A one-month period, e.g. only November 2026 */
    public static AccountingPeriod ofMonth(YearMonth month) {
        return new AccountingPeriod(month, month);
    }

    public YearMonth getFrom() { return from; }
    public YearMonth getTo()   { return to; }

    public LocalDate getFirstDay() { return from.atDay(1); }
    public LocalDate getLastDay()  { return to.atEndOfMonth(); }

    /** Is this date inside the period? (first and last day included) */
    public boolean contains(LocalDate date) {
        return date != null && !date.isBefore(getFirstDay()) && !date.isAfter(getLastDay());
    }

    /**
     * Short label for the badge:
     *   same month  -> "Nov. 2026"
     *   same year   -> "Nov. - Dec. 2026"
     *   two years   -> "Dec. 2026 - Jan. 2027"
     */
    public String getLabel() {
        String dash = " \u2013 ";   // an en dash
        if (from.equals(to)) {
            return shortMonth(from) + " " + from.getYear();
        }
        if (from.getYear() == to.getYear()) {
            return shortMonth(from) + dash + shortMonth(to) + " " + to.getYear();
        }
        return shortMonth(from) + " " + from.getYear() + dash + shortMonth(to) + " " + to.getYear();
    }

    /** "November 2026 to December 2026" (for messages) */
    public String getLongLabel() {
        return longMonth(from) + " to " + longMonth(to);
    }

    /** "Nov.", "Dec." ... but "May" has no dot because it is not shortened */
    private static String shortMonth(YearMonth month) {
        String full = month.format(DateTimeFormatter.ofPattern("MMMM", Locale.US));
        String abbreviation = month.format(DateTimeFormatter.ofPattern("MMM", Locale.US));
        return full.equals(abbreviation) ? full : abbreviation + ".";
    }

    public static String longMonth(YearMonth month) {
        return month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US));
    }
}
