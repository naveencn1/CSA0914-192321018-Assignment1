# Assignment 3 (CO3) – Written answers and analysis (DRAFT for the joint report)

**Name:** V V Naveen Kumar  |  **Register No:** 192321018  |  **Course:** Programming in Java (CSA09)  |  **Assignment 3 (CO3)**


> Use this as a starting point. You must understand every line, re-run the programs yourself, add your own
> screenshots and timings, and put each part under the right heading (Student A / Student B / Joint).
> Numbers vary a little from run to run (the saved output_*.txt files are the reference). Measured numbers below came from a 1-core Linux sandbox with Java 21 – replace them with yours.

---
## Q1 – ATM (Exceptions)
### Failure points and classification (Student A table)
| Failure point | Exception | Class | Where caught |
|---|---|---|---|
| Card-reader file missing | `FileNotFoundException` (subclass of `IOException`) | **Checked** | `catch (FileNotFoundException)` in `withdraw` |
| Account typed as letters | `NumberFormatException` | Unchecked | multi-catch `NumberFormatException \| InputMismatchException` |
| Amount typed as letters | `InputMismatchException` | Unchecked | same multi-catch |
| Amount missing | `NoSuchElementException` | Unchecked | `catch (NoSuchElementException)` |
| Amount ≤ 0 | `IllegalArgumentException` (thrown by us) | Unchecked | `catch (IllegalArgumentException)` |
| Amount > balance | `InsufficientFundsException` (ours, extends `Exception`) | **Checked** | `catch (InsufficientFundsException)` |
| JVM running out of memory, stack overflow | `OutOfMemoryError`, `StackOverflowError` | **Error** (unchecked, not meant to be caught) | not caught |

Hierarchy: `Throwable → Error` and `Throwable → Exception → {IOException → FileNotFoundException, RuntimeException → {IllegalArgumentException → NumberFormatException, NoSuchElementException → InputMismatchException}, InsufficientFundsException}`.

**Order of catch blocks.** Java checks catch blocks top to bottom and takes the first one that matches, so subclasses must come before their superclasses. `NumberFormatException` is a subclass of `IllegalArgumentException`, so it is caught (inside the multi-catch) first; `InputMismatchException` is a subclass of `NoSuchElementException`, so the multi-catch comes before it. The compiler rejects the wrong order ("exception … has already been caught" – see `BrokenOrder.java.txt`). Also, alternatives in a multi-catch may not be related by inheritance.

**What `finally` guarantees.** It runs whether the try ends normally, with a caught exception, or with an uncaught one (and even with `return`). Here it always closes the `Scanner` and "ejects the card", so a failed withdrawal cannot leave a session open. (Only `System.exit()` or a JVM crash skip it.)

### Student B – test table (from `Q1/output_ATM.txt`)
| Test | Input | Path | finally |
|---|---|---|---|
| T1 | `1001 2000` | no exception | ran |
| T2 | `abc 500` | NumberFormatException (multi-catch) | ran |
| T3 | `1001 xyz` | InputMismatchException (multi-catch) | ran |
| T4 | `1001 -50` | IllegalArgumentException | ran |
| T5 | `1001 99999` | InsufficientFundsException | ran |
| T6 | missing card file | FileNotFoundException | ran |
| T7 | `1001` | NoSuchElementException | ran |

**Stack trace (UncaughtDemo), read bottom to top:** `UncaughtDemo.main(line 6)` called `withdraw` (line 4) which called `parseAmount` (line 3) which called `Integer.parseInt`, which called `NumberFormatException.forInputString`, where the exception object was created. The first line names the exception and message (`For input string: "abc"`); each `at` line is one stack frame, most recent call first. Nobody caught it, so the thread died and the JVM printed it.

### Joint design question – single `catch (Exception e)`?
Acceptable only at the outermost boundary (e.g. the top of the ATM loop) to log and show a safe generic message so the machine does not crash, ideally after specific catches. What is lost: the ability to respond differently (retry on bad input vs. decline vs. call a technician), it hides programming bugs (`NullPointerException`), it catches things you did not intend, and the messages become uninformative.

