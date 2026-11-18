# Screenshots to capture (on your own computer)

A screenshot alone is not evidence. Under each image in the report write one line:
**"Screenshot X shows <requirement/test> because <what is visible>."** Save images in `TaskNN/screenshots/` with the names below.

## Task 01 – GUI design review
| File | What to do | Shows |
|---|---|---|
| `t1_original_freeze.png` | Run `FeePaymentFlawed` (or your supplied file), click Pay, capture while the window is frozen / not responding | Problem 5 – sleep() on the EDT |
| `t1_redesign_valid.png` | Run `FeePaymentFrame`; ID `STU2024001`, amount `2500.50`, UPI → Pay | Valid payment, status line with receipt no. and balance |
| `t1_redesign_invalid.png` | Amount `abc` (or `-5`, ID `ab`) → Pay | Validation messages instead of silent failure |
| `t1_redesign_receipt.png` | After a valid payment click Receipt | Receipt action now works |
| `t1_service_test.png` | Terminal: `java FeeServiceTest` | 11/11 checks pass (service tested without GUI) |

## Task 02 – no program; diagrams are in the report (draw the containment tree and event table neatly, or paste from `REPORT_Task02.md`).

## Task 03 – layout
| File | What to do | Shows |
|---|---|---|
| `t3_small_560x360.png` | Drag the window to its minimum size | Table shrinks; search row and buttons unchanged |
| `t3_medium_800x520.png` | Default size, search "java" | Reference layout, results in centre, status message |
| `t3_large_1200x800.png` | Maximise | Only the table area grows; buttons stay natural size |
| `t3_paging_reserve.png` | Search with empty keyword, Next, select a row, Reserve | Prev/Next paging and Reserve status message |

## Task 04 – forensics
| File | What to do | Shows |
|---|---|---|
| `t4_flawed_clicks.png` | Run `DrawingPadFlawed`, click 5 times | Counter stays 0, handler never runs (Bug 1) |
| `t4_fixed_before.png` | Run `DrawingPadFixed`, no clicks | Clicks: 0 |
| `t4_fixed_after_clicks.png` | Click 5 times | Dots drawn, label "Clicks: 5" |
| `t4_fixed_after_minimise.png` | Minimise, restore | Dots still present – drawn from stored points in paint |
| `t4_compile_error_override.png` | Add `@Override` to `mouseClick` in the flawed file and compile | "method does not override or implement…" error |
| `t4_benchmark.png` | Terminal: `java PointsVsImageBenchmark` | Your measurements for the comparison table |

## Task 05 – converter
| File | What to do | Shows |
|---|---|---|
| `t5_lambda_valid.png` | `Converter4_Lambda`: 100 USD → INR, click Convert | Label + dialog "100.00 USD = 8300.00 INR" |
| `t5_lambda_invalid.png` | amount `abc` and another with `-5` | Error dialog, label reset |
| `t5_lambda_enter.png` | Type 250, From EUR To GBP, press Enter | Enter key triggers conversion |
| `t5_close_confirm.png` | Click the window [X] | WindowAdapter confirmation |
| `t5_style1.png`, `t5_style2.png`, `t5_style3.png` | One valid conversion in each other style | Same behaviour, different wiring |
| `t5_logic_test.png` | Terminal: `java ConverterCoreTest` | 10/10 checks |

## Task 06 – registration app
| File | What to do | Shows |
|---|---|---|
| `t6_main_window.png` | Start the app (MySQL or `--demo`) | Menu bar, filter, table, pager, status |
| `t6_register_normal.png` + `t6_register_success.png` | Register S300001 / Meena Iyer / meena@campus.edu / Hackathon | T1 normal registration |
| `t6_register_invalid.png` | ID `ab`, name `A1`, e-mail `x` | T2–T4 clear error messages |
| `t6_register_duplicate.png` | Register S300001 for the same event again | T6 duplicate rejected |
| `t6_filter.png` | Choose one event in the filter | T8 filtered rows and count |
| `t6_paging.png` | Click Next with the 100,000-row data | T9 pagination "Page 2 of 2000" |
| `t6_responsive.png` (or short video) | Load/export while moving the window / clicking | T11 progress bar visible, UI not frozen |
| `t6_export.png` + `t6_csv_open.png` | File → Export, open the CSV | T10 export |
| `t6_mysql_rows.png` | `SELECT COUNT(*) FROM registrations;` after seeding | 100,000 rows present |
| `t6_benchmark.png` | `java -cp "out:lib/*" campus.tools.LoadBenchmark db.properties` | Your DB numbers for section 8 |
| `t6_service_test.png` | `java -cp out campus.RegistrationServiceTest` | 16/16 checks |
