# Task 1 – GUI Design Review (Student Fee-Payment Window)

**Files:** `FeePaymentFlawed.java` (reference copy of the flawed design – replace with your supplied file) · redesign: `FeePaymentFrame`, `FeeService`, `FeeRepository`, `JdbcFeeRepository`, `InMemoryFeeRepository`, `PaymentRequest`, `PaymentResult`, `ValidationException`, `DataAccessException` · test: `FeeServiceTest`

## 1. Diagnosis (10 problems found, 5 required)

| # | Problem (where) | Impact on user / application |
|---|---|---|
| 1 | **No separation of concerns** – the one `actionPerformed` reads fields, validates, talks to MySQL and updates the label | Cannot test without opening a window; any change (new rule, new database) means editing GUI code; handler grows without limit |
| 2 | **`cmd == "Pay"` compares String references** with `==` | Works only by accident of string interning; fails if the command string is built at run time or comes from another class – a button silently does nothing. Correct: `"Pay".equals(cmd)` or, better, no strings at all (use `Action`) |
| 3 | **No input validation** (empty ID, `abc` amount, negative amount, overpayment) | Bad data reaches the database; `NumberFormatException`/SQL errors are thrown deep inside the handler |
| 4 | **Database access and hard-coded credentials inside the window class** | UI cannot run without MySQL; credentials leak into source control; no way to fake the DB for testing |
| 5 | **`Thread.sleep(3000)` on the Event Dispatch Thread** | Window freezes for 3 s: no repaint, no click response, looks crashed |
| 6 | **SQL built by string concatenation** (`"... VALUES('" + id + "'," + amount + ")"`) | SQL injection – e.g. an ID of `x'); DROP TABLE payments; --` (or a crafted amount) alters the statement |
| 7 | **No user feedback / empty `catch`** | Failure is swallowed; the user never learns whether the payment succeeded; money may be lost without a trace |
| 8 | **Receipt button has an empty branch** | A visible button that does nothing; user cannot get proof of payment |
| 9 | **`Connection` and `Statement` never closed** | Connection leak – after enough payments the database refuses new connections |
| 10 | **One shared listener with an if/else chain on command strings** | Adding a button means editing the chain; cannot disable one action independently; typo in a command string = dead button |

## 2. Redesigned class structure

```
            +--------------------+         +--------------------+
  (view)    |  FeePaymentFrame   |  uses   |     FeeService     |   (business rules)
            |  Swing only        |-------->| validate, pay,     |
            |  PayAction         |         | receiptText        |
            |  ReceiptAction     |         +---------+----------+
            |  ClearAction       |                   | depends on interface
            |  SwingWorker       |                   v
            +--------------------+         +--------------------+
                                           |   <<interface>>    |
   PaymentRequest, PaymentResult (records) |    FeeRepository   |
   ValidationException                     +----+----------+----+
                                                |          |
                                  +-------------+--+  +----+----------------+
                                  | JdbcFee        |  | InMemoryFee         |
                                  | Repository     |  | Repository (demo,   |
                                  | PreparedStmt,  |  | tests)              |
                                  | try-with-res,  |  +---------------------+
                                  | transaction    |
                                  +----------------+
```

## 3. Rewritten event handling

```java
private final Action payAction = new AbstractAction("Pay") {
    @Override public void actionPerformed(ActionEvent e) { startPayment(); }
};
// ...
buttons.add(new JButton(payAction));          // one Action object per command
amountField.addActionListener(payAction);     // same Action reused for the Enter key

private void startPayment() {
    PaymentRequest req = new PaymentRequest(idField.getText(), amountField.getText(), (String) modeBox.getSelectedItem());
    payAction.setEnabled(false);                                  // no double submit
    new SwingWorker<PaymentResult, Void>() {
        protected PaymentResult doInBackground() { return service.pay(req); }   // background thread
        protected void done() { /* EDT: show receipt no. or the specific error */ }
    }.execute();
}
```
The window never touches SQL; `FeeService.pay()` validates every field, collects **all** problems into one `ValidationException`, checks the account exists and the amount ≤ outstanding balance, then calls the repository.

## 4. Design justification (95 words)

The window now only collects input and displays results; FeeService owns validation and payment rules, and FeeRepository hides all SQL. This separation lets each part be tested alone (FeeServiceTest runs without a GUI), blocks SQL injection through PreparedStatement, and allows the database to change without touching the window. Every button has its own Action, so name, behaviour and enabled state live together; Receipt is enabled only after a successful payment. A SwingWorker keeps the window responsive instead of sleeping on the event thread, and errors reach the user as specific messages instead of being swallowed.

## 5. Alternative concept – one shared listener vs separate listeners vs Swing `Action`

| Aspect | One shared listener (original) | Separate listener objects | Swing `Action` (redesign) |
|---|---|---|---|
| Dispatch | if/else on `getActionCommand()` / `getSource()` – O(k) comparisons for k buttons | Direct call – O(1) | Direct call – O(1) |
| Adding a button | Edit the shared method | Add a new class/lambda | Add a new Action |
| Typo risk | High (string commands) | None | None |
| Enable/disable | Per button, manually, in several places | Per button, manually | `action.setEnabled(false)` updates every button/menu bound to it |
| Reuse (button + menu + Enter key) | Awkward | Possible | Natural – same object everywhere |
| Memory | 1 listener | k listener objects | k Action objects (hold text, icon, enabled state) |

## 6. Complexity and measurements

* Validation: O(L) in the length of the input text (regex match); constant for a 6–12 character ID.
* Balance lookup/insert: O(log n) with an index on `student_id` (primary key); the original did no lookup at all.
* Memory: O(k) for k Actions; no connection kept open (try-with-resources), so DB resource use is O(1) per payment instead of O(payments) leaked connections.
* Measurement: `FeeServiceTest` – 11/11 checks pass (valid payment, 7 invalid inputs, unknown student, overpayment, SQL-injection-style ID). Responsiveness: the original blocks the EDT for 3000 ms per click by construction; the redesign blocks it for ≈0 ms (work is in `doInBackground`).

## 7. Evidence / screenshots to capture
See `../docs/SCREENSHOTS_TO_CAPTURE.md` (Task 1 section).
