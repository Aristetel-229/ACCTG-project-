package tala.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DashboardSummary
 * ------------------------------------------------------------
 * The totals shown on the Dashboard tiles.
 *
 *   Net income      = Revenue - Expenses
 *   Owner's equity  = Capital - Drawing + Net income
 *   Check:  Assets  = Liabilities + Owner's equity
 */
public class DashboardSummary {

    public BigDecimal totalAssets = BigDecimal.ZERO;
    public BigDecimal totalLiabilities = BigDecimal.ZERO;
    public BigDecimal totalEquity = BigDecimal.ZERO;      // includes net income
    public BigDecimal totalRevenue = BigDecimal.ZERO;
    public BigDecimal totalExpenses = BigDecimal.ZERO;
    public BigDecimal netIncome = BigDecimal.ZERO;
    public int entryCount;

    /** Accounts whose balance is on the wrong side (e.g. Cash with a credit balance) */
    public List<Account> abnormalAccounts = new ArrayList<>();

    /** Assets = Liabilities + Owner's equity ? */
    public boolean isEquationBalanced() {
        return totalAssets.compareTo(totalLiabilities.add(totalEquity)) == 0;
    }
}