### Alternative: error codes (`ErrorCodeATM.java`)
Pre-check with `hasNextInt()`, return `-1…-4`. Readability: the happy path is buried under `if` checks and every caller must remember to test the code (forgetting it is silent); exceptions separate normal flow from error flow and cannot be ignored (checked ones). Checks: 4 explicit checks per withdrawal for codes vs. checks only where the failure happens. Cost: a return code is O(1) time and O(1) space; throwing creates an exception object, **fills in the stack trace (cost grows with stack depth d, O(d) time and space)** and unwinds the stack. Measured: 2.6 ns/call for the code vs 230 ns/call for throw+catch (≈ 87×). That is irrelevant for user typos, but matters if failure is the *common* case in a hot loop.

---
## Q2 – Portal exceptions
**Hierarchy:** `Exception → ExamException → {ExamClosedException, NotRegisteredException, SubmissionStorageException}` (checked) and `RuntimeException → FileTooLargeException` (unchecked).
**Why checked/unchecked.** The first three are expected, recoverable conditions that the controller must decide how to show to the student, so the compiler forces handling. `FileTooLargeException` is a precondition that the controller can check up-front; forgetting it is a validation bug, and forcing `throws` on every layer would add noise.

**Propagation path:** `StorageLayer.store()` (IOException caught, wrapped) → `ServiceLayer.submit() throws ExamException` → `ControllerLayer.handle()` which finally handles it. The student only sees *"Sorry, your file could not be saved…"*; the full chain is logged.

**Chaining evidence (`output_ExamPortal.txt`):** with the cause the trace ends with `Caused by: java.io.IOException: Disk quota exceeded`; `getCause()` returns that exception. **Without the cause** the "Disk quota exceeded" reason disappears – `getCause()` is `null`, so support staff cannot tell whether it was a full disk, a permission problem or a network failure.

**Complete the code:** `throw new SubmissionStorageException("Could not store " + fileName, e);`. If the IOException were not passed, `getCause()` would be `null` and `e.getCause().getMessage()` would throw a `NullPointerException` instead of printing the Cause line.

**Joint conclusion.** A `SubmissionStorageException` hides storage technology (disk today, cloud tomorrow) from the UI, belongs to the portal's own vocabulary, lets the controller catch one meaningful type, and still keeps the low-level cause for diagnosis. An unchanged `IOException` leaks implementation detail and forces the UI to know about layers it should not.

**Alternative – unchecked custom exceptions.** Method signatures drop `throws …`; callers need no try/catch (they compile even if they ignore the case) – less clutter, but handling becomes a convention rather than a compiler guarantee. Cost: every exception captures a stack trace at construction – O(d) time and O(d) space (d = stack depth, ~tens to hundreds of frames); checked vs unchecked makes no difference. Chaining adds one reference per link, so one exception object keeps its own trace plus the whole cause chain alive: memory ≈ sum of all traces in the chain, O(k·d) for k links.

---
## Q3 – Resource safety (from `output_ReportGenerator.txt`)
Resources: each store `BufferedReader`/`FileReader` and the output `PrintWriter`/`FileWriter` – each must be closed exactly once, whatever happens.

**Why the manual version leaks (Student B).** `manualLeaky` calls `close()` at the end of the try block with no `finally`. Any exception between open and close (here `NumberFormatException` while reading store3) jumps over the `close()` lines, so handles stay open until garbage collection – eventually "Too many open files".

**Measured (Student A) – same failing input (store3 contains `N/A`):**
| Version | Open readers | Open writers |
|---|---|---|
| manual, no finally | **4** | **1** |
| manual with finally | 0 | 0 |
| try-with-resources | 0 | 0 |

Good input: all three produce identical reports (`Reports identical: true`). Debugging: put a breakpoint on the `close()` loop in `manualLeaky`, step through with the bad file – the debugger never reaches it. Logging: `sumStore` logs `File …store3.txt failed at line 2`.

**try-with-resources:** every resource declared in the header is closed automatically when the block ends (normally or by exception), **in reverse order of declaration**. **Suppressed exception:** if the body throws and `close()` also throws, the body's exception stays primary and the close exception is attached via `getSuppressed()` (shown in the demo: primary `body failure`, suppressed close failures of B and A).

**Joint decision – when is try-with-resources not enough?** When you need cleanup that is not `AutoCloseable` (delete a temp file, release a lock, roll back a transaction), when a partial output file must be deleted or marked incomplete, when failures must be retried or reported per file (one bad store should not abort the others), or when the resources are opened somewhere else and passed around (ownership unclear).

