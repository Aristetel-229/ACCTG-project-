package tala.model;

import java.math.BigDecimal;

/**
 * WorksheetRow
 * ------------------------------------------------------------
 * One row of the 12 Columnar worksheet.
 * It has 12 money cells, in this order (use the constants below):
 *
 *   0-1   Unadjusted trial balance   Dr | Cr
 *   2-3   Adjustments                Dr | Cr
 *   4-5   Adjusted trial balance     Dr | Cr
 *   6-7   Income statement           Dr | Cr
 *   8-9   Owner's equity             Dr | Cr
 *   10-11 Balance sheet              Dr | Cr
 *
 * A cell that is null is shown empty.
 */
public class WorksheetRow {

    public static final int UNADJ_DR = 0, UNADJ_CR = 1;
    public static final int ADJ_DR = 2,   ADJ_CR = 3;
    public static final int ADJTB_DR = 4, ADJTB_CR = 5;
    public static final int IS_DR = 6,    IS_CR = 7;
    public static final int OE_DR = 8,    OE_CR = 9;
    public static final int BS_DR = 10,   BS_CR = 11;
    public static final int CELL_COUNT = 12;

    /** What kind of row this is (decides the styling in the table) */
    public static final String ACCOUNT = "account";
    public static final String TOTAL = "total";
    public static final String NET_INCOME = "netIncome";
    public static final String CAPITAL = "capital";
    public static final String GRAND_TOTAL = "grandTotal";

    private String code;
    private String name;
    private String kind;
    private BigDecimal[] cells = new BigDecimal[CELL_COUNT];

    public WorksheetRow(String code, String name, String kind) {
        this.code = code;
        this.name = name;
        this.kind = kind;
    }

    public String getCode()      { return code; }
    public String getName()      { return name; }
    public String getKind()      { return kind; }
    public BigDecimal[] getCells() { return cells; }

    public BigDecimal get(int column) {
        return cells[column];
    }

    public void set(int column, BigDecimal amount) {
        cells[column] = amount;
    }
}
