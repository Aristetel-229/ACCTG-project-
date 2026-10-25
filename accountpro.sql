CREATE DATABASE IF NOT EXISTS accountpro
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE accountpro;

CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_code VARCHAR(20) NOT NULL UNIQUE,
    account_name VARCHAR(120) NOT NULL,
    account_type ENUM('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE') NOT NULL,
    normal_balance ENUM('DEBIT','CREDIT') NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS journal_entries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    entry_date DATE NOT NULL,
    reference_no VARCHAR(40) UNIQUE,
    description VARCHAR(255) NOT NULL,
    status ENUM('POSTED','REVERSED') NOT NULL DEFAULT 'POSTED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS journal_entry_lines (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    journal_entry_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    debit DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    credit DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    line_description VARCHAR(255),
    CONSTRAINT fk_line_entry FOREIGN KEY (journal_entry_id)
        REFERENCES journal_entries(id),
    CONSTRAINT fk_line_account FOREIGN KEY (account_id)
        REFERENCES accounts(id),
    CONSTRAINT chk_nonnegative_amounts CHECK (debit >= 0 AND credit >= 0),
    CONSTRAINT chk_one_side CHECK (
        (debit > 0 AND credit = 0) OR (credit > 0 AND debit = 0)
    ),
    INDEX idx_lines_account (account_id),
    INDEX idx_entries_date (journal_entry_id)
);

-- Starter Chart of Accounts. Safe to re-run because account_code is unique.
INSERT IGNORE INTO accounts (account_code, account_name, account_type, normal_balance, description) VALUES
('1010','Cash on Hand','ASSET','DEBIT','Cash held by the business'),
('1020','Cash in Bank','ASSET','DEBIT','Business bank account'),
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

-- Simple invoice and payment register for the starter desktop app.
CREATE TABLE IF NOT EXISTS invoices (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    invoice_no VARCHAR(40) NOT NULL UNIQUE,
    customer_name VARCHAR(160) NOT NULL,
    invoice_date DATE NOT NULL,
    description VARCHAR(255) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    amount_paid DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status ENUM('UNPAID','PARTIAL','PAID') NOT NULL DEFAULT 'UNPAID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_invoice_total CHECK (total_amount > 0),
    CONSTRAINT chk_invoice_paid CHECK (amount_paid >= 0 AND amount_paid <= total_amount)
);

CREATE TABLE IF NOT EXISTS invoice_payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    invoice_no VARCHAR(40) NOT NULL,
    payment_date DATE NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoice_payment FOREIGN KEY (invoice_no) REFERENCES invoices(invoice_no),
    CONSTRAINT chk_invoice_payment_amount CHECK (amount > 0),
    INDEX idx_invoice_payments_date (payment_date)
);
