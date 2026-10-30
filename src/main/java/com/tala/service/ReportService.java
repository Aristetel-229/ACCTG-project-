// This class lives in the service package.
package com.tala.service;

// Imports: BigDecimal for exact money and JDBC types for database access.
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Balance calculations used by the dashboard. */
public final class ReportService {
    // Private constructor: only static methods are used.
    private ReportService() {}

    /** Balance of an account type as a positive presentation amount (credit-normal types are negated). */
    public static BigDecimal accountTypeBalance(Connection c, String type) throws SQLException {
        // SQL: sum (debit - credit) of POSTED lines for all active accounts of one type (the ? is the type).
        // LEFT JOINs keep accounts with no lines; COALESCE turns a NULL sum into 0.
        String sql = """
            SELECT COALESCE(SUM(CASE WHEN je.status = 'POSTED' THEN jel.debit - jel.credit ELSE 0 END), 0) AS balance
            FROM accounts a
            LEFT JOIN journal_entry_lines jel ON jel.account_id = a.id
            LEFT JOIN journal_entries je ON je.id = jel.journal_entry_id
            WHERE a.account_type = ? AND a.is_active = TRUE
            """;
        // Prepare the statement on the connection given by the caller (we do not close that connection).
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            // Bind parameter 1 (the ?) to the account type, e.g. "ASSET".
            ps.setString(1, type);
            // Run the query and read its single result row.
            try (ResultSet rs = ps.executeQuery()) {
                // Move to the first (only) row.
                rs.next();
                // Net = debits minus credits for this account type.
                BigDecimal net = rs.getBigDecimal("balance");
                // Safety: treat a missing value as zero.
                if (net == null) net = BigDecimal.ZERO;
                // Liability, equity and revenue normally have credit balances, so flip the sign to show a positive number.
                if ("LIABILITY".equals(type) || "EQUITY".equals(type) || "REVENUE".equals(type)) net = net.negate();
                // Return the balance to display.
                return net;
            }
        }
    }

    // Counts how many journal entries have been posted (for the dashboard).
    public static int countPostedEntries(Connection c) throws SQLException {
        // Prepare and run a COUNT query (no parameters); both are closed automatically.
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM journal_entries WHERE status = 'POSTED'");
             ResultSet rs = ps.executeQuery()) {
            // Move to the single result row.
            rs.next();
            // Column 1 holds the count.
            return rs.getInt(1);
        }
    }
}
