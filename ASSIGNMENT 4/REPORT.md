# Programming in Java (CSA09) – Assignment 4 (CO4)
## Event-Driven GUI Design – Assignment Report

| | |
|---|---|
| Name | V V Naveen Kumar |
| Register number | 192321018 |
| GitHub repository | `<https://github.com/<user>/CSA09-192321018-Assignment4>` (shared with faculty) |
| Java version | JDK 21 |

**Contents:** Task 1 GUI Design Review · Task 2 Requirement to Event-Driven Design · Task 3 From Sketch to Layout · Task 4 Event-Handling Forensics · Task 5 Same Screen, Different Event Styles · Task 6 Design Challenge · Submission checklist

> Evidence rule: each screenshot is followed by one sentence stating which requirement/test it demonstrates (list in `docs/SCREENSHOTS_TO_CAPTURE.md`).

---

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

---

# Task 2 – Requirement to Event-Driven Design (Hospital Outpatient Appointment Desk)

**Requirement:** the desk operator chooses a department; the doctor list refreshes; enters patient name, phone and date; presses *Book*; the system validates and books the appointment, then shows a confirmation. Window opening/closing must be handled.

## 1. Event table

| # | Event source (component) | Event object | Listener interface | Action performed |
|---|---|---|---|---|
| 1 | `departmentChoice` (Choice / JComboBox) | `ItemEvent` (`getStateChange()==SELECTED`) | `ItemListener` → `itemStateChanged` | Clear doctor list, load doctors of the chosen department, enable the doctor list |
| 2 | `doctorChoice` (Choice / JComboBox) | `ItemEvent` | `ItemListener` | Remember chosen doctor; clear any old error message |
| 3 | `patientNameField` (TextField) | `TextEvent` / `DocumentEvent` (optional) | `TextListener` / `DocumentListener` | Live-clear the error label while typing |
| 4 | `phoneField` (TextField) – Enter key | `ActionEvent` | `ActionListener` → `actionPerformed` | Move focus to the date field (or trigger Book) |
| 5 | `bookButton` (Button) | `ActionEvent` | `ActionListener` | Validate the form, check slot, save appointment, show confirmation |
| 6 | `clearButton` (Button) | `ActionEvent` | `ActionListener` | Reset all fields and the status label |
| 7 | `exitMenuItem` (MenuItem) | `ActionEvent` | `ActionListener` | Same as pressing the window close button (confirm, dispose) |
| 8 | `Frame` (window) opened | `WindowEvent` | `WindowListener.windowOpened` | Load the department list; set focus on the first field |
| 9 | `Frame` close button | `WindowEvent` | `WindowListener.windowClosing` (via `WindowAdapter`) | Ask "Discard unsaved booking?"; if yes `dispose()` |
| 10 | `Frame` after dispose | `WindowEvent` | `WindowListener.windowClosed` | Release resources (close cache/connection), `System.exit(0)` |
| 11 | `Frame` minimise / restore | `WindowEvent` | `windowIconified` / `windowDeiconified` | Nothing required (listed to show they exist) |
| 12 | `Frame` focus change | `WindowEvent` | `windowActivated` / `windowDeactivated` | Optional: refresh slot availability on activation |

## 2. Component containment tree (AWT/Swing names)

```
Frame  appointmentFrame                      (BorderLayout)
 |-- MenuBar menuBar
 |     `-- Menu fileMenu --- MenuItem exitMenuItem
 |-- Panel headerPanel                        [NORTH]   (FlowLayout)
 |     `-- Label titleLabel  "Outpatient Appointment Desk"
 |-- Panel formPanel                          [CENTER]  (GridLayout 5 rows x 2 cols)
 |     |-- Label deptLabel        Choice  departmentChoice
 |     |-- Label doctorLabel      Choice  doctorChoice
 |     |-- Label nameLabel        TextField patientNameField
 |     |-- Label phoneLabel       TextField phoneField
 |     `-- Label dateLabel        TextField dateField   (dd-MM-yyyy)
 `-- Panel footerPanel                        [SOUTH]   (BorderLayout)
       |-- Panel buttonPanel                  [CENTER]  (FlowLayout)
       |     |-- Button bookButton
       |     `-- Button clearButton
       `-- Label statusLabel                  [SOUTH]
```

## 3. Pseudocode – refresh the doctor list when the department changes

