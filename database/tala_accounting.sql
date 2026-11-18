-- ============================================================
--  TALA Accounting System - Database Script (MySQL / MariaDB)
-- ============================================================
--  You do NOT have to run this file by hand: the Java program
--  creates the database and tables by itself the first time it
--  starts (see DatabaseSetup.java).
--
--  Run it yourself only if you want to build the database in
--  MySQL Workbench / phpMyAdmin first, or to look at the design.
-- ============================================================

CREATE DATABASE IF NOT EXISTS tala_accounting;
USE tala_accounting;

-- ------------------------------------------------------------
-- 1) ACCOUNTS  (the Chart of Accounts)
--    account_code  : 4 digits, first digit tells the type
--                    1 Asset, 2 Liability, 3 Equity,
--                    4 Revenue, 5 Expense
--    normal_balance: the side that INCREASES the account
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS accounts (
    account_id      INT AUTO_INCREMENT PRIMARY KEY,
    account_code    INT          NOT NULL UNIQUE,
    account_name    VARCHAR(100) NOT NULL UNIQUE,
    account_type    ENUM('Asset','Liability','Equity','Revenue','Expense') NOT NULL,
    normal_balance  ENUM('Debit','Credit') NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- 2) JOURNAL_ENTRIES  (one row per transaction)
--    entry_type: 'Regular'   = normal journal entry
--                'Adjusting' = period-end adjustment (12 Columnar)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS journal_entries (
    entry_id      INT AUTO_INCREMENT PRIMARY KEY,
    entry_date    DATE         NOT NULL,
    reference_no  VARCHAR(20)  NOT NULL UNIQUE,
    description   VARCHAR(255) NOT NULL,
    entry_type    ENUM('Regular','Adjusting') NOT NULL DEFAULT 'Regular',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- 3) JOURNAL_LINES  (the debit and credit lines of an entry)
--    Every entry has at least one debit line and one credit
--    line, and total debits = total credits (checked in Java).
--    A line is EITHER a debit OR a credit, never both.
--    Deleting an entry deletes its lines (ON DELETE CASCADE).
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS journal_lines (
    line_id     INT AUTO_INCREMENT PRIMARY KEY,
    entry_id    INT           NOT NULL,
    account_id  INT           NOT NULL,
    debit       DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    credit      DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_line_entry   FOREIGN KEY (entry_id)   REFERENCES journal_entries(entry_id) ON DELETE CASCADE,
    CONSTRAINT fk_line_account FOREIGN KEY (account_id) REFERENCES accounts(account_id),
    CONSTRAINT chk_line_amount CHECK (debit >= 0 AND credit >= 0
                                      AND (debit = 0 OR credit = 0)
                                      AND (debit > 0 OR credit > 0))
);

-- ------------------------------------------------------------
-- 4) APP_SETTINGS  (small settings remembered between runs)
--    period_from / period_to = the accounting period on the
--    Journal Entry page, saved as 'YYYY-MM' (e.g. '2026-11')
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_settings (
    setting_key    VARCHAR(50)  PRIMARY KEY,
    setting_value  VARCHAR(255) NOT NULL
);

-- ------------------------------------------------------------
-- 5) STARTING CHART OF ACCOUNTS
--    INSERT IGNORE skips an account that is already there.
--    Contra accounts (Allowance, Accumulated Depreciation)
--    are Assets with a CREDIT normal balance.
--    Owner's Drawing is Equity with a DEBIT normal balance.
-- ------------------------------------------------------------
INSERT IGNORE INTO accounts (account_code, account_name, account_type, normal_balance) VALUES
(1010, 'Cash',                            'Asset',     'Debit'),
(1100, 'Accounts Receivable',             'Asset',     'Debit'),
(1110, 'Allowance for Doubtful Accounts', 'Asset',     'Credit'),
(1120, 'Notes Receivable',                'Asset',     'Debit'),
(1130, 'Interest Receivable',             'Asset',     'Debit'),
(1200, 'Inventory',                       'Asset',     'Debit'),
(1300, 'Supplies',                        'Asset',     'Debit'),
(1310, 'Prepaid Insurance',               'Asset',     'Debit'),
(1320, 'Prepaid Rent',                    'Asset',     'Debit'),
(1330, 'Advances to Employees',           'Asset',     'Debit'),
(1500, 'Office Equipment',                'Asset',     'Debit'),
(1510, 'Furniture',                       'Asset',     'Debit'),
(1520, 'Vehicles',                        'Asset',     'Debit'),
(1590, 'Accumulated Depreciation',        'Asset',     'Credit'),
(1600, 'Land',                            'Asset',     'Debit'),
(1610, 'Building',                        'Asset',     'Debit'),
(2010, 'Accounts Payable',                'Liability', 'Credit'),
(2020, 'Salaries Payable',                'Liability', 'Credit'),
(2030, 'Taxes Payable',                   'Liability', 'Credit'),
(2040, 'Unearned Revenue',                'Liability', 'Credit'),
(2100, 'Notes Payable',                   'Liability', 'Credit'),
(3010, 'Owner''s Capital',                'Equity',    'Credit'),
(3020, 'Owner''s Drawing',                'Equity',    'Debit'),
(4010, 'Service Revenue',                 'Revenue',   'Credit'),
(4020, 'Interest Income',                 'Revenue',   'Credit'),
(5010, 'Salaries Expense',                'Expense',   'Debit'),
(5020, 'Rent Expense',                    'Expense',   'Debit'),
(5030, 'Utilities Expense',               'Expense',   'Debit'),
(5040, 'Supplies Expense',                'Expense',   'Debit'),
(5050, 'Insurance Expense',               'Expense',   'Debit'),
(5060, 'Depreciation Expense',            'Expense',   'Debit'),
(5070, 'Bad Debts Expense',               'Expense',   'Debit');
