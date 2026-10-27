// Package for services (database + business logic, no GUI).
package com.tala.service;

// Exact decimal numbers for money.
import java.math.BigDecimal;
// Database classes used to run the query.
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Balance calculations used by the dashboard. */
public final class ReportService {
    // Static-only class: never instantiated.
    private ReportService() {}

    /** Balance of an account type as a positive presentation amount (credit-normal types are negated). */
    // Adds up all debits minus credits for every account of one type (ASSET, LIABILITY, ...).
    // The connection "c" is passed in so the dashboard can reuse ONE connection for all its queries.
    public static BigDecimal accountTypeBalance(Connection c, String type) throws SQLException {
        // The SQL is written in a Java text block (""" ... """) so it can span many lines.
        // - SUM(CASE ...): adds (debit - credit) of each line, but ONLY for entries with status POSTED
        //   (reversed entries count as 0).
        // - COALESCE(..., 0): if there are no lines at all, SUM is NULL, so use 0 instead.
        // - LEFT JOIN: keep the account even if it has no journal lines yet.
        // - WHERE: only accounts of the requested type (the ?) that are active.
        String sql = """
            SELECT COALESCE(SUM(CASE WHEN je.status = 'POSTED' THEN jel.debit - jel.credit ELSE 0 END), 0) AS balance
            FROM accounts a
            LEFT JOIN journal_entry_lines jel ON jel.account_id = a.id
            LEFT JOIN journal_entries je ON je.id = jel.journal_entry_id
            WHERE a.account_type = ? AND a.is_active = TRUE
            """;
        // Prepare the statement (closed automatically at the end of the try block).
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            // Replace the ? with the requested account type, e.g. "ASSET".
            ps.setString(1, type);
            // Run the query.
            try (ResultSet rs = ps.executeQuery()) {
                // The query always returns exactly one row, so move to it.
                rs.next();
                // This is the raw "debit minus credit" total.
                BigDecimal net = rs.getBigDecimal("balance");
                // Safety check: treat NULL as zero.
                if (net == null) net = BigDecimal.ZERO;
                // Liabilities, equity and revenue normally have CREDIT balances (negative here).
                // Flip the sign so they display as positive numbers on the dashboard.
                if ("LIABILITY".equals(type) || "EQUITY".equals(type) || "REVENUE".equals(type)) net = net.negate();
                return net;
            }
        }
    }

    // Counts how many journal entries are currently POSTED (shown on the "POSTED ENTRIES" card).
    public static int countPostedEntries(Connection c) throws SQLException {
        // Two resources in one try: the statement and the result set; both close automatically.
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM journal_entries WHERE status = 'POSTED'");
             ResultSet rs = ps.executeQuery()) {
            // Move to the single result row.
            rs.next();
            // Column 1 holds the count.
            return rs.getInt(1);
        }
    }
}
