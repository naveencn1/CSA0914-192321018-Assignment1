# CSA09 – Programming in Java · Assignment 5 (CO5) · Data Persistence Engineering

| | |
|---|---|
| **Name** | V V Naveen Kumar |
| **Register number** | 192321018 |
| **Course / assignment** | CSA09 Programming in Java – Assignment 5 (CO5: Java I/O and JDBC) |
| **Java version** | OpenJDK 21.0.13 (any JDK 17+ works; the code uses only standard Java) |
| **Database / driver used** | MariaDB 10.11.14 (MySQL-compatible) + MariaDB Connector/J 2.7.6 · local database `csa09_assignment5` created only for this assignment |
| **Repository** | `CSA09-192321018-Assignment5` |

## Repository layout
```
PartA/   A1_file_format.md  A2_buffered_io.md  A3_dao_pattern.md  A4_serialization.md  A5_correctness_safety.md   (design + pseudocode)
Task01/  B1 file store          src/ data/ out/ results/ README.md
Task02/  B2 adversarial input   src/ sql/ data/ out/ README.md
Task03/  B3 JDBC performance    src/ sql/ out/ results/ README.md   (PassLoader TODO 1-3)
Task04/  B4 final decision      src/ sql/ data/ out/ results/ README.md
run_all.sh   compile and run everything
```

## How to compile and run
1. Install a JDK (17+) and MySQL or MariaDB; create the test user once:
   `CREATE USER 'csa09'@'localhost' IDENTIFIED BY 'csa09pass'; GRANT ALL ON csa09_assignment5.* TO 'csa09'@'localhost';`
2. Put the JDBC driver jar on the class path (`JDBC=/path/to/driver.jar ./run_all.sh`).
   For **Oracle MySQL** change `PassLoader.URL` to `jdbc:mysql://localhost:3306/csa09_assignment5` and use MySQL Connector/J.
3. Each task has its own commands in its `README.md`; `run_all.sh` runs them all in order.

## Results summary
| Task | Result |
|------|--------|
| **B1** File store | `find` returns the P1003 record, `null` for damaged P1002, missing id and missing file. 50 MB line count: **10,598 ms unbuffered (52.4 M read calls) vs 18 ms buffered (802 calls)**. 1,000 searches on 100,000 passes: 7.5 s scanning vs 0.08 s with a HashMap (≈ 90×). |
| **B2** Adversarial | Unsafe login **accepted 3 injection attacks**; `PreparedStatement` rejected all of them. All 6 kinds of damaged pass lines are rejected with a reason and the search continues. |
| **B3** JDBC | 100,000 inserts: **12.4–12.9 s in auto-commit for all three methods vs 1.5 s in one transaction**; multi-row INSERT 0.67 s; LOAD DATA 0.34 s. Failure halfway: **499 rows left (auto-commit) vs 0 (transaction)**. |
| **B4** Decision | Migration of 100,000 lines: 99,800 loaded, 200 rejected (all reconciled) in 3.6 s. At 800,000 passes: text scan 75 ms, DB primary-key lookup 0.04 ms, 12 serialized campus files 752 ms; unsynchronised text-file counters produced 3,479 duplicate numbers of 4,000. **Decision: one shared relational database.** |

## Notes on evidence
* The `.png` files in each `out/` folder are **renders of the real console / `mariadb` output** captured when the programs were executed (the raw text is next to each image). They are not photographs of a desktop; replace them with your own terminal screenshots when you re-run on your machine.
* Measurements were taken on a single-core Linux virtual machine with a local database; absolute times will differ on other machines, the ratios and trends are the findings.
* `TODO` completions: `PassStore.find()` (Task01 TODO 1) and `PassLoader` TODO 1–3 (Task03) are marked with `// TODO n (completed)` comments. The original supplied skeletons were not included in the assignment deck I was given, so these classes were written to the deck's specification.
