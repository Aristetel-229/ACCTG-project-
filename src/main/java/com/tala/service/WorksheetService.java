// This class lives in the service package.
package com.tala.service;

// Our helper that opens database connections.
import com.tala.db.DB;

// Imports: BigDecimal for exact money, JDBC types for the query, and collection helpers.
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Calculations for the 12-column worksheet.
 *
 * The 12 value columns are six debit/credit pairs, in this order:
 *   0 Unadjusted Trial Balance | 1 Adjustments | 2 Adjusted Trial Balance
 *   3 Income Statement | 4 Owner's Equity | 5 Balance Sheet
 * Index of a pair's debit column = pair * 2, credit column = pair * 2 + 1.
 */
public final class WorksheetService {
    // Private constructor: only static methods are used.
    private WorksheetService() {}

    // Number of debit/credit column pairs on the worksheet.
    public static final int PAIRS = 6;
    // Total number of value columns (6 pairs x 2 = 12).
    public static final int COLS = PAIRS * 2;

    /** One account's net movement (debits minus credits) before and after adjustments. */
    public static final class AccountRow {
        // Identity of the account.
        public final String code, name, type;
        // unadjNet = net of normal entries; adjNet = net of adjusting entries (debit minus credit; positive = debit).
        public final BigDecimal unadjNet, adjNet;
        // Constructor: just stores the values.
        public AccountRow(String code, String name, String type, BigDecimal unadjNet, BigDecimal adjNet) {
            this.code = code; this.name = name; this.type = type;
            this.unadjNet = unadjNet; this.adjNet = adjNet;
        }
    }

    /** One worksheet line: an account with its 12 values. */
    public static final class Line {
        // Account code and name shown on the line.
        public final String code, name;
        // The 12 column amounts (debit/credit pairs).
        public final BigDecimal[] v;
        // Constructor: just stores the values.
        Line(String code, String name, BigDecimal[] v) { this.code = code; this.name = name; this.v = v; }
    }

    /** Everything the page needs to draw the worksheet. */
    public static final class Result {
        // One line per account.
        public List<Line> lines = new ArrayList<>();
        public BigDecimal income = BigDecimal.ZERO;                 // revenue - expenses (negative = loss)
        public BigDecimal[] netIncomeRow = zeros();                 // balancing figures for net income / loss
        public BigDecimal[] endingEquityRow = zeros();              // balancing figures for ending owner's equity
        public BigDecimal[] totals = zeros();                       // grand total of every column
        public BigDecimal[] differences = new BigDecimal[PAIRS];    // debit - credit of each pair (must be 0)
        // True when there are no accounts with posted activity.
        public boolean isEmpty() { return lines.isEmpty(); }
        // Largest absolute difference between debit and credit totals; 0 means the worksheet is balanced.
        public BigDecimal maxDifference() {
            // Start at zero.
            BigDecimal max = BigDecimal.ZERO;
            // Check each pair's difference and keep the biggest absolute value.
            for (BigDecimal d : differences) if (d.abs().compareTo(max) > 0) max = d.abs();
            // Return the biggest one.
            return max;
        }
    }

    // Creates an array of 12 BigDecimal zeros.
    private static BigDecimal[] zeros() {
        // New array with one slot per column.
        BigDecimal[] a = new BigDecimal[COLS];
        // Fill every slot with zero (instead of null).
        Arrays.fill(a, BigDecimal.ZERO);
        // Give it back.
        return a;
    }

    // Returns the number if it is greater than zero, otherwise zero (used to split a net into a debit or credit side).
    private static BigDecimal pos(BigDecimal n) { return n.signum() > 0 ? n : BigDecimal.ZERO; }

