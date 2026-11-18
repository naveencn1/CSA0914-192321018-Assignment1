# CSA09 – Assignment 3 (CO3): Exception Handling & Multithreading

| | |
|---|---|
| Name | V V Naveen Kumar |
| Register number | 192321018 |
| Course | Programming in Java (CSA09) |
| Java version | *(fill in: output of `java -version` on YOUR machine)* |
| Partner (Student B / A) | *(fill in)* |

## How to compile and run
Every folder holds the source plus `output_*.txt` (captured console output). Run everything with `./run_all.sh`
or one program at a time, e.g.

```bash
javac -d out/q1 Q1/ATM.java && java -cp out/q1 ATM
```

| Task | Source | Run | Result |
|---|---|---|---|
| Q1 ATM | `Q1/ATM.java` | `java -cp out/q1 ATM` | 7 tests, each hits a different catch block; `finally` ran 7/7 |
| Q1 stack trace | `Q1/UncaughtDemo.java` | `java -cp out/q1 UncaughtDemo` | uncaught `NumberFormatException` trace |
| Q1 broad-before-narrow | `Q1/BrokenOrder.java.txt` | rename to `.java`, compile | compile error "already been caught" |
| Q1 alternative | `Q1/ErrorCodeATM.java` | `java -cp out/q1 ErrorCodeATM` | error codes + exception-vs-code benchmark |
| Q2 complete the code | `Q2/StorageDemo/StorageDemo.java` | `java -cp out/q2a StorageDemo` | matches expected output |
| Q2 portal | `Q2/ExamPortal/ExamPortal.java` | `java -cp out/q2b ExamPortal` | 6 scenarios, chained + unchained trace |
| Q3 | `Q3/ReportGenerator.java` | `java -cp out/q3 ReportGenerator` | leaky version leaves 4 readers + 1 writer open; other two leave 0 |
| Q4 | `Q4/Scanning.java`, `ScanExecutor.java` | `java -cp out/q4 Scanning` | life cycle, run vs start, 10 runs, priority, daemon; executor alternative |
| Q5 complete the code | `Q5/SeatBooking.java` | `java -cp out/q5 SeatBooking` | `Seats sold: 10` |
| Q5 experiments | `Q5/BookingExperiment.java`, `StaticSyncDemo.java` | `java -cp out/q5 BookingExperiment` | race reproduced; 5 strategies timed |
| Q6 complete the code | `Q6/TokenCounter.java` | `java -cp out/q6 TokenCounter` | `Sum of tokens collected: 210 (expected 210)` |
| Q6 experiments | `Q6/TokenCounterExperiments.java`, `WalletTransfer.java`, `AlternativeDesigns.java` | see `run_all.sh` | size trace, while→if, deadlock + fix, blocking queue, tryLock |
| Q7 | `Q7/ResultProcessor.java` | `java -cp out/q7 ResultProcessor test, bench or alt` | 10-file hand check, pools of 1/2/4/8/16, parallel stream and thread-per-file |

## Commit plan (one commit per task at least)
`git add Q1 && git commit -m "Q1: ATM exception handling"` … repeat for Q2–Q7, then `README.md`, then the report.
Do NOT upload everything in one commit on the last day – the history is checked.

## Honesty note
The output files were produced on a 1-core Linux sandbox. **Re-run on your own computer, take your own screenshots and
use your own timings** – absolute numbers (and race/ordering results) will differ, the conclusions should not.
