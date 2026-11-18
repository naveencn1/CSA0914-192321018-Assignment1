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
