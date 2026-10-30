// This class lives in the service package.
package com.tala.service;

// Our helper that opens database connections.
import com.tala.db.DB;

// Imports: BigDecimal for exact money, JDBC types for the query, LocalDate for dates, and collections for lists/maps.
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds the general ledger: one separate ledger per account that has posted activity. */
public final class LedgerService {
    // Private constructor: only static methods are used.
    private LedgerService() {}

    /** One posted debit/credit line of an account. */
    // A record is a compact immutable class; each posting holds a date, reference number, description, and debit/credit amounts.
    public record Posting(LocalDate date, String reference, String description, BigDecimal debit, BigDecimal credit) {}

    /** The ledger of a single account: its postings, running balances and totals. */
    public static final class AccountLedger {
        // Account code from the Chart of Accounts.
        public final String code;
        // Account name.
        public final String name;
        public final String type;          // ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
        public final boolean debitNormal;  // normal balance is DEBIT
        // All posted lines of this account, in order (the ledger rows).
        public final List<Posting> postings = new ArrayList<>();
        /** Running balance after each posting as debits - credits (positive = debit balance). */
        public final List<BigDecimal> running = new ArrayList<>();
        // Sum of all debits so far (starts at zero).
        public BigDecimal totalDebit = BigDecimal.ZERO;
        // Sum of all credits so far (starts at zero).
        public BigDecimal totalCredit = BigDecimal.ZERO;

        // Package-private constructor: only LedgerService creates ledgers.
        AccountLedger(String code, String name, String type, boolean debitNormal) {
            // Store the account details.
            this.code = code;
            this.name = name;
            this.type = type;
            this.debitNormal = debitNormal;
        }

        // Adds one posting to this account's ledger and updates totals and running balance.
        void add(Posting p) {
            // Keep the posting as a ledger row.
            postings.add(p);
            // Add its debit to the debit total.
            totalDebit = totalDebit.add(p.debit());
            // Add its credit to the credit total.
            totalCredit = totalCredit.add(p.credit());
            // Running balance = total debits so far minus total credits so far.
            running.add(totalDebit.subtract(totalCredit));
        }

        /** Ending balance as debits - credits (positive = debit balance, negative = credit balance). */
        public BigDecimal endingNet() {
            // Final balance of the account: debits minus credits.
            return totalDebit.subtract(totalCredit);
        }
    }

    /**
     * Pure grouping step, kept separate from the database so it can be tested on its own.
     * Each row is {code, name, type, debitNormal (Boolean), Posting}; rows of one account are
     * expected to arrive in posting order.
     */
    public static List<AccountLedger> build(List<Object[]> rows) {
        // Map from account code to its ledger; LinkedHashMap keeps the original (sorted) order.
        Map<String, AccountLedger> byCode = new LinkedHashMap<>();
        // Go through every row from the database.
        for (Object[] r : rows) {
            // Find the ledger for this account code, or create it the first time we see the code.
            AccountLedger l = byCode.computeIfAbsent((String) r[0],
                    k -> new AccountLedger((String) r[0], (String) r[1], (String) r[2], (Boolean) r[3]));
            // Post this line into that account's ledger.
            l.add((Posting) r[4]);
        }
        // Return all account ledgers as a list.
        return new ArrayList<>(byCode.values());
    }

    /** Loads every POSTED line, grouped by account. Accounts without activity are left out. */
    public static List<AccountLedger> load() throws SQLException {
        // SQL: join each journal line to its entry (date, reference, description) and its account;
        // only POSTED entries count; sorted by account code, then date, then entry id, then line id so postings are in order.
        String sql = """
            SELECT a.account_code, a.account_name, a.account_type, a.normal_balance,
                   je.entry_date, je.reference_no, je.description, jel.debit, jel.credit
            FROM journal_entry_lines jel
            JOIN journal_entries je ON je.id = jel.journal_entry_id
            JOIN accounts a ON a.id = jel.account_id
            WHERE je.status = 'POSTED'
            ORDER BY a.account_code, je.entry_date, je.id, jel.id
            """;
        // Raw rows collected before grouping.
        List<Object[]> rows = new ArrayList<>();
        // Open connection, prepare the query (no parameters to bind) and run it; all are closed automatically.
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            // Loop over each result row.
            while (rs.next()) {
                // Build a Posting from the date, reference, description, debit and credit columns.
                Posting p = new Posting(rs.getDate("entry_date").toLocalDate(),
                        rs.getString("reference_no"), rs.getString("description"),
                        rs.getBigDecimal("debit"), rs.getBigDecimal("credit"));
                // Save the account info plus the posting; normal_balance is turned into true when it equals "DEBIT".
                rows.add(new Object[]{rs.getString("account_code"), rs.getString("account_name"),
                        rs.getString("account_type"), "DEBIT".equals(rs.getString("normal_balance")), p});
            }
        }
        // Group the rows into one ledger per account.
        return build(rows);
    }
}