**Alternative.** *finally with explicit close:* same O(f) time (read f files once), but more code and risk (close can throw, must null-check, must close each in its own try). *No closing:* works until the OS limit; handles held = f readers + 1 writer. Max handles open on failure: leaky/no close = f+1 (all opened up front; in our run 4+1); finally and try-with-resources = 0 after failure; if try-with-resources processes one store at a time, peak is 2 handles (one reader + writer) → O(1) space instead of O(f).

---
## Q4 – Threads (from `output_Scanning.txt`)
**Thread vs Runnable (Student A).** Extending `Thread` ties the task to the thread and uses up the single inheritance slot; implementing `Runnable` separates *what to run* from *how to run it*, allows another superclass, allows the same task to be run by threads or executors. Prefer Runnable (or Callable) in almost all cases.

**Life cycle:** NEW (created) → RUNNABLE (after `start()`) → TIMED_WAITING (`sleep`) → RUNNABLE → TERMINATED. Also BLOCKED (waiting for a monitor) and WAITING (`wait()`, `join()`). Evidence printed in the output: `NEW` before start, `TIMED_WAITING` while scanning, `TERMINATED`/`isAlive=false` after join.

**`run()` vs `start()`:** `run()` is an ordinary method call, so it executes in the *calling* thread. Evidence: `running in thread: main` for `run()` vs `running in thread: Worker` for `start()`.

**`join()`:** `main` blocks until each station terminates, so the "TOTAL scanned" line prints only after all four finish (77 every time). Without join it could print early with a smaller number.

**Experiments (Student B).** Ten runs: finishing order was not constant (2 distinct orders in our 10 runs; yours will differ). Priorities: with priority 1 vs 10 for identical work the winner was not consistent (Station-2 won 3 of 5 – a *hint*, the OS scheduler decides; on a mostly-sleeping workload priority has almost no effect). Daemon: `main()` ended while the daemon was alive; the JVM exited without waiting and its final message never appeared (only non-daemon threads keep the JVM alive).

**Joint conclusion.** The order depends on the OS scheduler, CPU load, timing jitter, sleep accuracy – none are under the program's control, so a correct program must use join/locks/queues for ordering, never rely on "usually A finishes first".

**Alternative – ExecutorService (`ScanExecutor.java`).** `Executors.newFixedThreadPool(4)`, `submit` Callables, `future.get()` for results (no shared counter needed). For n tasks of cost c on p threads the completion time ≈ ⌈n/p⌉·c. Measured n=400, 2 ms each: p=1 878 ms, p=2 430, p=4 216, p=8 108 (ideal 800/400/200/100). Memory: p thread stacks (≈1 MB virtual each) instead of one thread per task → O(p) instead of O(n) when n tasks would otherwise each get a thread; the pool also reuses threads (no creation cost per task).

---
## Q5 – Race condition
**Complete the code:** `public synchronized void book(int seat) { if (!booked[seat]) { booked[seat] = true; bookingsMade++; } }` → `Seats sold: 10`.

**Why the unsynchronised version can sell more than 10:** check-then-act is not atomic. Interleaving: T1 reads `booked[3]==false`; T2 reads `booked[3]==false`; T1 sets true, `bookingsMade++`; T2 sets true, `bookingsMade++` → seat 3 sold twice. `bookingsMade++` is also read-modify-write, so counts can be lost too. **Critical section:** the `if (!booked[seat]) { booked[seat]=true; bookingsMade++; }` block.

**Honest result.** On my machine the *original* slide program (no `synchronized`) printed `Seats sold: 10` in 10/10 runs: each thread is so short that it usually finishes before the next starts – a race that exists but is rarely visible. To reproduce it reliably (Student A), `BookingExperiment` widens the window with `Thread.yield()` between check and act: sold = 16–18 in all 10 runs (76 extra bookings in total). Try the slide version on your own computer 10 times and record whatever you get – both outcomes are valid evidence.

**Strategies (Student B).** Method vs block: `synchronized` method locks `this` for the whole method; block locks `this` only around the critical section, so non-critical work (pricing) runs in parallel. Static synchronisation locks `Foo.class` – needed only for *static shared state* (e.g. one counter across all 20 screens: `StaticSyncDemo` gives 100000 ✓). The intrinsic lock: every object has one monitor; instance methods and `synchronized(this)` use it; static methods use the `Class` object's.
Measured 10,000 attempts / 1,000 seats / 8 threads (µs, median): unsafe 389, sync method 549, sync block 504, AtomicBoolean 136, ReentrantLock 234 – **1 core, very noisy; re-measure on yours.** Expect on multi-core: block ≤ method; atomics best at low/moderate contention.

