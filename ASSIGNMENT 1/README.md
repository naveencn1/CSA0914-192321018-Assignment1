# CSA0914-192321018-Assignment1 — Campus Smart-Parking System

- **GitHub:** https://github.com/naveencn1/CSA0914-192321018-Assignment1
- **Name:** V V Naveen Kumar
- **Register Number:** 192321018
- **Course:** Programming in Java — Assignment 1 (CO1)
- **Java version:** JDK 17 or newer (developed and tested on OpenJDK 21)

## Structure
- `src/parking/model` and `src/parking/service` — the shared object model (Vehicle hierarchy, ticket, passes, ParkingLot, pricing).
- `Task01` … `Task12` — one folder per task: program(s), `NOTES.md` (answer summary, pseudocode/UML where asked), `output.txt` and `screenshot.png` of the run.
- `Benchmarks` — complexity measurements.

## Compile and run everything
From the repository root: `bash run_all.sh` (needs `javac`).
On Windows PowerShell, run the commands inside `run_all.sh` one by one.

| Task | Program | Result |
|---|---|---|
| 1 | Task01Demo | objects vs parallel arrays |
| 2 | Task02Demo | casting risk: Rs 60 (wrong) vs Rs 90 (correct); jagged array |
| 3 | ParkingFacility (Complete the Code) | matches expected output |
| 4 | Task04Demo | constructors, copy constructor |
| 5 | Task05Demo | static counter across two facilities |
| 6 | Task06Demo | constructor order trace |
| 7 | Task07Demo | equals/hashCode/toString, overriding |
| 8 | FeeDemo (Complete the Code), DispatchDemo | Rs 30/90/210/true; overload + dispatch |
| 9 | Task09Demo | abstract class + interface |
| 10 | Task10Demo | encapsulation and validation |
| 11 | Task11Tests | 15/15 tests pass |
| 12 | Task12Demo | 7/7 tests pass |
| – | Bench | complexity measurements |

The full report (Tasks 1–12 with UML, pseudocode, test tables and complexity analysis) is `Report.pdf` (or `.docx`) in the repository root.