    /** Pure calculation (no database), so it can be tested on its own. */
    public static Result compute(List<AccountRow> accounts) {
        // Holder for all results.
        Result r = new Result();
        // Process each account one by one.
        for (AccountRow a : accounts) {
            // The 12 values of this account's worksheet line.
            BigDecimal[] v = zeros();
            BigDecimal adjusted = a.unadjNet.add(a.adjNet);          // debit-positive balance after adjustments
            // Columns 0/1 (Unadjusted Trial Balance): positive net goes to debit, negative net goes to credit.
            v[0] = pos(a.unadjNet);  v[1] = pos(a.unadjNet.negate());
            // Columns 2/3 (Adjustments): same rule using the adjustment amount.
            v[2] = pos(a.adjNet);    v[3] = pos(a.adjNet.negate());
            // Columns 4/5 (Adjusted Trial Balance): unadjusted + adjustments.
            v[4] = pos(adjusted);    v[5] = pos(adjusted.negate());
            // Decide where the adjusted balance goes next, depending on account type.
            switch (a.type) {
                // Revenue and expenses go to the Income Statement (columns 6/7).
                case "REVENUE", "EXPENSE" -> {
                    v[6] = pos(adjusted); v[7] = pos(adjusted.negate());
                    r.income = r.income.subtract(adjusted);          // revenue (credit) adds, expense (debit) subtracts
                }
                // Equity accounts go to the Owner's Equity columns (8/9).
                case "EQUITY" -> { v[8] = pos(adjusted); v[9] = pos(adjusted.negate()); }
                // Everything else goes to the Balance Sheet columns (10/11).
                default -> { v[10] = pos(adjusted); v[11] = pos(adjusted.negate()); } // ASSET, LIABILITY
            }
            // Add this line's values into the column totals.
            for (int j = 0; j < COLS; j++) r.totals[j] = r.totals[j].add(v[j]);
            // Save the line for display.
            r.lines.add(new Line(a.code, a.name, v));
        }
        // No accounts: everything is zero and balanced, so stop here.
        if (r.isEmpty()) {
            Arrays.fill(r.differences, BigDecimal.ZERO);
            return r;
        }
        // Net income: debit in Income Statement, credit in Owner's Equity (a loss is the opposite).
        if (r.income.signum() >= 0) {
            // Profit: balancing debit on Income Statement, credit on Owner's Equity.
            r.netIncomeRow[6] = r.income;           r.netIncomeRow[9] = r.income;
        } else {
            // Loss: balancing credit on Income Statement, debit on Owner's Equity.
            r.netIncomeRow[7] = r.income.negate();  r.netIncomeRow[8] = r.income.negate();
        }
        // Include the net income/loss row in the column totals.
        for (int j = 0; j < COLS; j++) r.totals[j] = r.totals[j].add(r.netIncomeRow[j]);

        // Ending owner's equity: balances the Owner's Equity pair and carries to the Balance Sheet credit side.
        BigDecimal ending = r.totals[9].subtract(r.totals[8]);
        if (ending.signum() >= 0) {
            // Credit-side equity: balancing debit in Owner's Equity, credit in Balance Sheet.
            r.endingEquityRow[8] = ending;           r.endingEquityRow[11] = ending;
        } else {
            // Negative equity: the opposite sides.
            r.endingEquityRow[9] = ending.negate();  r.endingEquityRow[10] = ending.negate();
        }
        // Include the ending equity row in the totals.
        for (int j = 0; j < COLS; j++) r.totals[j] = r.totals[j].add(r.endingEquityRow[j]);

        // For each pair, debit total minus credit total; should be zero if the books balance.
        for (int p = 0; p < PAIRS; p++) r.differences[p] = r.totals[p * 2].subtract(r.totals[p * 2 + 1]);
        // Return the finished worksheet.
        return r;
    }

    /** Loads only accounts that have posted lines (an empty database gives an empty worksheet). */
    public static List<AccountRow> loadAccounts() throws SQLException {
        // SQL: per account, sum (debit - credit) of POSTED lines, split into two sums:
        // unadj_net = normal entries (reference not starting with 'ADJ-'), adj_net = adjusting entries ('ADJ-%').
        // Grouped by account and sorted by code. No parameters to bind.
        String sql = """
            SELECT a.account_code, a.account_name, a.account_type,
                   SUM(CASE WHEN je.reference_no IS NULL OR je.reference_no NOT LIKE 'ADJ-%'
                            THEN jel.debit - jel.credit ELSE 0 END) AS unadj_net,
                   SUM(CASE WHEN je.reference_no LIKE 'ADJ-%'
                            THEN jel.debit - jel.credit ELSE 0 END) AS adj_net
            FROM accounts a
            JOIN journal_entry_lines jel ON jel.account_id = a.id
            JOIN journal_entries je ON je.id = jel.journal_entry_id
            WHERE je.status = 'POSTED'
            GROUP BY a.id, a.account_code, a.account_name, a.account_type
            ORDER BY a.account_code
            """;
        // List that will hold the result rows.
        List<AccountRow> rows = new ArrayList<>();
        // Open connection, prepare and run the query; all closed automatically.
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            // Read each row.
            while (rs.next()) {
                // Columns by position: 1 code, 2 name, 3 type, 4 unadjusted net, 5 adjustment net.
                rows.add(new AccountRow(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getBigDecimal(4), rs.getBigDecimal(5)));
            }
        }
        // Give the rows back.
        return rows;
    }
}
