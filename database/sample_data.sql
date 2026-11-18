-- ============================================================
--  TALA Accounting System - OPTIONAL practice data
-- ============================================================
--  A small service business for November 2026:
--  11 regular entries + 4 adjusting entries.
--  Run this AFTER the app has started once (so the tables and
--  the chart of accounts exist). Run it only once.
--
--  Expected results after loading it:
--    Unadjusted trial balance total ..... P 163,000.00
--    Net income ......................... P  24,000.00
--    Ending capital ..................... P 120,000.00
--    Balance sheet difference ........... P       0.00
-- ============================================================

USE tala_accounting;

-- Each block: 1) insert the entry  2) remember its id  3) insert its lines

-- JE-000001  Owner invests cash
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-11-01', 'JE-000001', 'Owner invested cash in the business', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 100000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 3010), 0.00, 100000.00);

-- JE-000002  Office equipment bought on account
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-11-02', 'JE-000002', 'Bought office equipment on account', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1500), 30000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 2010), 0.00, 30000.00);

-- JE-000003  Supplies bought for cash
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-11-03', 'JE-000003', 'Bought office supplies for cash', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1300), 5000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 5000.00);

-- JE-000004  One year of insurance paid in advance
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-11-05', 'JE-000004', 'Paid one-year insurance premium', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1310), 12000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 12000.00);

-- JE-000005  Services rendered for cash
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-11-10', 'JE-000005', 'Rendered services for cash', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 25000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 4010), 0.00, 25000.00);

-- JE-000006  Services rendered on account
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-11-15', 'JE-000006', 'Rendered services on account', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1100), 18000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 4010), 0.00, 18000.00);

-- JE-000007  Salaries paid
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-11-20', 'JE-000007', 'Paid employee salaries', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5010), 8000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 8000.00);

-- JE-000008  Rent paid
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-11-25', 'JE-000008', 'Paid office rent for November', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5020), 6000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 6000.00);

-- JE-000009  Owner withdrew cash
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-11-28', 'JE-000009', 'Owner withdrew cash for personal use', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 3020), 4000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 4000.00);

-- JE-000010  Collected part of receivables
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-11-29', 'JE-000010', 'Collected from customers on account', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 10000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1100), 0.00, 10000.00);

-- JE-000011  Paid part of payables
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-11-30', 'JE-000011', 'Paid part of accounts payable', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 2010), 10000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 10000.00);

-- ---------------- ADJUSTING ENTRIES (Nov. 30) ----------------

-- AJE-000001  Supplies used
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-11-30', 'AJE-000001', 'Supplies used during the month', 'Adjusting');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5040), 2000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1300), 0.00, 2000.00);

-- AJE-000002  One month of insurance expired (12,000 / 12)
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-11-30', 'AJE-000002', 'Insurance expired for November', 'Adjusting');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5050), 1000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1310), 0.00, 1000.00);

-- AJE-000003  Depreciation of office equipment
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-11-30', 'AJE-000003', 'Depreciation of office equipment', 'Adjusting');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5060), 500.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1590), 0.00, 500.00);

-- AJE-000004  Salaries earned but not yet paid
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-11-30', 'AJE-000004', 'Accrued salaries at month end', 'Adjusting');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5010), 1500.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 2020), 0.00, 1500.00);
