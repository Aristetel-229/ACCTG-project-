package tala.dao;

import tala.database.DBConnection;
import tala.model.Account;
import tala.model.AccountType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * AccountDAO  (DAO = Data Access Object)
 * ------------------------------------------------------------
 * Every SQL statement for the "accounts" table lives here.
 * The screens never write SQL themselves; they call these methods.
 */
public class AccountDAO {

    /** All accounts, sorted by account code */
    public List<Account> getAllAccounts() throws SQLException {
        String sql = "SELECT account_id, account_code, account_name, account_type, normal_balance"
                   + " FROM accounts ORDER BY account_code";

        List<Account> accounts = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                accounts.add(readAccount(rs));
            }
        }
        return accounts;
    }

    /** Saves a new account and gives it the id MySQL created */
    public void addAccount(Account account) throws SQLException {
        String sql = "INSERT INTO accounts (account_code, account_name, account_type, normal_balance)"
                   + " VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, account.getCode());
            ps.setString(2, account.getName());
            ps.setString(3, account.getType().getLabel());
            ps.setString(4, account.getNormalBalance());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    account.setId(keys.getInt(1));
                }
            }
        }
    }

    /** Is this account code already in the chart? */
    public boolean isCodeTaken(int code) throws SQLException {
        return exists("SELECT 1 FROM accounts WHERE account_code = ?", String.valueOf(code));
    }

    /** Is this account name already in the chart? (not case-sensitive) */
    public boolean isNameTaken(String name) throws SQLException {
        return exists("SELECT 1 FROM accounts WHERE LOWER(account_name) = LOWER(?)", name.trim());
    }

    // ---------------------------------------------------------------- helpers

    private boolean exists(String sql, String value) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();   // true if at least one row was found
            }
        }
    }

    /** Turns the current ResultSet row into an Account object */
    static Account readAccount(ResultSet rs) throws SQLException {
        return new Account(
                rs.getInt("account_id"),
                rs.getInt("account_code"),
                rs.getString("account_name"),
                AccountType.fromLabel(rs.getString("account_type")),
                rs.getString("normal_balance"));
    }
}
