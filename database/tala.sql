-- Create the 'tala' database only if it does not exist yet (safe to re-run).
-- utf8mb4 lets text store any character (including the peso sign); the collation sets case-insensitive sorting/comparison.
CREATE DATABASE IF NOT EXISTS tala
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Select the tala database so the statements below apply to it.
USE tala;

-- TABLE accounts = the Chart of Accounts: the list of every account the business can use.
CREATE TABLE IF NOT EXISTS accounts (
    -- Unique row number, generated automatically by MySQL; the primary key.
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    -- Short account number such as 1010 (Cash); must be present and unique so no account is duplicated.
    account_code VARCHAR(20) NOT NULL UNIQUE,
    -- Human-readable account name such as 'Cash'.
    account_name VARCHAR(120) NOT NULL,
    -- Category in the accounting equation: Assets = Liabilities + Equity; Revenue and Expense feed the income statement.
    account_type ENUM('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE') NOT NULL,
    -- Side where the account normally increases: DEBIT or CREDIT (e.g. assets/expenses = debit, liabilities/equity/revenue = credit).
    normal_balance ENUM('DEBIT','CREDIT') NOT NULL,
    -- Optional note explaining what the account is for.
    description VARCHAR(255),
    -- TRUE if the account can still be used in new entries; defaults to TRUE.
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    -- Time the row was created; MySQL fills it with the current time automatically.
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- TABLE journal_entries = the header of each journal entry (one row per transaction: date, reference, description).
CREATE TABLE IF NOT EXISTS journal_entries (
    -- Unique row number, generated automatically by MySQL; the primary key.
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    -- Date of the transaction (the date used for ordering in the journal and ledger).
    entry_date DATE NOT NULL,
    -- Optional unique voucher/reference number (adjusting entries use the prefix ADJ-).
    reference_no VARCHAR(40) UNIQUE,
    -- Explanation of the transaction (required).
    description VARCHAR(255) NOT NULL,
    -- POSTED = counted in the books; REVERSED = cancelled entry. Defaults to POSTED.
    status ENUM('POSTED','REVERSED') NOT NULL DEFAULT 'POSTED',
    -- Time the row was created; MySQL fills it with the current time automatically.
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- TABLE journal_entry_lines = the debit/credit lines of each journal entry.
-- In double-entry bookkeeping every entry has at least two lines and total debits must equal total credits.
CREATE TABLE IF NOT EXISTS journal_entry_lines (
    -- Unique row number, generated automatically by MySQL; the primary key.
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    -- Which journal entry this line belongs to (links to journal_entries.id).
    journal_entry_id BIGINT NOT NULL,
    -- Which account is debited or credited (links to accounts.id).
    account_id BIGINT NOT NULL,
    -- Debit amount in pesos with 2 decimals (exact decimal type avoids rounding errors that floats would cause).
    debit DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    -- Credit amount in pesos with 2 decimals; 0.00 when the line is a debit.
    credit DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    -- Optional note for this single line.
    line_description VARCHAR(255),
    -- Foreign key: a line must point to an existing journal entry (no orphan lines).
    CONSTRAINT fk_line_entry FOREIGN KEY (journal_entry_id)
        REFERENCES journal_entries(id),
    -- Foreign key: a line must point to an existing account.
    CONSTRAINT fk_line_account FOREIGN KEY (account_id)
        REFERENCES accounts(id),
    -- Rule: debit and credit can never be negative.
    CONSTRAINT chk_nonnegative_amounts CHECK (debit >= 0 AND credit >= 0),
    -- Rule: each line is either a debit OR a credit (one side positive, the other zero), never both.
    CONSTRAINT chk_one_side CHECK (
        (debit > 0 AND credit = 0) OR (credit > 0 AND debit = 0)
    ),
    -- Index to speed up looking up all lines of one account (used by the General Ledger).
    INDEX idx_lines_account (account_id),
    -- Index to speed up finding all lines of one journal entry.
    INDEX idx_entries_date (journal_entry_id)
);

-- Seed data, group 1: the basic starter accounts (assets, liabilities, equity, revenue, expenses).
-- INSERT IGNORE skips rows whose account_code already exists instead of raising an error.
-- Starter Chart of Accounts. Safe to re-run because account_code is unique.
INSERT IGNORE INTO accounts (account_code, account_name, account_type, normal_balance, description) VALUES
('1010','Cash','ASSET','DEBIT','Cash on hand and in bank'),
('1100','Accounts Receivable','ASSET','DEBIT','Amounts owed by customers'),
('1200','Inventory','ASSET','DEBIT','Inventory held for sale'),
('1500','Office Equipment','ASSET','DEBIT','Office equipment and computers'),
('1590','Accumulated Depreciation','ASSET','CREDIT','Accumulated depreciation contra-asset'),
('2010','Accounts Payable','LIABILITY','CREDIT','Amounts owed to suppliers'),
('2020','Salaries Payable','LIABILITY','CREDIT','Unpaid salary obligations'),
('2030','Taxes Payable','LIABILITY','CREDIT','Taxes payable'),
('2100','Loans Payable','LIABILITY','CREDIT','Outstanding loans'),
('3010',"Owner's Capital",'EQUITY','CREDIT','Owner capital contributions'),
('3020',"Owner's Drawings",'EQUITY','DEBIT','Owner withdrawals'),
('4010','Sales Revenue','REVENUE','CREDIT','Revenue from sales'),
('4020','Service Revenue','REVENUE','CREDIT','Revenue from services'),
('4090','Other Income','REVENUE','CREDIT','Other business income'),
('5010','Cost of Goods Sold','EXPENSE','DEBIT','Cost of goods sold'),
('6010','Rent Expense','EXPENSE','DEBIT','Rent and lease expenses'),
('6020','Salaries Expense','EXPENSE','DEBIT','Employee salaries'),
('6030','Utilities Expense','EXPENSE','DEBIT','Electricity, water, and internet'),
('6040','Supplies Expense','EXPENSE','DEBIT','Office and operating supplies'),
('6050','Transportation Expense','EXPENSE','DEBIT','Transportation expenses');

-- Seed data, group 2: extra common accounts (supplies, prepaid items, furniture, vehicles, unearned revenue, more expenses).
INSERT IGNORE INTO accounts (account_code, account_name, account_type, normal_balance, description) VALUES
-- ASSETS
('1300','Supplies','ASSET','DEBIT','Supplies on hand not yet used'),
('1310','Prepaid Insurance','ASSET','DEBIT','Insurance paid in advance'),
('1320','Prepaid Rent','ASSET','DEBIT','Rent paid in advance'),
('1510','Furniture','ASSET','DEBIT','Furniture and fixtures'),
('1520','Vehicles','ASSET','DEBIT','Company vehicles'),
-- LIABILITIES
('2040','Unearned Revenue','LIABILITY','CREDIT','Payments received before service is performed'),
('2110','Notes Payable','LIABILITY','CREDIT','Written promissory notes owed'),
-- EXPENSES
('6060','Advertising Expense','EXPENSE','DEBIT','Advertising and promotion costs'),
('6070','Maintenance Expense','EXPENSE','DEBIT','Maintenance costs'),
('6080','Professional Fees Expense','EXPENSE','DEBIT','Fees for professional services'),
('6090','Repairs Expense','EXPENSE','DEBIT','Repairs costs'),
('6100','Interest Expense','EXPENSE','DEBIT','Interest on loans and notes'),
('6110','Insurance Expense','EXPENSE','DEBIT','Insurance used up during the period'),
('6120','Depreciation Expense','EXPENSE','DEBIT','Depreciation of fixed assets');

-- NEWLY ADDED (previously missing)
-- Seed data, group 3: more accounts for fuller coverage (contra accounts, receivables, taxes, closing accounts, other expenses).
INSERT IGNORE INTO accounts (account_code, account_name, account_type, normal_balance, description) VALUES
-- ASSETS
('1110','Allowance for Doubtful Accounts','ASSET','CREDIT','Contra-asset for estimated uncollectible receivables'),
('1120','Notes Receivable','ASSET','DEBIT','Written promissory notes owed to the business'),
('1130','Interest Receivable','ASSET','DEBIT','Interest earned but not yet received'),
('1330','Advances to Employees','ASSET','DEBIT','Cash advances given to employees'),
('1600','Land','ASSET','DEBIT','Land owned by the business'),
('1610','Building','ASSET','DEBIT','Buildings owned by the business'),
-- LIABILITIES
('2050','Interest Payable','LIABILITY','CREDIT','Interest incurred but not yet paid'),
('2060','Withholding Tax Payable','LIABILITY','CREDIT','Taxes withheld from employees and suppliers'),
('2070','VAT Payable','LIABILITY','CREDIT','Output VAT net of input VAT'),
('2080','SSS/PhilHealth/Pag-IBIG Payable','LIABILITY','CREDIT','Government contributions withheld and employer share'),
-- EQUITY
('3030','Retained Earnings','EQUITY','CREDIT','Accumulated profits retained in the business'),
('3040','Income Summary','EQUITY','CREDIT','Temporary account used during closing entries'),
-- REVENUE
('4030','Interest Income','REVENUE','CREDIT','Interest earned on bank deposits and notes'),
('4110','Sales Returns and Allowances','REVENUE','DEBIT','Contra-revenue for returned or discounted sales'),
('4120','Sales Discounts','REVENUE','DEBIT','Contra-revenue for early payment discounts'),
-- EXPENSES
('6130','Bad Debts Expense','EXPENSE','DEBIT','Uncollectible accounts expense'),
('6140','Taxes and Licenses Expense','EXPENSE','DEBIT','Business permits, licenses, and local taxes'),
('6150','Communication Expense','EXPENSE','DEBIT','Phone, mobile load, and postage'),
('6160','Bank Charges Expense','EXPENSE','DEBIT','Bank fees and service charges'),
('6170','Employee Benefits Expense','EXPENSE','DEBIT','13th month pay, SSS/PhilHealth/Pag-IBIG employer share'),
('6180','Representation Expense','EXPENSE','DEBIT','Meals and client entertainment'),
('6190','Miscellaneous Expense','EXPENSE','DEBIT','Minor expenses not classified elsewhere'),
('7010','Income Tax Expense','EXPENSE','DEBIT','Income tax for the period');
