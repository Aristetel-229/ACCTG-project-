package tala.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Worksheet
 * ------------------------------------------------------------
 * The finished 12 Columnar worksheet: all its rows plus the
 * key numbers shown in the tiles above it.
 */
public class Worksheet {

    private List<WorksheetRow> rows = new ArrayList<>();
    private BigDecimal unadjustedTotal = BigDecimal.ZERO;   // trial balance total
    private BigDecimal adjustmentsTotal = BigDecimal.ZERO;  // total of the Adjustments Dr column
    private int adjustingEntryCount;
    private BigDecimal netIncome = BigDecimal.ZERO;         // negative = net loss
    private BigDecimal endingCapital = BigDecimal.ZERO;
    private BigDecimal balanceSheetDifference = BigDecimal.ZERO;
    private boolean trialBalanceBalanced = true;

    public List<WorksheetRow> getRows()              { return rows; }
    public BigDecimal getUnadjustedTotal()           { return unadjustedTotal; }
    public BigDecimal getAdjustmentsTotal()          { return adjustmentsTotal; }
    public int getAdjustingEntryCount()              { return adjustingEntryCount; }
    public BigDecimal getNetIncome()                 { return netIncome; }
    public BigDecimal getEndingCapital()             { return endingCapital; }
    public BigDecimal getBalanceSheetDifference()    { return balanceSheetDifference; }
    public boolean isTrialBalanceBalanced()          { return trialBalanceBalanced; }

    public void setUnadjustedTotal(BigDecimal v)        { unadjustedTotal = v; }
    public void setAdjustmentsTotal(BigDecimal v)       { adjustmentsTotal = v; }
    public void setAdjustingEntryCount(int v)           { adjustingEntryCount = v; }
    public void setNetIncome(BigDecimal v)              { netIncome = v; }
    public void setEndingCapital(BigDecimal v)          { endingCapital = v; }
    public void setBalanceSheetDifference(BigDecimal v) { balanceSheetDifference = v; }
    public void setTrialBalanceBalanced(boolean v)      { trialBalanceBalanced = v; }

    /** True when the trial balance balances AND the balance sheet difference is zero */
    public boolean isBalanced() {
        return trialBalanceBalanced && balanceSheetDifference.compareTo(BigDecimal.ZERO) == 0;
    }

    /** True when no account has been used yet (the worksheet starts empty) */
    public boolean isEmpty() {
        return rows.isEmpty();
    }
}