```
itemStateChanged(ItemEvent e):
    if e.getStateChange() != SELECTED: return          // ignore the DESELECTED event of the old item
    dept <- departmentChoice.getSelectedItem()
    if dept is the placeholder "-- Select --":
        doctorChoice.removeAll(); doctorChoice.setEnabled(false); return
    doctors <- doctorCache.get(dept)                   // Map<String, List<Doctor>>, loaded once (see section 7)
    if doctors == null:                                // not cached yet
        doctors <- doctorService.findByDepartment(dept)   // DB call, ideally in background
        doctorCache.put(dept, doctors)
    doctorChoice.removeAll()
    for each d in doctors: doctorChoice.add(d.name)
    doctorChoice.setEnabled(true)
    statusLabel.setText(doctors.size() + " doctor(s) available")
```

## 4. Pseudocode – validate and book an appointment

```
actionPerformed(ActionEvent e)            // source = bookButton
    errors <- empty list
    name  <- trim(patientNameField.text)
    phone <- trim(phoneField.text)
    date  <- parseDate(dateField.text)          // null if the format is wrong
    if department not chosen:               add "Select a department"
    if doctor not chosen:                   add "Select a doctor"
    if name is empty or not letters:        add "Enter a valid patient name"
    if phone does not match [0-9]{10}:      add "Phone must be 10 digits"
    if date is null:                        add "Date must be dd-MM-yyyy"
    else if date < today:                   add "Date cannot be in the past"
    if errors not empty:
        statusLabel.setText(join(errors)); return          // nothing saved
    if appointmentService.slotTaken(doctor, date):
        statusLabel.setText("Doctor is not available on that date"); return
    id <- appointmentService.book(doctor, name, phone, date)   // saves, returns appointment no.
    statusLabel.setText("Booked. Appointment No " + id)
    clear the form
```

## 5. Method-call sequence – opening to closing the window

```
main()
 +- EventQueue.invokeLater(...)                    -> run on the Event Dispatch Thread
     +- new AppointmentFrame()                     constructor: create components, add listeners
     |    +- setLayout / add(panels) / setMenuBar
     |    +- addWindowListener(new WindowAdapter(){...})
     +- frame.setSize(..) ; frame.setVisible(true)
          +- addNotify()  (peer created)  ->  paint(g) / update(g)       first drawing
          +- WindowEvent: windowOpened(e)            load departments, set focus
          +- WindowEvent: windowActivated(e)
 ... user works: ItemEvent, ActionEvent, TextEvent, KeyEvent ... (listener methods above)
 ... user minimises/restores: windowIconified(e) -> windowDeiconified(e) -> paint(g) again
 user clicks [X]:
     WindowEvent: windowClosing(e)                 our handler asks for confirmation
         +- frame.dispose()
              WindowEvent: windowDeactivated(e)
              WindowEvent: windowClosed(e)         release resources
                   +- System.exit(0)
```

## 6. Window sequence vs applet lifecycle

| Applet method | Called when | Closest Frame/Swing equivalent |
|---|---|---|
| `init()` | Once, when the applet is loaded | The constructor (create components, register listeners) |
| `start()` | After init and each time the page becomes visible again | `setVisible(true)` / `windowOpened` / `windowDeiconified` |
| `paint(Graphics)` | Whenever the area must be redrawn | `paint(Graphics)` / `paintComponent` – identical idea |
| `stop()` | Page hidden / browser leaves page | `windowIconified` / `windowDeactivated` |
| `destroy()` | Applet unloaded | `windowClosing` + `dispose()` → `windowClosed` |

Differences: an applet's lifecycle is driven by the browser (the programmer overrides methods); a Frame's lifecycle is driven by the program and the window system (the programmer calls `setVisible`, `dispose`) and window changes arrive as `WindowEvent`s. Applets are obsolete (removed from modern browsers and deprecated since Java 9), so Frame/JFrame is used for new desktop work.

## 7. Alternative concept – load doctors from the database every time vs load once into a Map

| Aspect | Query DB on every department change | Load once into `Map<String, List<Doctor>>` |
|---|---|---|
| Time per change | One network round trip + query (typically milliseconds on a LAN, more over a WAN); O(log n) with an index, plus transfer O(d) for d doctors | Hash lookup, O(1) average |
| Memory | O(d) temporary, freed after use | O(D) permanently for all D doctors (tiny for a hospital – hundreds of rows) |
| Freshness | Always current | Can be stale (doctor added/left); needs a Refresh button, a TTL, or reload on `windowActivated` |
| Failure | DB outage breaks every department change | Only the first load can fail; later changes still work |
| Load on DB | One query per click | One query per session |
| Complexity of code | Simple | Needs invalidation logic |

