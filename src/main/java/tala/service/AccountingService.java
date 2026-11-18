package tala.service;

import tala.model.Account;
import tala.model.AccountType;
import tala.model.DashboardSummary;
import tala.model.JournalEntry;
import tala.model.JournalLine;
import tala.model.LedgerRow;
import tala.model.Worksheet;
import tala.model.WorksheetRow;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static tala.model.WorksheetRow.*;

/**
 * AccountingService
 * ------------------------------------------------------------
 * ALL the accounting math is here, in plain Java (no SQL).
 * The screens load accounts and entries with the DAOs, then
 * ask this class for balances, ledgers and the worksheet.
 *
 * The two rules everything is built on:
 *   Debit-normal accounts  (Assets, Expenses, Drawing):   balance = debits - credits
 *   Credit-normal accounts (Liabilities, Capital, Revenue): balance = credits - debits
 * A NEGATIVE balance means the account is on its abnormal side.
 */
public class AccountingService {

    /** Small holder for "total debits and total credits" of one account */
    private static class Totals {
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;

        /** debit minus credit: positive = debit balance, negative = credit balance */
        BigDecimal net() {
            return debit.subtract(credit);
        }
    }

    // =================================================================
    //  Basic balances
    // =================================================================

    /**
     * Adds up the lines of the given entries, account by account.
     * onlyType = JournalEntry.REGULAR, JournalEntry.ADJUSTING, or null for all entries.
     */
    private Map<Integer, Totals> totalsByAccount(List<JournalEntry> entries, String onlyType) {
        Map<Integer, Totals> map = new HashMap<>();
        for (JournalEntry entry : entries) {
            if (onlyType != null && !onlyType.equals(entry.getEntryType())) {
                continue;   // skip entries of the other type
            }
            for (JournalLine line : entry.getLines()) {
                int accountId = line.getAccount().getId();
                Totals totals = map.get(accountId);
                if (totals == null) {
                    totals = new Totals();
                    map.put(accountId, totals);
                }
                totals.debit = totals.debit.add(line.getDebit());
                totals.credit = totals.credit.add(line.getCredit());
            }
        }
        return map;
    }

    /** Balance on the account's normal side (see the rules at the top) */
    private BigDecimal normalBalance(Account account, Totals totals) {
        if (totals == null) {
            return BigDecimal.ZERO;
        }
        return account.isDebitNormal()
                ? totals.debit.subtract(totals.credit)
                : totals.credit.subtract(totals.debit);
    }

    /** The current balance of one account (positive = normal side) */
    public BigDecimal getBalance(Account account, List<JournalEntry> entries) {
        return normalBalance(account, totalsByAccount(entries, null).get(account.getId()));
    }

    /** Total debits posted to one account */
    public BigDecimal getTotalDebits(Account account, List<JournalEntry> entries) {
        Totals t = totalsByAccount(entries, null).get(account.getId());
        return t == null ? BigDecimal.ZERO : t.debit;
    }

    /** Total credits posted to one account */
    public BigDecimal getTotalCredits(Account account, List<JournalEntry> entries) {
        Totals t = totalsByAccount(entries, null).get(account.getId());
        return t == null ? BigDecimal.ZERO : t.credit;
    }

    /** Only the accounts that appear in at least one entry, sorted by code */
    public List<Account> getUsedAccounts(List<Account> allAccounts, List<JournalEntry> entries) {
        Map<Integer, Totals> totals = totalsByAccount(entries, null);
        List<Account> used = new ArrayList<>();
        for (Account account : allAccounts) {
            if (totals.containsKey(account.getId())) {
                used.add(account);
            }
        }
        used.sort(Comparator.comparingInt(Account::getCode));
        return used;
    }

    // =================================================================
    //  General Ledger
    // =================================================================

    /** Every posting to one account, oldest first, with a running balance */
    public List<LedgerRow> buildLedger(Account account, List<JournalEntry> entries) {
        List<JournalEntry> sorted = sortByDate(entries);
        List<LedgerRow> rows = new ArrayList<>();
        BigDecimal running = BigDecimal.ZERO;

        for (JournalEntry entry : sorted) {
            for (JournalLine line : entry.getLines()) {
                if (line.getAccount().getId() != account.getId()) {
                    continue;
                }
                // Move the running balance the way this account's normal side says
                BigDecimal change = account.isDebitNormal()
                        ? line.getDebit().subtract(line.getCredit())
                        : line.getCredit().subtract(line.getDebit());
                running = running.add(change);

                rows.add(new LedgerRow(entry.getDate(), entry.getDescription(), entry.getReferenceNo(),
                        line.getDebit(), line.getCredit(), running));
            }
        }
        return rows;
    }

    /**
     * "DR" or "CR": which side a balance is really on.
     * Example: Cash (debit-normal) with -500 is really a 500 CREDIT balance.
     */
    public static String sideOf(Account account, BigDecimal normalSideBalance) {
        boolean onNormalSide = normalSideBalance.signum() >= 0;
        boolean debitSide = account.isDebitNormal() == onNormalSide;
        return debitSide ? "DR" : "CR";
    }

