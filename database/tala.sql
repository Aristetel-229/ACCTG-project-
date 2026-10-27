-- =====================================================================
-- TALA database setup script.
-- Run this ONCE in MySQL Workbench before starting the app.
-- It is safe to run again: it uses IF NOT EXISTS / INSERT IGNORE,
-- so existing data is not duplicated or deleted.
-- =====================================================================

-- Create the database named "tala" if it does not exist yet.
-- utf8mb4 = full Unicode support (needed to store the peso sign and any language).
-- utf8mb4_unicode_ci = case-insensitive text comparison/sorting.
CREATE DATABASE IF NOT EXISTS tala
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Switch to the tala database: all tables below are created inside it.
USE tala;

-- ---------------------------------------------------------------------
-- TABLE 1: accounts  (the Chart of Accounts)
-- One row per account, e.g. 1010 Cash on Hand.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS accounts (
    -- Unique number for each row, generated automatically (1, 2, 3, ...). Other tables point to it.
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    -- The account code such as '1010'. UNIQUE = no two accounts may share a code. NOT NULL = required.
    account_code VARCHAR(20) NOT NULL UNIQUE,
    -- The account name such as 'Cash on Hand' (max 120 characters).
    account_name VARCHAR(120) NOT NULL,
    -- The account category. ENUM = the value MUST be one of these five words.
    account_type ENUM('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE') NOT NULL,
    -- Which side increases the account: DEBIT or CREDIT (e.g. Cash = DEBIT, Accounts Payable = CREDIT).
    normal_balance ENUM('DEBIT','CREDIT') NOT NULL,
    -- Optional short explanation of the account (may be empty/NULL).
    description VARCHAR(255),
    -- TRUE = the account can be used; FALSE = hidden from dropdowns and reports. Defaults to TRUE.
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    -- Date and time the row was created; filled in automatically.
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------
-- TABLE 2: journal_entries  (the HEADER of each transaction)
-- One row per transaction. The debit/credit amounts live in journal_entry_lines.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS journal_entries (
    -- Automatic unique id of the entry.
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    -- The date of the transaction (date only, no time).
    entry_date DATE NOT NULL,
    -- Reference number such as 'JE-123456' or 'ADJ-123456'. UNIQUE = cannot be reused (a duplicate makes posting fail).
    -- It may be NULL because NULL is allowed unless NOT NULL is written.
    reference_no VARCHAR(40) UNIQUE,
    -- What the transaction was about (required).
    description VARCHAR(255) NOT NULL,
    -- POSTED = counts in all reports; REVERSED = ignored by the reports. New entries default to POSTED.
    status ENUM('POSTED','REVERSED') NOT NULL DEFAULT 'POSTED',
    -- When the row was created; filled in automatically.
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------
-- TABLE 3: journal_entry_lines  (the DEBIT and CREDIT lines)
-- Each journal entry has at least two lines: one debit and one credit.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS journal_entry_lines (
    -- Automatic unique id of the line.
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    -- Which journal entry this line belongs to (links to journal_entries.id).
    journal_entry_id BIGINT NOT NULL,
    -- Which account is affected (links to accounts.id).
    account_id BIGINT NOT NULL,
    -- Debit amount: up to 15 digits with 2 decimals. DECIMAL is exact (never use FLOAT for money). Default 0.00.
    debit DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    -- Credit amount, same format. Default 0.00.
    credit DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    -- Optional description for this specific line.
    line_description VARCHAR(255),
    -- FOREIGN KEY: a line's journal_entry_id must match an existing entry id.
    -- It also stops you from deleting an entry that still has lines (that's why the reset deletes lines first).
    CONSTRAINT fk_line_entry FOREIGN KEY (journal_entry_id)
        REFERENCES journal_entries(id),
    -- FOREIGN KEY: a line's account_id must match an existing account id.
    CONSTRAINT fk_line_account FOREIGN KEY (account_id)
        REFERENCES accounts(id),
    -- CHECK: debit and credit can never be negative.
    CONSTRAINT chk_nonnegative_amounts CHECK (debit >= 0 AND credit >= 0),
    -- CHECK: each line is EITHER a debit (credit = 0) OR a credit (debit = 0), never both and never both zero.
    CONSTRAINT chk_one_side CHECK (
        (debit > 0 AND credit = 0) OR (credit > 0 AND debit = 0)
    ),
    -- INDEX: makes searches by account fast (used by the ledger and reports).
    INDEX idx_lines_account (account_id),
    -- INDEX on journal_entry_id (the name says "entries_date", but the column is journal_entry_id): speeds up finding the lines of an entry.
    INDEX idx_entries_date (journal_entry_id)
);

-- ---------------------------------------------------------------------
-- STARTER DATA: the initial Chart of Accounts.
-- ---------------------------------------------------------------------
-- Starter Chart of Accounts. Safe to re-run because account_code is unique.
-- INSERT IGNORE = if an account code already exists, skip that row instead of raising an error.
-- Column order of each row: code, name, type, normal balance, description.
INSERT IGNORE INTO accounts (account_code, account_name, account_type, normal_balance, description) VALUES
-- 1xxx = ASSETS (things the business owns). Normal balance is DEBIT.
('1010','Cash on Hand','ASSET','DEBIT','Cash held by the business'),
('1020','Cash in Bank','ASSET','DEBIT','Business bank account'),
('1100','Accounts Receivable','ASSET','DEBIT','Amounts owed by customers'),
('1200','Inventory','ASSET','DEBIT','Inventory held for sale'),
('1500','Office Equipment','ASSET','DEBIT','Office equipment and computers'),
-- Accumulated Depreciation is a "contra-asset": it is an asset account with a CREDIT normal balance because it reduces the equipment's value.
('1590','Accumulated Depreciation','ASSET','CREDIT','Accumulated depreciation contra-asset'),
-- 2xxx = LIABILITIES (what the business owes). Normal balance is CREDIT.
('2010','Accounts Payable','LIABILITY','CREDIT','Amounts owed to suppliers'),
('2020','Salaries Payable','LIABILITY','CREDIT','Unpaid salary obligations'),
('2030','Taxes Payable','LIABILITY','CREDIT','Taxes payable'),
('2100','Loans Payable','LIABILITY','CREDIT','Outstanding loans'),
-- 3xxx = EQUITY (the owner's stake). Capital is CREDIT; Drawings (owner withdrawals) reduce equity, so it is DEBIT.
-- Double quotes are used for the names because they contain an apostrophe (Owner's).
('3010',"Owner's Capital",'EQUITY','CREDIT','Owner capital contributions'),
('3020',"Owner's Drawings",'EQUITY','DEBIT','Owner withdrawals'),
-- 4xxx = REVENUE (money earned). Normal balance is CREDIT.
('4010','Sales Revenue','REVENUE','CREDIT','Revenue from sales'),
('4020','Service Revenue','REVENUE','CREDIT','Revenue from services'),
('4090','Other Income','REVENUE','CREDIT','Other business income'),
-- 5xxx and 6xxx = EXPENSES (money spent). Normal balance is DEBIT.
('5010','Cost of Goods Sold','EXPENSE','DEBIT','Cost of goods sold'),
('6010','Rent Expense','EXPENSE','DEBIT','Rent and lease expenses'),
('6020','Salaries Expense','EXPENSE','DEBIT','Employee salaries'),
('6030','Utilities Expense','EXPENSE','DEBIT','Electricity, water, and internet'),
('6040','Supplies Expense','EXPENSE','DEBIT','Office and operating supplies'),
('6050','Transportation Expense','EXPENSE','DEBIT','Transportation expenses');