**Joint decision – 20 screens.** One lock *per screen* (a `ReentrantLock` or `synchronized` on the screen object, critical section kept minimal) or `AtomicBoolean[]` per screen: screens are independent, so a single global lock would serialise unrelated bookings. Never a static lock for per-screen data.

**Alternative approaches.** `AtomicBoolean[]` + `compareAndSet`: lock-free, one atomic op per booking, O(1) time (retries only under contention), O(s) extra space (one object per seat). `ReentrantLock`: same space O(1) extra, adds `tryLock`/fairness/timeouts, must `unlock()` in `finally`. `ConcurrentHashMap.putIfAbsent`: O(1) expected, O(s) space for entries (grows with booked seats). Under contention locks make threads block/context-switch; CAS retries burn CPU but never block.

---
## Q6 – Deadlock and communication
**Part B (Student A).** `wait()` must be inside `synchronized` because it must own the monitor (otherwise `IllegalMonitorStateException`) and the check-then-wait must be atomic (no lost wake-up). It must be in a **loop** because of spurious wake-ups and because another thread may have changed the condition between `notifyAll()` and this thread regaining the lock. Measured (`TokenCounterExperiments`): with `while`, 300/300 trials OK; with `if`, **53/300 wrong** (a consumer woke up with count==0 and took garbage / counter went below 0). `notify()` vs `notifyAll()`: with one producer and several consumers I observed 100/100 OK for `notify()`, because only one kind of thread can be waiting at a time here (consumers wait only when empty, the producer only when full, never both). It fails when there are several producers *and* consumers: `notify()` can wake another waiter of the same kind and everyone sleeps forever. `notifyAll()` is always safe. Counter-size trace: min 0, max 5 never exceeded. `sleep()` vs `wait()`: sleep keeps the lock, wakes after time; wait releases the lock, wakes by notify/timeout/interrupt.

**Part A (Student B).** `WalletTransfer`: A→B and B→A, each locks `from` then sleeps then `to` → both threads `BLOCKED`, JVM `findDeadlockedThreads` reports 2. Four conditions (Coffman): mutual exclusion (monitor), hold-and-wait (holds one account, wants another), no pre-emption (monitors cannot be taken away), circular wait (T1→B held by T2→A held by T1). **Fix:** always lock the lower account id first: circular wait becomes impossible because locks are acquired in one global order. Evidence: same transfers finish (C=950, D=1050) and 40,000 random concurrent transfers keep the total at 5000.

**Joint.** Both are about threads waiting for each other: deadlock is waiting with no one able to proceed, producer–consumer is waiting that is released by the other side's action. Progress is ensured by breaking circular wait (lock ordering) and by `notifyAll()` after every state change in a loop.

**Alternatives.** `ArrayBlockingQueue(5)`: `put`/`take` block internally, O(1) per operation, space O(5); sum 210 ✓ – fewer lines and no wait/notify mistakes. `tryLock(timeout)`: no deadlock because a thread gives up and retries, but adds retry logic and possible livelock/starvation (0–5 retries per run in my tests). Per transfer: ordering O(1) with 2 locks; tryLock O(1) expected plus retries. Space O(1) per account (a lock object). **Less error-prone:** the blocking queue and lock ordering (simple rule) – hand-written wait/notify and timeouts are easiest to get wrong.

---
## Q7 – Fault-tolerant processor (design from first principles)
**Stage 1 – requirements (joint).** Parallel: each of the 10,000 files is independent (read→validate→compute). Shared: the result/failure collections and counters. Failures: missing file, empty, wrong format, non-numeric, out-of-range, other I/O error, unexpected bug. Measurable requirements: R1 every file is accounted for: `succeeded + failed == 10,000`; R2 a bad file never stops the run (0 aborts); R3 each failure has a category and file name; R4 total time decreases as the pool grows (to the point where the CPU/disk saturates); R5 summary verified on a hand-checked set.