Choice: **load once into a Map** (doctor lists change rarely), do the first load in a background thread, and add a *Refresh* menu item. Bookings are **never** cached – slot availability must always be checked against the database.

---

# Task 3 – From Sketch to Layout (Campus Library Search Screen)

**Files:** `LibrarySearchFrame.java` (screen), `LibraryCatalog.java` (data + search + paging), `LibraryCatalogTest.java` (8/8 checks pass)

## 1. Sketch

```
+--------------------------------------------------------------+
|                 Campus Library Search          <- title      |
| Search: [______________] [Category v] [Search]  <- search row |
|--------------------------------------------------------------|
|  ID | Title              | Author       | Category | Status   |
|  1  | Java: The Complete | H. Schildt   | CS       | Available|   <- results (grows)
|  ...                                                         |
|--------------------------------------------------------------|
|              [ Prev ]  [ Next ]  [ Reserve ]   <- buttons     |
| Page 1 of 4 (20 results)                       <- status      |
+--------------------------------------------------------------+
```

## 2. Nested panel structure and layout managers

```
JFrame contentPane ........................... BorderLayout(8,8)
 |-- NORTH  headerPanel ...................... BorderLayout
 |      |-- NORTH  titleLabel
 |      `-- CENTER searchRow ................. FlowLayout(LEFT)
 |             JLabel, JTextField(20), JComboBox, JButton "Search"
 |-- CENTER JScrollPane(JTable) ............. (resizes with the window)
 `-- SOUTH  footerPanel ...................... BorderLayout
        |-- CENTER buttonRow ................. FlowLayout(CENTER)
        |      JButton Prev, Next, Reserve
        `-- SOUTH  statusLabel
