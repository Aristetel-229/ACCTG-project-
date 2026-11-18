# TALA Accounting System (Student Edition)

A double-entry accounting desktop app.
**Main program:** Java (Swing) - **Database:** MySQL

Screens (top navigation, purple and yellow). Pictures with the practice data are in `docs/screenshots/`.

| Page | What it does |
|---|---|
| Dashboard | Total assets, liabilities, revenue, expenses, net income, entry count, latest entries, accounting-equation check, quick actions, tip. Point at a tile: it lifts up and a tooltip shows how the number was worked out. |
| Journal Entry | Choose the **accounting period** (From month - To month), then post a transaction (debit account, credit account, amount, date, description) with a live T-account preview and checklist. A date outside the period is refused. Delete one entry at a time. |
| General Journal | Date / Particulars / Post ref / Debit / Credit, credits indented, explanation line under each entry, totals, search and month filter |
| Chart of Accounts | All accounts, filter chips by type, search, add a new account (code rules checked) |
| General Ledger | Every used account with its balance; per-account ledger with running balance, totals, ending balance, and an "Abnormal balance" warning |
| 12 Columnar | Unadjusted TB, Adjustments, Adjusted TB, Income statement, Owner's equity, Balance sheet (Dr/Cr each). Starts empty, lists only used accounts, last row shows the balance sheet difference (0.00). Add adjustment pop-up, CSV export. The four tiles on top are hoverable too. |

---

## 1. What you need

