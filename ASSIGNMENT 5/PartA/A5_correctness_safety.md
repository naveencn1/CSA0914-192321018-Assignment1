# Task A5 – Correctness and Safety

## 1. Transaction pseudocode: transfer ₹500 from hostel wallet to canteen wallet
Table `wallet(account_id, balance)`; the hostel account and canteen account are rows.
```
ALGORITHM TransferMoney
INPUT   connection C, fromAccount, toAccount, amount = 500
OUTPUT  SUCCESS or FAILURE (database unchanged on FAILURE)
BEGIN
 1  oldMode <- C.autoCommit
 2  SET C.autoCommit <- FALSE                         // start one transaction
 3  TRY
 4      rows1 <- EXECUTE "UPDATE wallet SET balance = balance - ? WHERE account_id = ? AND balance >= ?"
 5               WITH (amount, fromAccount, amount)    // PreparedStatement
 6      IF rows1 <> 1 THEN THROW "insufficient balance or unknown account"
 7      rows2 <- EXECUTE "UPDATE wallet SET balance = balance + ? WHERE account_id = ?"
 8               WITH (amount, toAccount)
 9      IF rows2 <> 1 THEN THROW "unknown canteen account"
10      COMMIT C                                       // both updates become permanent together
11      RETURN SUCCESS
12  CATCH any error E
13      ROLLBACK C                                     // both updates undone: no money lost or created
14      RETURN FAILURE
15  FINALLY
16      SET C.autoCommit <- oldMode
END
```
The debit's `balance >= ?` condition makes the check and the change one atomic statement (no race between "check balance" and "debit").

## 2. The unsafe login query (assumed form of the supplied code)
```
sql <- "SELECT * FROM users WHERE username = '" + u + "' AND password = '" + p + "'"
rs  <- EXECUTE sql ; RETURN rs HAS NEXT ROW
```
## 3. How crafted input bypasses it
User input is pasted into the SQL *text*, so a quote character ends the string literal and the rest of the input is parsed as SQL.

| Input | SQL sent to the server | Effect |
|-------|------------------------|--------|
| password = `x' OR '1'='1` | `... username='naveen' AND password='x' OR '1'='1'` | `AND` binds tighter than `OR`; `'1'='1'` is always true → **every row matches** |
| username = `admin' -- ` | `... username='admin' -- ' AND password='anything'` | `--` comments out the password test → login as admin **without a password** |
| username = `nosuchuser' OR 1=1 -- ` | `... WHERE username='nosuchuser' OR 1=1 -- ...` | accepted even for a user that does not exist |
(All three were executed and accepted by the unsafe version in Task02; see `Task02/out/task02_login_test.txt`.)

## 4. Redesign with `PreparedStatement`
```
ALGORITHM SafeLogin
INPUT   connection C, username u, password p
OUTPUT  TRUE if credentials match, else FALSE
BEGIN
 1  ps <- PREPARE "SELECT 1 FROM users WHERE username = ? AND password = ?"   // SQL fixed in advance
 2  BIND parameter 1 <- u                                                     // data, never SQL
 3  BIND parameter 2 <- p
 4  rs <- EXECUTE_QUERY(ps)
 5  RETURN (rs HAS NEXT ROW)
END
```
The statement is parsed *before* the values arrive; the values are sent separately as data, so a quote in `u` is just a character. (Production systems additionally store a salted hash, e.g. bcrypt, instead of the clear-text password used in this demo table.)

## 5. One transaction vs two independent auto-commit updates

| | One transaction (commit/rollback) | Two auto-commit updates |
|---|---|---|
| Failure after debit | rollback → balances unchanged | debit is already permanent; credit never happens → **₹500 lost** |
| Failure before debit | nothing happens | nothing happens |
| Failure of credit (e.g. bad account) | debit undone | debit stays → money lost |
| Other users see | only the state before or after (isolation) | the in-between state (money "in the air") |
| Atomicity | yes (all or nothing) | no |
| Cost | slightly longer lock time; **faster** for many rows (Task03: 100,000 inserts 12.4 s auto-commit vs 1.6 s one transaction) | one disk-sync per statement |
Evidence (Task03 test): a failure halfway left **499 rows** in auto-commit mode but **0 rows** after the rolled-back transaction.