```

## 3. Layout choice and justification

| Container | Layout | Why chosen | Why not the others |
|---|---|---|---|
| Content pane | **BorderLayout** | Five regions match the sketch exactly; CENTER absorbs all extra space, NORTH/SOUTH keep their preferred height | FlowLayout would wrap components; GridLayout would force equal-sized cells (table as small as the buttons); CardLayout shows only one child at a time |
| Search row | **FlowLayout (LEFT)** | Components keep their preferred size and sit in a row; wraps only on a very narrow window (min size prevents it) | GridLayout would stretch the Search button to the text field's width |
| Button row | **FlowLayout (CENTER)** | Three buttons at natural size, centred, with gaps | GridLayout would stretch buttons across the whole width |
| Header / footer wrappers | **BorderLayout** | Stack title above search row, buttons above status | – |
| (Not used) CardLayout | – | Would be right if the screen had several "pages" (search / details / reservation) swapped in one area | – |

This gives the required behaviour: the **results area grows** in both directions; the **search row and bottom buttons keep their intended size** and stay attached to the top and bottom edges.

## 4. Test at three window sizes

| Size | What should be visible | Expected behaviour |
|---|---|---|
| Small – 560 × 360 (minimum) | All search-row components on one line; table with scroll bars; 3 buttons; status | Table area shrinks, scroll bar appears; header/footer unchanged |
| Medium – 800 × 520 (default) | Complete table, comfortable spacing | Reference layout |
| Large – 1200 × 800 (maximised) | Table much larger, columns wider | Extra space goes only to the table; buttons stay centred at natural size |

`setMinimumSize(560 x 360)` stops the window being shrunk until the search row would wrap. Screenshots to take: see `../docs/SCREENSHOTS_TO_CAPTURE.md` (Task 3).

## 5. Alternative concept – nested layouts vs GridBagLayout vs absolute positioning

| Aspect | Nested simple layouts (used) | GridBagLayout | Absolute positioning (`setLayout(null)`) |
|---|---|---|---|
| Code size | Moderate, but each piece is trivial | One panel, but verbose constraints (`gridx, gridy, weightx, fill, insets…`) | Short per component (`setBounds`) |
| Resizing | Correct automatically | Correct if weights are set right | **Does not adapt** – clipped or empty space |
| Fonts / look-and-feel / DPI changes | Adapts (preferred sizes) | Adapts | Breaks (fixed pixel sizes) |
| Ease of change | Move a panel | Edit constraints of several cells | Recalculate every coordinate |
| Learning curve | Low | High | Lowest, but fragile |
| Number of objects | More containers (≈ 5 panels) | Fewer containers, one `GridBagConstraints` | Fewest |

Time/space: layout is O(c) in the number of components c for a validate pass (nested layouts add small overhead per level; GridBag does extra work computing weights); memory O(c) in all three. For a screen with ~12 components the difference is negligible – maintainability and correct resizing decide, so nested layouts win.

---

# Task 4 – Event-Handling Forensics (Drawing Pad)

**Files:** `DrawingPadFlawed.java` (reference copy of the broken program – use your supplied file), `DrawingPadFixed.java` (repair), `PointsVsImageBenchmark.java` + `benchmark_output.txt` (measurements)

## 1. Why the click handler never runs

```java
addMouseListener(new MouseAdapter() {
    public void mouseClick(MouseEvent e) { ... }      // WRONG NAME
});
```
`MouseListener` declares `mouseClicked(MouseEvent)`. The programmer wrote `mouseClick`. Because `MouseAdapter` supplies empty bodies for every method, the class still compiles – `mouseClick` is just an extra, unrelated method that Swing never calls. The real `mouseClicked` is the inherited empty one, so a click does nothing.

## 2. Why the label does not update

The code that increments `count` and calls `countLabel.setText(...)` lives **inside** `mouseClick`, which is never invoked, so the label stays at "Clicks: 0". (Even if it ran, the dot would be drawn with `getGraphics()` – see next section.)

## 3. How minimising and restoring reveals the repaint problem

`getGraphics()` draws straight onto the screen surface, but the program keeps **no record** of what was drawn. When the window is minimised and restored (or covered by another window, or resized) the system asks the component to repaint itself: `repaint()` → `paint()/paintComponent()`. That method knows nothing about the dots, so the background is redrawn and **all dots vanish**. Rule: *drawing must be reproducible from the program's state inside `paint`*.

## 4. The repair (TODO 1 and TODO 2)

| Step | Code | Purpose |
|---|---|---|
| TODO 1 – store | `private final List<Point> points = new ArrayList<>();` and in the handler `points.add(e.getPoint());` | The **model**: every click is remembered |
| Update label | `countLabel.setText("Clicks: " + points.size());` | The label is always derived from the model |
| Request redraw | `canvas.repaint();` | Schedules a paint on the EDT; never call `paint()` yourself |
| TODO 2 – draw | `for (Point p : points) g.fillOval(p.x - 5, p.y - 5, 10, 10);` inside `paintComponent` (AWT: `paint`) after `super.paintComponent(g)` | The **view**: redraws everything on every repaint, so dots survive minimise/restore |
| Correct name | `@Override public void mouseClicked(MouseEvent e)` | See next section |

(If your supplied file labels TODO 1/TODO 2 differently, map them as: TODO "store point & update counter" → the handler body; TODO "draw points" → the paint method.)

## 5. How `@Override` detects an incorrectly named listener method

`@Override` tells the compiler "this method must override or implement a method of a supertype". With `@Override public void mouseClick(MouseEvent e)` the compiler reports
`error: method does not override or implement a method from a supertype` **at compile time**, instead of the program silently doing nothing at run time. It turns a hard-to-find logic bug into an immediate error message (and also catches wrong parameter types or a misspelt `paintComponnet`).

## 6. Screenshots (take on your machine)
Before clicking · after 5 clicks (dots + "Clicks: 5") · after minimise → restore (dots still there) · flawed version after clicks (counter stays 0). Exact list: `../docs/SCREENSHOTS_TO_CAPTURE.md`.

## 7. Alternative concept – storing `Point` objects vs drawing onto an off-screen `BufferedImage`

| Aspect | Store `Point` list, redraw in `paint` | Draw onto `BufferedImage`, `drawImage` in `paint` |
|---|---|---|
| Repaint cost | O(n) per repaint for n dots | O(W·H) blit, independent of n |
| Memory | O(n) – ≈ 32 bytes per dot (object + reference) | O(W·H) – 4 bytes per pixel, constant (800×600 = 1,875 KB) |
| Undo / delete one dot / move | Easy (edit the list) | Hard (pixels forgotten) |
| Resize | Redraws correctly at any size | Image has fixed size; must be recreated/copied |
| Save as PNG | Must render first | `ImageIO.write` directly |
| Best for | Editable drawings, few thousand shapes | Freehand painting, very many strokes |

**Measured on this machine** (`PointsVsImageBenchmark`, 800×600 target, 20 repaints averaged, headless Java 2D software rendering – your numbers will differ):

| Dots stored | A: redraw list (ms/repaint) | B: `drawImage` (ms/repaint) | List memory (KB) |
|---|---|---|---|
| 100 | 0.735 | 0.229 | 3 |
| 1,000 | 2.779 | 0.182 | 31 |
| 10,000 | 11.441 | 0.188 | 312 |
| 100,000 | 98.466 | 0.206 | 3,125 |

Reading: A grows linearly with n (≈ 1 ms per 1,000 dots here); B is flat. Below ~1,000 dots both are far under one 16 ms frame, so the simple list is the better design for this pad; at 100,000 dots A (≈ 98 ms) would feel sluggish. Run the benchmark on your own computer and put your numbers in this table.

---

# Task 5 – Same Screen, Different Event Styles (Currency Converter)

**Files:** `ConverterCore` (rates + validation, no GUI) · `ConverterFrame` (shared screen) · `Converter1_FrameImplements` · `Converter2_NamedInner` · `Converter3_Anonymous` · `Converter4_Lambda` · `ConverterCoreTest` (10/10 checks pass)

The screen (amount field, From/To `JComboBox`, Convert button, result label) is built once in `ConverterFrame`; the **only** thing that changes between versions is how events are wired. Every version: Convert button **and** Enter in the amount field call `convert()`; the result appears in the label **and** in a dialog; invalid amounts show an error dialog; closing is handled by a `WindowAdapter` (confirmation dialog).

## 1. The four wirings

| Style | Wiring (essential lines) |
|---|---|
| 1. Frame implements `ActionListener` | `class Converter1 extends ConverterFrame implements ActionListener` · `convertButton.addActionListener(this); amountField.addActionListener(this);` · `public void actionPerformed(ActionEvent e){ convert(); }` |
| 2. Named inner class | `private class ConvertListener implements ActionListener { public void actionPerformed(ActionEvent e){ convert(); } }` · `ConvertListener l = new ConvertListener(); button.addActionListener(l); field.addActionListener(l);` |
| 3. Anonymous inner class | `ActionListener l = new ActionListener(){ public void actionPerformed(ActionEvent e){ convert(); } };` · `button.addActionListener(l); field.addActionListener(l);` |
| 4. Lambda | `ActionListener l = e -> convert();` · `button.addActionListener(l); field.addActionListener(l);` |
| Closing (all) | `addWindowListener(new WindowAdapter(){ @Override public void windowClosing(WindowEvent e){ confirmExit(); } });` – cannot be a lambda: `WindowAdapter` is a class and `WindowListener` has 7 methods |

## 2. Record of code change (non-blank, non-comment lines in each file; shared `ConverterFrame` = 51 lines, unchanged)

| Version | Lines in file | Extra constructs needed | Change vs version 1 |
|---|---|---|---|
| 1 Frame implements | 19 | `implements ActionListener`, one method on the frame | baseline |
| 2 Named inner class | 23 | 2 inner classes (convert listener + close handler) | +4 |
| 3 Anonymous class | 21 | 2 anonymous classes | +2 |
| 4 Lambda | 16 | none (one lambda) | −3 |

(Counted with `grep -vE '^\s*(//|\*|/\*|$)' file | wc -l`; each file also contains its `main`.)

## 3. Comparison

| Criterion | 1 Frame implements | 2 Named inner | 3 Anonymous | 4 Lambda |
|---|---|---|---|---|
| **Readability** | Handler is far from the registration line; one `actionPerformed` for everything becomes an if/else chain with many components | Clear name, handler grouped in one place | Handler is right at the registration line; noisy syntax | Shortest and clearest for one-line handlers; long bodies hurt |
| **Access to frame fields** | Direct (it *is* the frame) | Direct (inner class sees outer members) | Direct (captures outer `this` and effectively-final locals) | Direct (captures; `this` = enclosing frame, not the lambda) |
| **Handler reuse** | One method serves all sources, but cannot be reused by another class | Best: named class can be instantiated for several components or other frames | Reuse only through a local variable | Same as anonymous (a variable) |
| **Ease of change** | Frame's public API now exposes `actionPerformed`; adding a 2nd action means testing `e.getSource()` | Add another small class | Add another block | Add another lambda |
| **Class files produced** | 1 | 1 + 1 per inner class | 1 + 1 per anonymous class | 1 (lambdas compile to `invokedynamic`, no extra class file) |

Recommendation: lambda for the Convert action (single method call), `WindowAdapter` anonymous class for the window, named inner class if the handler grows or is shared.

## 4. Alternative concept – lambda vs method reference vs shared `Action` object

```java
// lambda
convertButton.addActionListener(e -> convert());
// method reference (only valid when the method signature matches: void m(ActionEvent))
private void onConvert(ActionEvent e) { convert(); }
convertButton.addActionListener(this::onConvert);
// shared Action: one object = text + behaviour + enabled state, usable by button, menu item, key binding
Action convertAction = new AbstractAction("Convert") {
    public void actionPerformed(ActionEvent e) { convert(); }
};
convertButton.setAction(convertAction);
amountField.addActionListener(convertAction);
convertMenuItem.setAction(convertAction);
```

| Aspect | Lambda | Method reference | `Action` |
|---|---|---|---|
| Lines | 1 | 1 (+ method) | 4–5 |
| Naming/debug | Anonymous | Named method shows in stack trace | Named object |
| Reuse across button + menu + key | Variable only | Variable only | Built-in |
| Enable/disable everywhere at once | No | No | Yes (`setEnabled(false)`) |
| Best for | Quick one-off | When the handler already exists as a method | Real applications with several triggers |

Complexity: dispatch of an `ActionEvent` is O(L) in the number of listeners L registered on the source (here 1); memory O(L). Style choice changes readability, not run-time performance.

## 5. Complete the supplied lambda program – checklist
Replace each TODO in your supplied file using `Converter4_Lambda` and `ConverterFrame.convert()` as the model: (a) register `e -> convert()` on the button, (b) register the same listener on `amountField` for Enter, (c) show the result with `JOptionPane.showMessageDialog`, (d) validate with `ConverterCore.parseAmount` so `abc`, empty and negative values show an error dialog.

## 6. Screenshots (take on your machine)
Valid conversion (label + dialog) · invalid amount (error dialog) · Enter-key conversion · closing confirmation · one valid conversion for each of the other three styles. List in `../docs/SCREENSHOTS_TO_CAPTURE.md`.

---

# Task 6 – Design Challenge: Campus Event Registration (Swing + MySQL)

**Code:** `src/campus/{model,dao,service,ui,tools}` · `test/campus/RegistrationServiceTest.java` (16/16 checks pass) · `sql/schema.sql`, `sql/seed_100k.sql`, `sql/queries.sql` · `db.properties.example`

## 1. Analysis

**Users:** *Student* (registers for an event – the committee member enters it at the desk or the student uses the form), *Committee member* (views registrations, filters by event, exports).

**Screens:** (1) Main window with menu bar, event filter, registrations table, pager, status line; (2) Register Student dialog; (3) Save-file dialog (export); (4) message dialogs (success / error / About).

**Data:** `events(event_id, name, event_date, venue)`; `registrations(reg_id, student_id, student_name, email, event_id, registered_at)` with `UNIQUE(student_id, event_id)` and index `(event_id, reg_id)`.

**Business rules:** R-a student ID is 6–12 letters/digits; R-b name 2–60 letters; R-c valid e-mail; R-d an event must be chosen; R-e a student may register for an event only once (but may register for different events); R-f only committee can export.

**Testable requirements**

| ID | Requirement | Test | Expected | Automated check |
|---|---|---|---|---|
| T1 | Valid registration saves | S200001 / Asha Raman / asha@campus.edu / Hackathon | Success message, count 1, row visible | `RegistrationServiceTest` "normal registration succeeds" |
| T2 | Short/invalid student ID rejected | `ab` | Message "Student ID must be 6-12…", nothing saved | "invalid input rejected: ab…" |
| T3 | Invalid name rejected | `A1` | "Name must be…" | yes |
| T4 | Invalid e-mail rejected | `not-an-email` | "Enter a valid e-mail…" | yes |
| T5 | No event chosen | (blank event) | "Select an event." | yes |
| T6 | Duplicate rejected | same ID + same event twice | "already registered for …", count unchanged | "duplicate for same event rejected" |
| T7 | Same student, other event allowed | S200001 + Tech Talk | Saved | yes |
| T8 | Filter by event | choose Hackathon | only its rows, count matches | "filter by event 1 gives 200 rows" |
| T9 | Paging | 1000 rows | 20 pages of 50 | "paging: 20 pages of 50" |
| T10 | Export CSV | File → Export | header + all rows for the filter, commas escaped | "CSV export", "CSV escapes commas" |
| T11 | UI stays responsive | load 100,000 rows | window repaints, spinner moves, can click while loading | manual (screenshot/video) |

## 2. Screen sketches and flow

```
MAIN WINDOW                                              REGISTER STUDENT (modal dialog)
+------------------------------------------------+       +--------------------------------+
| File | Registrations | Help                    |       | Student ID: [____________]     |
| Filter by event: [All events v]                |       | Name:       [____________]     |
|------------------------------------------------|       | E-mail:     [____________]     |
| RegID | StudentID | Name | E-mail | Event | At |       | Event:      [ -- choose v ]    |
| ...                                            |       | (red message area)             |
|------------------------------------------------|       |            [Register] [Cancel] |
|        [< Prev]  Page 1 of 20  [Next >]       |       +--------------------------------+
| 1000 registration(s) - showing 50      [====]  |
+------------------------------------------------+
Menu bar:  File -> Export to CSV...(Ctrl+E), Exit
           Registrations -> Register Student...(Ctrl+N), Refresh(F5)
           Help -> About
