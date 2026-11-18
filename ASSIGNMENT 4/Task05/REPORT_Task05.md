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
