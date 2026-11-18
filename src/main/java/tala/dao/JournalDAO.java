package tala.dao;

import tala.database.DBConnection;
import tala.model.Account;
import tala.model.JournalEntry;
import tala.model.JournalLine;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JournalDAO
 * ------------------------------------------------------------
 * Every SQL statement for journal_entries and journal_lines.
 * Posting an entry uses a TRANSACTION: either the entry and all
 * its lines are saved, or nothing is saved (rollback).
 */
public class JournalDAO {

    /**
     * All entries with their lines, oldest first.
     * One query joins the three tables; we then group the rows by entry.
     */
    public List<JournalEntry> getAllEntries() throws SQLException {
        String sql = "SELECT e.entry_id, e.entry_date, e.reference_no, e.description, e.entry_type,"
                   + "       l.debit, l.credit,"
                   + "       a.account_id, a.account_code, a.account_name, a.account_type, a.normal_balance"
                   + " FROM journal_entries e"
                   + " JOIN journal_lines l ON l.entry_id = e.entry_id"
                   + " JOIN accounts a ON a.account_id = l.account_id"
                   + " ORDER BY e.entry_date, e.entry_id, (l.credit > 0), l.line_id";  // debit lines first

        // LinkedHashMap keeps the entries in the same order as the query
        Map<Integer, JournalEntry> entriesById = new LinkedHashMap<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int entryId = rs.getInt("entry_id");

                // First time we see this entry? Create it.
                JournalEntry entry = entriesById.get(entryId);
                if (entry == null) {
                    entry = new JournalEntry(
                            entryId,
                            rs.getDate("entry_date").toLocalDate(),
                            rs.getString("reference_no"),
                            rs.getString("description"),
                            rs.getString("entry_type"));
                    entriesById.put(entryId, entry);
                }

                // Every row is one line of that entry
                Account account = AccountDAO.readAccount(rs);
                entry.addLine(new JournalLine(account, rs.getBigDecimal("debit"), rs.getBigDecimal("credit")));
            }
        }
        return new ArrayList<>(entriesById.values());
    }

    /**
     * Saves a new entry and its lines in ONE transaction.
     * Refuses an entry whose debits do not equal its credits.
     */
    public void postEntry(JournalEntry entry) throws SQLException {
        if (!entry.isBalanced()) {
            throw new IllegalArgumentException("Total debits must equal total credits.");
        }

        String entrySql = "INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)"
                        + " VALUES (?, ?, ?, ?)";
        String lineSql = "INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES (?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);          // --- START the transaction

            // 1) the entry itself
            int newEntryId;
            try (PreparedStatement ps = conn.prepareStatement(entrySql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setDate(1, Date.valueOf(entry.getDate()));
                ps.setString(2, entry.getReferenceNo());
                ps.setString(3, entry.getDescription());
                ps.setString(4, entry.getEntryType());
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    newEntryId = keys.getInt(1);
                }
            }

            // 2) its debit and credit lines
            try (PreparedStatement ps = conn.prepareStatement(lineSql)) {
                for (JournalLine line : entry.getLines()) {
                    ps.setInt(1, newEntryId);
                    ps.setInt(2, line.getAccount().getId());
                    ps.setBigDecimal(3, line.getDebit());
                    ps.setBigDecimal(4, line.getCredit());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();                      // --- SAVE everything
            entry.setId(newEntryId);

        } catch (SQLException e) {
            rollbackQuietly(conn);              // --- UNDO everything
            throw e;
        } finally {
            closeQuietly(conn);
        }
    }

    /** Deletes one entry. Its lines are removed too (ON DELETE CASCADE). */
    public void deleteEntry(int entryId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM journal_entries WHERE entry_id = ?")) {
            ps.setInt(1, entryId);
            ps.executeUpdate();
        }
    }

    /** Deletes every entry (the chart of accounts stays). Used by "Reset data". */
    public void deleteAllEntries() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM journal_entries");
        }
    }

    /**
     * The next free reference number for a prefix.
     *   prefix "JE"  -> JE-000001, JE-000002, ...  (regular entries)
     *   prefix "AJE" -> AJE-000001, ...           (adjusting entries)
     */
    public String nextReferenceNo(String prefix) throws SQLException {
        String sql = "SELECT reference_no FROM journal_entries WHERE reference_no LIKE ?";
        int highest = 0;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix + "-%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String digits = rs.getString(1).substring(prefix.length() + 1);
                    try {
                        highest = Math.max(highest, Integer.parseInt(digits));
                    } catch (NumberFormatException ignored) {
                        // a reference typed some other way; just skip it
                    }
                }
            }
        }
        return String.format("%s-%06d", prefix, highest + 1);
    }

    // ---------------------------------------------------------------- helpers

    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
                // nothing more we can do; the original error is reported instead
            }
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