```
```
 [Start] -> Main window --(Ctrl+N / menu)--> Register dialog --Register--> [validate] --fail--> message shown, stay in dialog
                |  |                                |                          |
                |  |                                |                          +--ok--> success box -> dialog closes -> table reloads
                |  +--(File > Export)--> Save dialog --OK--> background export --> status "Exported n rows"
                +--(Filter / Prev / Next)--> background page load --> table + pager updated
                +--(File > Exit / [X])--> [End]
```

## 3. Where validation, duplicate checking and database access happen

| Concern | Layer / class | Method |
|---|---|---|
| Collect input, show messages | UI – `RegisterDialog`, `MainFrame` | `submit()`, `done()` |
| Format validation (ID, name, e-mail, event) | Service – `RegistrationService` | `register()` |
| Duplicate check (friendly message) | Service → DAO | `dao.exists(studentId, eventId)` |
| Duplicate guarantee under concurrency | Database | `UNIQUE(student_id, event_id)` → `DuplicateKeyException` mapped to the same message |
| SQL / JDBC | DAO – `JdbcRegistrationDao` | `insert`, `findPage`, `count`, `findAll` (PreparedStatement, try-with-resources) |
| Background threads | UI – `SwingWorker` | `doInBackground()` calls service; `done()` updates Swing on the EDT |

## 4. Layers and class diagram

```
 ui                          service                      dao                          model
