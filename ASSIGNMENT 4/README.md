# CSA09 – Assignment 4 (CO4): Event-Driven GUI Design

| | |
|---|---|
| **Name** | V V Naveen Kumar |
| **Register number** | 192321018 |
| **Course** | Programming in Java (CSA09) |
| **Java version** | JDK 21 (tested with OpenJDK 21; JDK 17+ works because records and `instanceof` patterns are used) |
| **Libraries** | Java AWT / Swing, JDBC. Task 6 with MySQL also needs `mysql-connector-j` in `Task06/lib/` |

> Create the repository `CSA09-192321018-Assignment4`, add the faculty as collaborator, and put the repository link on the report cover page.

## How to compile and run (from the repository root)

Tasks 1, 3, 4, 5 use the default package: compile all `.java` files inside the task folder.

**Linux / macOS**
```bash
cd Task05 && javac *.java && java Converter4_Lambda       # GUI
java ConverterCoreTest                                    # logic test
```
**Windows (cmd)**: same commands (`javac *.java`, `java Converter4_Lambda`).

Task 6 (packages, so compile with `-d out`):
```bash
cd Task06
javac -d out $(find src test -name "*.java")                     # Windows: dir /s /b src\*.java test\*.java > files.txt  then javac -d out @files.txt
java -cp out campus.ui.AppMain --demo                            # no database needed (in-memory demo)
java -cp out campus.RegistrationServiceTest                      # headless tests
# With MySQL:  mysql -u root -p < sql/schema.sql ; mysql -u root -p < sql/seed_100k.sql
#              copy db.properties.example db.properties ; put mysql-connector-j-*.jar in lib/
java -cp "out:lib/*" campus.ui.AppMain                           # Windows: -cp "out;lib/*"
java -cp "out:lib/*" campus.tools.LoadBenchmark db.properties    # 100,000 rows vs one page
java -cp out campus.tools.TableModelBenchmark                    # UI/heap side, no DB
```

## Tasks – short explanation and result

| Folder | Program(s) | What it does | Result note |
|---|---|---|---|
| `Task01` | `FeePaymentFlawed` (reference), `FeePaymentFrame` + `FeeService` + `FeeRepository` (+Jdbc/InMemory) | Diagnosis of 10 design problems and redesign: UI / service / repository, `Action` per button, `SwingWorker`, `PreparedStatement` | `FeeServiceTest`: 11/11 pass |
| `Task02` | – (design report) | Event table, containment tree, 2 pseudocodes, call sequence, applet comparison, DB-vs-Map | `REPORT_Task02.md` |
| `Task03` | `LibrarySearchFrame`, `LibraryCatalog` | Library search screen: BorderLayout + nested FlowLayouts; results grow, search row/buttons keep size; paging + Reserve | `LibraryCatalogTest`: 8/8 pass; 3 resize screenshots |
| `Task04` | `DrawingPadFlawed`, `DrawingPadFixed`, `PointsVsImageBenchmark` | Misnamed listener + `getGraphics()` bugs; repair with stored `Point`s, `repaint()`, `paintComponent`; `Point` list vs `BufferedImage` measured | benchmark in `benchmark_output.txt` |
| `Task05` | `Converter1..4`, `ConverterFrame`, `ConverterCore` | Currency converter wired 4 ways (frame implements / named inner / anonymous / lambda), Enter key, dialog, `WindowAdapter` | `ConverterCoreTest`: 10/10 pass |
| `Task06` | `campus.*` (ui / service / dao / model), `sql/` | Swing + JDBC event registration: menu bar, filter, paging, duplicate check, CSV export, `SwingWorker` | `RegistrationServiceTest`: 16/16 pass; UI benchmark in `table_model_benchmark_output.txt` |

Each task folder has `REPORT_TaskNN.md` (diagnoses, tables, pseudocode, comparisons, complexity) and a `screenshots/` folder. What to capture and which requirement each screenshot proves: `docs/SCREENSHOTS_TO_CAPTURE.md`. The combined report is `REPORT.md`.