**Stage 2 – design (Student A).** Class diagram: `ResultProcessor` (main/orchestrator) → `ExecutorService` (fixed pool) → one `Callable<Boolean>` per file (`task(path, store)`); `Store` holds `ConcurrentHashMap<String,Result> successes`, `ConcurrentHashMap<String,String> failures`, `ConcurrentHashMap<String,LongAdder> failureCounts`; `BadScoreFileException(category)` for validation failures; `Result` record.
Pseudocode:
```
pool = fixedThreadPool(p)
for file in manifest: futures += pool.submit(task(file))
for f in futures: try f.get() catch ExecutionException -> record UNEXPECTED
pool.shutdown(); awaitTermination
print summary; assert succeeded + failed == total
task(file): try ok(process(file)) catch BadScore -> fail(category) catch NoSuchFile -> fail(MISSING) catch IOException -> fail(IO)
```
Why `ConcurrentHashMap`: thread-safe, fine-grained locking so many workers write without one global lock, keyed by file name so no lost update or duplicate; `LongAdder` for contended counters. Exception strategy: every Callable catches the known failures itself and the loop over Futures catches anything else, so one failure cannot stop the run.

**Stage 3 – measurements (Student B, `output_bench.txt`; 1-core sandbox).** 10,000 files, 1 ms simulated I/O latency each: pool 1 = 12,039 ms; 2 = 5,849; 4 = 2,893; 8 = 1,481; 16 = 748 (almost perfect scaling because threads mostly *wait*). With no added latency (pure CPU/disk work on 1 core): 1 = 183 ms, 2 = 129, 4 = 112, 8 = 121, 16 = 131 – beyond a few threads there is no gain and 8/16 are slightly slower than 4 (context switching on 1 core). Counts for the generated data: ok = 9,424, failed = 576 (EMPTY 90, FORMAT 185, MISSING 202, RANGE 99), `succeeded + failed = 10,000`. Hand-checked set of 10 files: 6 ok + EMPTY 1, FORMAT 1, RANGE 1, MISSING 1 – matched with pool sizes 1 and 4. Unhandled failures found: none; (limitation tested: a file whose first line is blank is classified EMPTY even if later lines are valid).

**Stage 4 – defend (joint).** Recommended pool size: for I/O-bound work ≈ cores × (1 + wait/compute) – large (e.g. 16–64) when files come from slow storage; for CPU-bound work ≈ number of cores (extra threads only add context-switch overhead – shown by the slowdown beyond 4 threads). Guarantee of a complete, correct summary: every file produces exactly one entry in `successes` or `failures` (keyed map, no duplicates), futures are all waited on, shutdown + awaitTermination before printing, and the invariant `succeeded + failed = total` is checked. Remaining limitation: results are only in memory (a crash loses them; no checkpointing/retry of transient I/O errors), and a hung read would block a worker forever (no per-file timeout).

**Alternative (`output_alt.txt`).** `parallelStream()`: uses the shared common pool (size = cores−1; here 1) → 5,958 ms for blocking I/O tasks – poor, and blocking inside the common pool starves other parallel streams; exceptions must be caught inside the lambda or the first one aborts the stream. Thread per file: 1,145 ms here only because tiny tasks end quickly (peak measured 74 threads); worst case all 10,000 alive = 10,000 × stack size (≈1 MB reserved each ≈ 10 GB address space) → possible `OutOfMemoryError: unable to create native thread`; failure handling identical if each thread catches its own. ExecutorService: time ≈ (n/p)·c (+ overhead), memory p × stack, controllable and failure handling per `Future`.

---
## Individual Contribution Record (fill in your names)
| Question | Student A – Primary | Student B – Primary | Joint |
|---|---|---|---|
| Q1 | Exception design: hierarchy, failure table, catch order, finally | Testing & tracing: 5+ tests, stack trace, broad-before-narrow demo | Implementation, catch(Exception) discussion |
| Q2 | Class diagram, checked/unchecked reasons, propagation path | Scenarios, getCause()/chained trace, lost-cause demo, test table | Layered implementation, conclusion |
| Q3 | Debugging, failing input, open-file count, logging | Resources, leak explanation, TWR order, suppressed exceptions | Both versions, engineering decision |
| Q4 | Thread vs Runnable, life-cycle diagram, run vs start, join | 10 runs, priorities, daemon, scheduler explanation | Implementation, conclusion |
| Q5 | Reproduce and explain race, critical section | Timing, method vs block, static sync, intrinsic lock | Three versions, 20-screen decision |
| Q6 | wait/notify design, size trace, sleep vs wait | Deadlock construction, four conditions, lock ordering | Both parts, conclusion |
| Q7 | Stage 2 design | Stage 3 implement & measure | Stage 1, Stage 4 |