+-------------------+      +-----------------------+    +-------------------------+   +------------------+
| AppMain           |      | RegistrationService   |    | <<interface>>           |   | Registration     |
| MainFrame         |----->| register(...)         |--->| RegistrationDao         |   | EventInfo        |
| RegisterDialog    |      | page(eventId, n)      |    | findEvents() exists()   |   +------------------+
+-------------------+      | count(eventId)        |    | insert() findPage()     |
                           | exportCsv(path, ev)   |    | findAll() count()       |
 tools                     | ValidationException   |    +------------+------------+
+-------------------+      | DuplicateRegistration |                 |
| LoadBenchmark     |      |   Exception           |      +----------+-----------+
| TableModelBench.. |      +-----------------------+      | JdbcRegistrationDao  | (MySQL)
+-------------------+                                     | InMemoryRegistration | (demo/tests)
                                                          |   Dao                |
                                                          +----------------------+
```
UI depends only on the service; the service depends only on the `RegistrationDao` interface; only `JdbcRegistrationDao` imports `java.sql`.

## 5. Pseudocode

```
REGISTER(studentId, name, email, event):                     // service layer
    id <- upper(trim(studentId)); n <- trim(name); mail <- trim(email)
    errors <- []
    if id !~ [A-Za-z0-9]{6,12}: errors += "Student ID must be 6-12 letters or digits."
    if n  !~ name pattern:      errors += "Name must be 2-60 letters ..."
    if mail !~ email pattern:   errors += "Enter a valid e-mail ..."
    if event == null:           errors += "Select an event."
    if errors not empty: throw ValidationException(errors)
    if dao.exists(id, event.id): throw DuplicateRegistrationException
    try dao.insert(new Registration(id, n, mail, event, now))
    catch DuplicateKeyException: throw DuplicateRegistrationException   // lost a race
    return registration

