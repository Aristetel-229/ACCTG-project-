package tala.dao;

import tala.database.DBConnection;
import tala.model.AccountingPeriod;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * SettingsDAO
 * ------------------------------------------------------------
 * Small app settings saved in the "app_settings" table as key = value,
 * so they are remembered the next time the program starts.
 *
 * Used for the accounting period:
 *   period_from = 2026-11
 *   period_to   = 2026-12
 */
public class SettingsDAO {

    private static final String PERIOD_FROM = "period_from";
    private static final String PERIOD_TO = "period_to";

    /** One setting's value, or null if it was never saved */
    public String getSetting(String key) throws SQLException {
        String sql = "SELECT setting_value FROM app_settings WHERE setting_key = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** Saves a setting: inserts it the first time, updates it after that */
    public void saveSetting(String key, String value) throws SQLException {
        String sql = "INSERT INTO app_settings (setting_key, setting_value) VALUES (?, ?)"
                   + " ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }

    /** The saved accounting period, or this month only if none was saved yet */
    public AccountingPeriod getAccountingPeriod() throws SQLException {
        String from = getSetting(PERIOD_FROM);
        String to = getSetting(PERIOD_TO);
        try {
            if (from != null && to != null) {
                return new AccountingPeriod(YearMonth.parse(from), YearMonth.parse(to));
            }
        } catch (DateTimeParseException | IllegalArgumentException e) {
            // a broken value in the table: fall back to this month
        }
        return AccountingPeriod.ofMonth(YearMonth.now());
    }

    public void saveAccountingPeriod(AccountingPeriod period) throws SQLException {
        saveSetting(PERIOD_FROM, period.getFrom().toString());   // "2026-11"
        saveSetting(PERIOD_TO, period.getTo().toString());
    }
}