    // =================================================================
    //  Dashboard
    // =================================================================

    public DashboardSummary buildSummary(List<Account> accounts, List<JournalEntry> entries) {
        DashboardSummary s = new DashboardSummary();
        Map<Integer, Totals> totals = totalsByAccount(entries, null);
        BigDecimal ownerAccounts = BigDecimal.ZERO;   // Capital - Drawing

        for (Account account : accounts) {
            Totals t = totals.get(account.getId());
            if (t == null) {
                continue;   // account never used
            }
            switch (account.getType()) {
                case ASSET:
                    // contra assets (credit balance) automatically reduce this total
                    s.totalAssets = s.totalAssets.add(t.net());
                    break;
                case LIABILITY:
                    s.totalLiabilities = s.totalLiabilities.subtract(t.net());
                    break;
                case EQUITY:
                    ownerAccounts = ownerAccounts.subtract(t.net());
                    break;
                case REVENUE:
                    s.totalRevenue = s.totalRevenue.subtract(t.net());
                    break;
                case EXPENSE:
                    s.totalExpenses = s.totalExpenses.add(t.net());
                    break;
            }
            if (normalBalance(account, t).signum() < 0) {
                s.abnormalAccounts.add(account);
            }
        }

        s.netIncome = s.totalRevenue.subtract(s.totalExpenses);
        s.totalEquity = ownerAccounts.add(s.netIncome);
        s.entryCount = entries.size();
        return s;
    }

    // =================================================================
    //  12 Columnar worksheet
    // =================================================================

    /**
     * Builds the 12 Columnar worksheet from the entries.
     *
     *  1. Unadjusted TB : balances from REGULAR entries only
     *  2. Adjustments   : debits and credits from ADJUSTING entries
     *  3. Adjusted TB   : unadjusted balance + adjustment debits - adjustment credits
     *  4. Each adjusted balance is carried to ONE statement:
     *       Revenue / Expense  -> Income statement
     *       Capital / Drawing  -> Owner's equity
     *       Asset / Liability  -> Balance sheet
     *  5. Net income (or loss) closes the Income statement and moves to Owner's equity.
     *  6. Ending capital closes Owner's equity and moves to the Balance sheet (credit side).
     *  7. The last row shows the Balance sheet DIFFERENCE, which must be 0.00.
     */
    public Worksheet buildWorksheet(List<Account> accounts, List<JournalEntry> entries) {
        Worksheet sheet = new Worksheet();
        Map<Integer, Totals> regular = totalsByAccount(entries, JournalEntry.REGULAR);
        Map<Integer, Totals> adjusting = totalsByAccount(entries, JournalEntry.ADJUSTING);

        BigDecimal[] columnTotals = zeros();

        List<Account> sortedAccounts = new ArrayList<>(accounts);
        sortedAccounts.sort(Comparator.comparingInt(Account::getCode));

        // ---------- Steps 1 to 4: one row per USED account ----------
        for (Account account : sortedAccounts) {
            Totals unadj = regular.get(account.getId());
            Totals adj = adjusting.get(account.getId());
            if (unadj == null && adj == null) {
                continue;   // only accounts that were actually used are listed
            }
            WorksheetRow row = new WorksheetRow(String.valueOf(account.getCode()), account.getName(), ACCOUNT);

            BigDecimal unadjustedNet = unadj == null ? BigDecimal.ZERO : unadj.net();
            putOnSide(row, UNADJ_DR, unadjustedNet);

            BigDecimal adjustedNet = unadjustedNet;
            if (adj != null) {
                row.set(ADJ_DR, nullIfZero(adj.debit));
                row.set(ADJ_CR, nullIfZero(adj.credit));
                adjustedNet = adjustedNet.add(adj.debit).subtract(adj.credit);
            }
            putOnSide(row, ADJTB_DR, adjustedNet);
            putOnSide(row, statementColumnFor(account.getType()), adjustedNet);

            addToTotals(columnTotals, row);
            sheet.getRows().add(row);
        }

        if (sheet.getRows().isEmpty()) {
            return sheet;   // nothing posted yet: the worksheet starts empty
        }

        // ---------- Totals row ----------
        WorksheetRow totalRow = new WorksheetRow("", "Total", TOTAL);
        for (int c = 0; c < CELL_COUNT; c++) {
            totalRow.set(c, columnTotals[c]);
        }
        sheet.getRows().add(totalRow);

        // ---------- Step 5: Net income / Net loss ----------
        BigDecimal netIncome = columnTotals[IS_CR].subtract(columnTotals[IS_DR]);
        WorksheetRow netRow = new WorksheetRow("", netIncome.signum() < 0 ? "Net loss" : "Net income", NET_INCOME);
        if (netIncome.signum() >= 0) {
            netRow.set(IS_DR, netIncome);          // balances the Income statement columns
            netRow.set(OE_CR, netIncome);          // income increases capital
        } else {
            netRow.set(IS_CR, netIncome.negate());
            netRow.set(OE_DR, netIncome.negate()); // a loss decreases capital
        }
        sheet.getRows().add(netRow);

        // ---------- Step 6: Ending capital ----------
        BigDecimal oeDebits = columnTotals[OE_DR].add(valueOf(netRow.get(OE_DR)));
        BigDecimal oeCredits = columnTotals[OE_CR].add(valueOf(netRow.get(OE_CR)));
        BigDecimal endingCapital = oeCredits.subtract(oeDebits);

        WorksheetRow capitalRow = new WorksheetRow("", "Owner's equity, end", CAPITAL);
        if (endingCapital.signum() >= 0) {
            capitalRow.set(OE_DR, endingCapital);  // closes the Owner's equity columns
            capitalRow.set(BS_CR, endingCapital);  // shows as equity on the Balance sheet
        } else {
            capitalRow.set(OE_CR, endingCapital.negate());
            capitalRow.set(BS_DR, endingCapital.negate());
        }
        sheet.getRows().add(capitalRow);

        // ---------- Step 7: Grand total ----------
        WorksheetRow grand = new WorksheetRow("", "Grand total", GRAND_TOTAL);
        for (int c = UNADJ_DR; c <= ADJTB_CR; c++) {
            grand.set(c, columnTotals[c]);
        }
        grand.set(IS_DR, columnTotals[IS_DR].add(valueOf(netRow.get(IS_DR))));
        grand.set(IS_CR, columnTotals[IS_CR].add(valueOf(netRow.get(IS_CR))));
        grand.set(OE_DR, oeDebits.add(valueOf(capitalRow.get(OE_DR))));
        grand.set(OE_CR, oeCredits.add(valueOf(capitalRow.get(OE_CR))));

        // Balance sheet: show the DIFFERENCE between the two sides (0.00 when balanced)
        BigDecimal bsDebits = columnTotals[BS_DR].add(valueOf(capitalRow.get(BS_DR)));
        BigDecimal bsCredits = columnTotals[BS_CR].add(valueOf(capitalRow.get(BS_CR)));
        BigDecimal difference = bsDebits.subtract(bsCredits);
        grand.set(BS_DR, difference.signum() > 0 ? difference : BigDecimal.ZERO.setScale(2));
        grand.set(BS_CR, difference.signum() < 0 ? difference.negate() : BigDecimal.ZERO.setScale(2));
        sheet.getRows().add(grand);

        // ---------- Key numbers for the tiles ----------
        int adjustingCount = 0;
        for (JournalEntry e : entries) {
            if (e.isAdjusting()) {
                adjustingCount++;
            }
        }
        sheet.setUnadjustedTotal(columnTotals[UNADJ_DR]);
        sheet.setAdjustmentsTotal(columnTotals[ADJ_DR]);
        sheet.setAdjustingEntryCount(adjustingCount);
        sheet.setNetIncome(netIncome);
        sheet.setEndingCapital(endingCapital);
        sheet.setBalanceSheetDifference(difference.abs());
        sheet.setTrialBalanceBalanced(
                columnTotals[UNADJ_DR].compareTo(columnTotals[UNADJ_CR]) == 0
                && columnTotals[ADJ_DR].compareTo(columnTotals[ADJ_CR]) == 0
                && columnTotals[ADJTB_DR].compareTo(columnTotals[ADJTB_CR]) == 0);
        return sheet;
    }