FILTER(eventFilter, pageIndex):                              // UI + service, in a SwingWorker
    eventId <- (eventFilter == "All events") ? null : eventFilter.id
    show busy cursor + progress bar
    background: total <- service.count(eventId)
                pages <- max(1, ceil(total / 50))
                p     <- clamp(pageIndex, 0, pages-1)
                rows  <- service.page(eventId, p)            // SELECT ... WHERE ... ORDER BY reg_id LIMIT 50 OFFSET p*50
    on EDT (done): if this request is not the latest, ignore
                   fill table model with rows; set "Page p+1 of pages"; enable Prev/Next; hide busy
```

## 6. Implementation and how to demonstrate it
* Without MySQL: `java -cp out campus.ui.AppMain --demo` (in-memory DAO with 1,000 rows; same service and UI code).
* With MySQL: run `sql/schema.sql` (and `sql/seed_100k.sql` for the 100,000-row test), copy `db.properties.example` → `db.properties`, put `mysql-connector-j-*.jar` in `lib/`, run `AppMain`.
* Demonstrations to capture (normal / invalid / duplicate): see `../docs/SCREENSHOTS_TO_CAPTURE.md`.

## 7. Keeping the interface responsive
All DAO calls (events, count, page, insert, export) run in `SwingWorker.doInBackground()` on a worker thread; Swing components are touched only in `done()` on the Event Dispatch Thread. While loading, the busy cursor and an indeterminate progress bar show activity, the Register button is disabled to prevent double submission, and a sequence number makes the UI ignore the result of a superseded load (user clicked Next twice / changed the filter quickly).

## 8. Alternative concept – load all 100,000 registrations at once vs paginated loading (`LIMIT/OFFSET`)

**Measured on this machine – UI/heap side, no database** (`TableModelBenchmark`, 3 runs): building 100,000 `Registration` objects and filling a `DefaultTableModel` took **406–983 ms** and about **53–57 MB** of heap; one page of 50 rows took **0.2–0.6 ms** and **0.34 MB**. Filling the model with 100,000 rows on the EDT would freeze the window for that whole time, so even the "all at once" approach must run in a background thread – and the memory cost (≈ 55 MB here, before the JTable's own per-row structures) remains.

**Database side – measure yourself** with `LoadBenchmark` (needs MySQL + `seed_100k.sql`; prints full-load, first-page and last-page times and heap). Fill in:

| Approach | Rows | DB + transfer time (ms) | Java heap (MB) | UI responsive? |
|---|---|---|---|---|
| Load all | 100,000 | ______ | ______ | Only if in background; table build ≈ 0.4–1 s |
| Page 1 (LIMIT 50 OFFSET 0) | 50 | ______ | ______ | Yes |
| Last page (OFFSET 99,950) | 50 | ______ | ______ | Yes (slower than page 1 – see below) |

| Aspect | Load all | Paginated (`LIMIT/OFFSET`) |
|---|---|---|
| Loading time | O(n) rows read, transferred, converted | O(offset + limit) per page for `OFFSET` (the server still walks the skipped rows; with `ORDER BY` on the indexed primary key this is a cheap index scan), plus one `COUNT(*)` |
| Memory | O(n) in Java (≈ 55 MB here for 100,000 rows) | O(page size) – constant (≈ 0.3 MB) |
| Window responsiveness | Poor unless background; even then the UI shows nothing until everything arrives | Immediate – first page appears at once |
| Searching/sorting whole data | Easy client-side | Must be done by SQL |
| Weakness | Does not scale to 1,000,000 rows (out of memory) | Deep pages slow down with large `OFFSET`; page contents can shift if rows are inserted while browsing; fix with keyset paging (`WHERE reg_id > ? ORDER BY reg_id LIMIT 50`, O(log n + limit)) |

Chosen design: **paginated loading, page size 50**, a `COUNT(*)` for the pager, and a full read (`findAll`) **only** for CSV export, which streams to a file in the background instead of building a table.

## 9. Complexity summary

| Operation | Time | Space |
|---|---|---|
| Validate input | O(L) for input length L | O(1) |
| Duplicate check | O(log n) (unique index) | O(1) |
| Register (insert) | O(log n) | O(1) |
| Show a page | O(offset + 50) with OFFSET; O(log n + 50) with keyset | O(50) |
| Count | O(n) worst case for InnoDB `COUNT(*)` on the filtered range (index range scan; O(1) if cached/approximate) | O(1) |
| Export | O(n) | O(n) for the list in this implementation (can be reduced to O(1) by streaming the `ResultSet`) |

---

# Final submission checklist
- [ ] Repository `CSA09-192321018-Assignment4` created and shared with faculty
- [ ] Folders Task01–Task06 with source, input files, screenshots
- [ ] Every program (including each Complete-the-Code section) executed
- [ ] README has name, register number, Java version, run steps, results
- [ ] This report has diagnoses, event tables, diagrams, pseudocode, comparisons, complexity
- [ ] Screenshots captioned with the requirement they demonstrate
- [ ] Task 6 SQL script included (`Task06/sql/`)
- [ ] GitHub link on the cover page and submitted at the required place
- [ ] At least one commit per task
