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
