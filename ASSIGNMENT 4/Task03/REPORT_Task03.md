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
