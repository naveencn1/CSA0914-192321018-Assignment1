# CSA09-192321018-Assignment2

**Name:** V V Naveen Kumar  **Register Number:** 192321018  **Java version:** 21 (OpenJDK 21.0.12) – any Java 17+ works

Full analysis, tables and answers: see **REPORT.md**. Raw output of every program: `TaskXX/output*.txt`.

## How to compile and run
Each task is a single `.java` file (several classes; the class with `main` is named after the file).
```
cd Task01 && javac Q1Benchmark.java && java Q1Benchmark
```
(Java 11+ also allows `java Q1Benchmark.java` directly.)

| Task | Folder / main class | What it does | Result (short note) |
|---|---|---|---|
| Q1 | Task01 / `Q1Benchmark` | String +, StringBuilder, StringBuffer, join, StringJoiner, format on 3 workloads | `+` is ~70× slower and allocates ~325× more on 2,000-field tickets; negligible for one short ticket; StringBuffer ≈ StringBuilder uncontended |
| Q2 | Task02 / `Q2Playlist` | ArrayList vs LinkedList (add/insert/remove/get/iterate) at 1k/10k/100k + ArrayDeque "Up Next" | ArrayList best for index/iterate; front edits O(n); LinkedList middle ops slow too; ArrayDeque for Up Next |
| Q3 | Task03 / `Q3Contract` | hashCode/equals contract; TODO 1 completed | Unique students 3 before TODO 1, **2** after |
| Q4 | Task04 / `Q4Pharmacy` | HashMap, LinkedHashMap, TreeMap, Hashtable, sorted array at 4 sizes | all < 1 µs/lookup; LinkedHashMap + sort for code-order report chosen |
| Q5 | Task05 / `Q5Ranking` | Generic `Leaderboard<T>`, comparator chain, faulty comparators, TODO 1 completed | consistent ranking; subtraction comparator broken by overflow |
| Q6 | Task06 / `Triage` | Triage PriorityQueue (TODO 1 completed), growth, starvation + aging | hand-verified order matches; 5,000 patients/day ≈ 35 ms |
| Q7 | Task07 / `Q7Redesign` | Diagnose/redesign/validate the Q1 ticket builder | 27× less allocation (50 fields), 60× faster (2,000 fields) |

## Screenshots
Add a screenshot of each program's output in the matching `TaskXX/` folder (e.g. `Task03/before_TODO1.png`, `Task03/after_TODO1.png`).