1. **JDK 11 or newer** (JDK 17 or 21 is fine)
2. **MySQL** (XAMPP's MySQL/MariaDB works) - start it before running the app
3. **MySQL Connector/J** jar (the JDBC driver), see `lib/PUT-MYSQL-CONNECTOR-JAR-HERE.txt`

## 2. Connection settings (no db.properties)

The settings are written **in Java**, in one class:

`src/main/java/tala/database/DBConnection.java`

```java
public static final String HOST          = "localhost";
public static final int    PORT          = 3306;
public static final String DATABASE_NAME = "tala_accounting";
public static final String USERNAME      = "root";
public static final String PASSWORD      = "";   // XAMPP default
```

Change them if your MySQL uses another password or port.

## 3. Run it

The **first run creates the database, the tables and the chart of accounts by itself**
(`DatabaseSetup.java`). You do not have to import the SQL file.

**Option A - double-click `run.bat`** (Windows)
Put the connector jar in `lib/` first. The script compiles and starts the app.
(`run.sh` does the same on macOS/Linux.)

**Option B - NetBeans / IntelliJ as a Maven project**
File > Open Project > choose this folder (it has `pom.xml`). Maven downloads the
connector. Run `tala.Main`.

**Option C - NetBeans "Java with Ant" project**
1. New Project > Java with Ant > Java Application (uncheck "Create Main Class").
2. Copy the `tala` folder from `src/main/java/` **and** the `images` folder from
   `src/main/resources/` into the project's `src` folder (the logo lives in `images`).
3. Right-click Libraries > Add JAR/Folder > the connector jar.
4. Right-click `tala/Main.java` > Run File.

**Practice data (optional):** after the app has started once, run
`database/sample_data.sql` in MySQL Workbench / phpMyAdmin. It adds 11 entries and 4
adjustments for November 2026. You should then see:

| Check | Expected |
|---|---|
| Unadjusted trial balance | P163,000.00 = P163,000.00 |
| Net income | P24,000.00 |
| Ending capital | P120,000.00 |
| Total assets = Liabilities + Owner's equity | P141,500.00 = P21,500.00 + P120,000.00 |
| Balance sheet difference | P0.00 |

## 4. How the code is organized

Read the packages in this order:

```
src/main/java/tala/
|-- Main.java                 START HERE: prepares the database, opens the window
|-- database/
|   |-- DBConnection.java     MySQL settings + getConnection()
|   `-- DatabaseSetup.java    creates the database/tables, adds the starting accounts
|-- model/                    plain data classes (no SQL, no screens)
|   |-- Account, AccountType  one account of the chart (1010 Cash, Asset, Debit)
|   |-- AccountingPeriod      From month - To month; is a date inside it?
|   |-- JournalEntry, JournalLine  a transaction and its debit/credit lines
|   |-- LedgerRow             one line of a General Ledger
|   |-- WorksheetRow, Worksheet    the 12 Columnar
|   `-- DashboardSummary      the Dashboard totals
|-- dao/                      ALL the SQL lives here
|   |-- AccountDAO.java       read / add accounts
|   |-- JournalDAO.java       read / post (transaction + rollback) / delete entries
|   `-- SettingsDAO.java      remembers the accounting period (app_settings table)
|-- service/
|   `-- AccountingService.java  ALL the accounting math (balances, ledger, worksheet)
|-- util/
|   |-- Money.java            BigDecimal peso formatting and parsing
|   `-- DateUtil.java         YYYY-MM-DD in, "Nov 3, 2026" out
`-- ui/
    |-- MainFrame.java        the window: top bar + pages (CardLayout)
    |-- theme/                colors and fonts (change the look here)
    |-- components/           reusable pieces: cards, buttons, tables, badges, icons
    `-- pages/                one class per screen + AddAdjustmentDialog
```

The rule: **screens never write SQL** (they call a DAO) and **DAOs never do
accounting math** (that is `AccountingService`). Each layer has one job.

## 5. The accounting rules used

* Every entry is saved in two tables: `journal_entries` (date, reference, description, type)
  and `journal_lines` (one row per debit or credit). Posting uses a **transaction**:
  if any line fails, nothing is saved (`rollback`).
* An entry is accepted only if **total debits = total credits**, the amount is above zero, and
  its date is inside the **accounting period** (first day of the From month to the last day of
  the To month). The period is saved in the `app_settings` table, so it is remembered next time.
* Money is always `BigDecimal` in Java and `DECIMAL(15,2)` in MySQL, never `double`.
* Balances:
  * Debit-normal accounts (Assets, Expenses, Owner's Drawing): **debits - credits**
  * Credit-normal accounts (Liabilities, Owner's Capital, Revenue): **credits - debits**
  * Contra accounts (Allowance for Doubtful Accounts, Accumulated Depreciation) are Assets with a
    **Credit** normal balance, so they reduce total assets.
  * A negative result is an **abnormal balance** (e.g. Cash with a credit balance) and is flagged.
* Net income = Revenue - Expenses. Owner's equity = Capital - Drawing + Net income.
* 12 Columnar:
  1. Unadjusted TB = balances from regular entries
  2. Adjustments = debits/credits of adjusting entries (reference `AJE-...`)
  3. Adjusted TB = unadjusted + adjustment debits - adjustment credits
  4. Revenue/Expense go to the Income statement, Capital/Drawing to Owner's equity,
     Assets/Liabilities to the Balance sheet
  5. Net income closes the Income statement and is added to Owner's equity
  6. Ending capital closes Owner's equity and goes to the Balance sheet credit side
  7. The last row shows the balance sheet **difference**, which is 0.00 when the books balance

## 6. Account code guide

| Codes | Type | Normal balance |
|---|---|---|
| 1000-1999 | Asset | Debit (contra assets: Credit) |
| 2000-2999 | Liability | Credit |
| 3000-3999 | Equity | Credit (Drawing: Debit) |
| 4000-4999 | Revenue | Credit |
| 5000-5999 | Expense | Debit |

## 7. The logo

The TALA logo is `src/main/resources/images/logo.png`. It is used in two places:

* the purple top bar (in a rounded square), drawn by `ui/components/TopNavBar.java`
* the window's title bar and the Windows taskbar, set in `ui/MainFrame.java`
  with `setIconImages(AppImages.getWindowIcons())`

To change it, replace that file with another **square** PNG (512 x 512 is ideal).
`ui/theme/AppImages.java` loads it and makes the small sizes. If the file is missing,
the app draws a yellow "T" instead.

## 8. Common problems

| Message | Fix |
|---|---|
| "MySQL Connector/J was not found" | The connector jar is not in `lib/` (or not added to the IDE project). |
| "Access denied for user 'root'" | Set the right `PASSWORD` in `DBConnection.java`. |
| "Communications link failure" | MySQL is not running (XAMPP: press Start on MySQL) or the `PORT` is wrong. |
| The peso sign shows as a box | Your font has no peso sign; the app then shows "PHP" automatically. |