    // ---------------------------------------------------------------- helpers

    /** Which worksheet column pair an account type is carried to */
    private int statementColumnFor(AccountType type) {
        switch (type) {
            case REVENUE:
            case EXPENSE:
                return IS_DR;
            case EQUITY:
                return OE_DR;
            default:
                return BS_DR;   // Asset or Liability
        }
    }

    /**
     * Puts a net amount in the Dr column (if positive) or the Cr column (if negative).
     * drColumn is the Dr column of the pair; the Cr column is always drColumn + 1.
     */
    private void putOnSide(WorksheetRow row, int drColumn, BigDecimal net) {
        if (net.signum() > 0) {
            row.set(drColumn, net);
        } else if (net.signum() < 0) {
            row.set(drColumn + 1, net.negate());
        }
    }

    private void addToTotals(BigDecimal[] totals, WorksheetRow row) {
        for (int c = 0; c < CELL_COUNT; c++) {
            totals[c] = totals[c].add(valueOf(row.get(c)));
        }
    }

    private BigDecimal[] zeros() {
        BigDecimal[] values = new BigDecimal[CELL_COUNT];
        for (int c = 0; c < CELL_COUNT; c++) {
            values[c] = BigDecimal.ZERO.setScale(2);
        }
        return values;
    }

    private BigDecimal valueOf(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private BigDecimal nullIfZero(BigDecimal amount) {
        return amount.signum() == 0 ? null : amount;
    }

    /** A copy of the entries sorted by date (then by id, the order they were posted) */
    public List<JournalEntry> sortByDate(List<JournalEntry> entries) {
        List<JournalEntry> copy = new ArrayList<>(entries);
        copy.sort(Comparator.comparing(JournalEntry::getDate).thenComparingInt(JournalEntry::getId));
        return copy;
    }
}
