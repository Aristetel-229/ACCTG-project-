# TALA — Accounting (Java Swing + MySQL)

Windows desktop accounting app. Java 17+, Swing, JDBC, MySQL.

## Pages
| Menu | Class | What it does |
|---|---|---|
| Dashboard | `DashboardPage` | Totals (assets, liabilities, revenue, expenses, net income), recent entries, and the **Reset Data** button |
| Journal Entry | `JournalEntryPage` | Post a balanced debit/credit entry, and **delete a single entry** from the Posted entries list (other data is untouched) |
| General Journal | `GeneralJournalPage` | Every posted entry in date order (Date, Particulars, Post Ref, Debit, Credit) with explanation lines and a TOTAL row. Only accounts you used appear |
| Chart of Accounts | `ChartOfAccountsPage` | List of all accounts |
| General Ledger | `GeneralLedgerPage` | A separate ledger for each account you used, two per row: transactions with running balance (DR/CR) and a **Total / ending balance** row. An Account drop-down shows just one |
| 12 Columnar | `TwelveColumnWorksheetPage` | Unadjusted TB, Adjustments, Adjusted TB, Income Statement, Owner's Equity, Balance Sheet (6 Dr/Cr pairs = 12 columns). Starts empty and lists only accounts with posted entries. Grand Total and Balance Difference rows should read ₱0.00 |

## Requirements
JDK 17+, Maven 3.9+, MySQL 8.0+

## 1. Database
Run `database/tala.sql` in MySQL Workbench. It creates the `tala` database and the starter Chart of Accounts.

## 2. Connection
Set these environment variables, or edit the defaults in `src/main/java/com/tala/db/DB.java`:
`TALA_DB_URL`, `TALA_DB_USER`, `TALA_DB_PASSWORD`

## 3. Run
Double-click `Run-TALA.bat`, or run `mvn clean compile exec:java`.

## Structure
```
src/main/java/com/tala/
├── Main.java
├── db/DB.java
├── model/AccountOption.java
├── service/  JournalService, LedgerService, ReportService, ResetDataService, WorksheetService
├── util/Money.java
└── ui/  MainFrame, BasePage, Theme (green palette), components/, pages/
```

## Reset Data
The Dashboard's **Reset Data** button deletes all journal entries and adjustments (after two confirmations, including typing RESET). The Chart of Accounts is kept.
