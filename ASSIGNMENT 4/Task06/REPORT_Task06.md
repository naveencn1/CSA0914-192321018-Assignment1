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
