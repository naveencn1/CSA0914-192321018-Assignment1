# Programming in Java (CSA09) – Assignment 2 (CO2): Strings and Collections

**Name:** V V Naveen Kumar  **Register No:** 192321018  **GitHub:** https://github.com/<username>/CSA09-192321018-Assignment2

> All numbers below come from the programs in this repository, run on Java 21 (1 CPU core, no other load). Raw output is in each `TaskXX/output*.txt`. Timings of a few microseconds or less are noisy (JIT, GC, cache); **re-run on your own machine, replace the tables with your numbers, and keep the reasoning.**

---

## Q1 – Building Text Under Load: String, StringBuilder or StringBuffer? (`Task01/Q1Benchmark.java`)

### Prediction (before running)
`String` is immutable, so `s += x` allocates a new String and copies everything built so far on every iteration. For a ticket of n fields (average length L) the characters copied are L(1+2+…+n) = O(n²·L), with n garbage Strings per ticket. `StringBuilder` appends into one growing char/byte array (capacity doubles, so amortised O(1) per append, O(n) per ticket). `StringBuffer` does the same but every method is `synchronized`, so I predicted it to be equal or slightly slower than `StringBuilder` when uncontended. For one short ticket I predicted the difference to be negligible; for 10,000 × 50 fields noticeable; for very long tickets dramatic.

### Workloads
A: 1 ticket × 8 fields. B: 10,000 tickets × 50 fields. C (own): 50 tickets × 2,000 fields. Time = average of repeated runs after 2 warm-up runs (`System.nanoTime()`); "allocated" = bytes allocated by the thread (`ThreadMXBean.getThreadAllocatedBytes`) – more reliable than heap-used, which GC resets (heap-before/after values were ≈4 MB for every approach, i.e. the garbage was already collected, which itself shows the intermediates are short-lived garbage rather than a leak).

| Workload | Approach | avg time (ms) | allocated (MB) | est. intermediate Strings |
|---|---|---|---|---|
| A (1×8) | String + | 0.005 | ≈0 | 8 |
| | StringBuilder | 0.000 | ≈0 | 1 |
| | StringBuffer | 0.001 | ≈0 | 1 |
| B (10,000×50) | String + | 13.32 | 220.6 | 500,000 |
| | StringBuilder | 7.66 | 31.4 | 10,000 |
| | StringBuffer | 6.71 | 31.5 | 10,000 |
| | String.join | 6.62 | 10.4 | – |
| | StringJoiner | 7.09 | 13.5 | – |
| | String.format | 27.02 | 171.1 | – |
| C (50×2,000) | String + | 116.34 | 1,724.5 | 100,000 |
| | StringBuilder | 1.60 | 5.3 | 50 |
| | StringBuffer | 1.29 | 5.3 | 50 |
| | String.join | 0.69 | 2.1 | – |
| | StringJoiner | 0.79 | 2.5 | – |
| | String.format | 5.73 | 35.0 | – |

### Prediction vs observation
Supported. Workload A: all approaches are microseconds – the choice is irrelevant. Workload B: `+` is ~1.7× slower and allocates ~7× more. Workload C: `+` is ~70× slower and allocates ~325× more, matching the quadratic growth (40× more fields per ticket → time per ticket grew far faster than 40×). `StringBuffer` was *not* measurably slower than `StringBuilder` (it was marginally faster in this run, which is noise): the JIT removes/cheapens uncontended locking. So the real reason to prefer `StringBuilder` over `StringBuffer` is not speed but that the lock buys nothing for a local object (see below).

### Design decision (multithreaded booking server)
* **Per-ticket text → `StringBuilder`** (or `String.join`/`StringJoiner` if the fields are already in a collection). The builder is a local variable of one thread, so no synchronisation is needed.
* **Shared booking log →** `StringBuffer` is *safe per call* but not enough: `log.append(pnr).append(" ").append(fare)` is three separate synchronised calls, so another thread's text can be interleaved between them. Best choice: build the whole log line in a local `StringBuilder`, then hand it over with **one** atomic operation – e.g. `synchronized (lock) { log.append(line); }`, a `ReentrantLock`, or (preferred) a `BlockingQueue<String>` drained by one logger thread. `StringBuffer` is acceptable only if each thread appends one complete string in a single `append` call.

### Alternative concept and complexity analysis
`String.join("", fields)` / `StringJoiner` first compute the total length and allocate once (or grow a single buffer) → O(n) time, O(n) space, no intermediates; in the measurements they allocated the least (10.4 MB vs 31.4 MB for B). `String.format` parses the format string on every call, creates a `Formatter` and temporaries: O(n) but with a large constant (≈3.5× slower than the builder in B, 5.4× more memory) – good for readability, bad for hot loops.

| Method (n fields, avg length L) | Time (chars copied) | Extra space |
|---|---|---|
| repeated `+` | Σ i·L = L·n(n+1)/2 = **O(n²)** | n garbage Strings, total allocated O(n²) chars, live at any time O(n) |
| `StringBuilder` | amortised **O(n)** (doubling ⇒ total copy ≤ 2·nL) | one buffer ≤ 2× final size + final `toString` copy ⇒ O(n) |
| `String.join`/`StringJoiner` | **O(n)** | exact-size result, O(n) |

---

## Q2 – Music Playlist Editor (`Task02/Q2Playlist.java`)

### Experiments
Sizes n = 1,000 / 10,000 / 100,000. Each operation is repeated (2,000 ops, or 500–20 for slow ones), each configuration is averaged over 5 repetitions on a fresh list after 1 warm-up. Values: microseconds per operation (iterate: per full pass).

| Operation | List | n=1,000 | n=10,000 | n=100,000 |
|---|---|---|---|---|
| add at end | ArrayList | 0.054 | 0.034 | 0.041 |
| | LinkedList | 0.125 | 0.038 | 0.070 |
| insert at front | ArrayList | 0.299 | 0.971 | 16.451 |
| | LinkedList | 0.148 | 0.042 | 0.058 |
| insert in middle | ArrayList | 0.064 | 0.194 | 5.479 |
| | LinkedList | 1.834 | 7.559 | 106.123 |
| remove at front | ArrayList | 0.050 | 0.421 | 7.409 |
| | LinkedList | 0.024 | 0.122 | 0.025 |
| remove in middle | ArrayList | 0.037 | 0.121 | 4.384 |
| | LinkedList | 0.664 | 6.447 | 109.409 |
| get k-th | ArrayList | 0.279 | 0.129 | 0.085 |
| | LinkedList | 0.575 | 3.741 | 55.647 |
| iterate (full pass) | ArrayList | 20.0 | 85.1 | 80.7 |
| | LinkedList | 73.1 | 125.2 | 554.9 |

### Interpretation
* Add at end: both O(1) amortised – equal. ArrayList occasionally copies when it grows (×1.5).
* Front insert/remove: ArrayList shifts n elements (O(n)): 16 µs vs 0.06 µs at n=100,000 – about 280× worse. LinkedList O(1).
* **Middle insert/remove: LinkedList is *worse* (≈20–25× at n=100k)**, because it must first *walk* to the position (O(n) pointer chasing, cache-unfriendly) whereas ArrayList does a fast `System.arraycopy`. LinkedList only wins in the middle if you already hold an iterator at that position.
* get(k): ArrayList O(1), LinkedList O(n) (≈650× slower at 100k). Iteration: both O(n) but LinkedList is ≈1.5–7× slower (poor locality, extra node objects, ~24 bytes overhead per element).

### When is ArrayList reasonable, and when would I stop recommending it?
Reasonable: playlists that are mostly appended to, read by index ("jump to k-th") and iterated, with insertions/removals that are occasional and lists of up to a few thousand songs (a 1,000-song playlist inserting at the front costs 0.3 µs – irrelevant to a human). I would stop recommending it when the workload has *frequent insert/remove at the front or middle on large lists* (here: at ≈10⁴ songs the cost is already ~1 µs per op and at 10⁵ ~16 µs; with thousands of edits per second per list it becomes visible), or when it is used as a queue (`remove(0)`).

### Changed condition – "Up Next" queue
Operations: add at both ends, remove from front, no random access. Measured (µs/op, mix of addFirst/addLast/pollFirst): n=100,000 → ArrayList 4.98, LinkedList 0.013, ArrayDeque 0.045 (small-n values are noisy). `ArrayDeque` is the right choice: a resizable circular array, O(1) amortised at both ends, no per-element node objects (less memory, better cache locality than LinkedList), and it implements `Deque`/`Queue` which is exactly the intent. Its only limit is no `get(k)`, so I keep `ArrayList<Song>` for the playlist and use `ArrayDeque<Song>` for Up Next.

---

## Q3 – Engineer the hashCode–equals Contract (`Task03/Q3Contract.java`)

**Complete the Code** (the slide cuts off before `main`; I reconstructed `main` to produce the expected output).
* Before TODO 1: `Unique students: 3` (output in `output_before_TODO1.txt`).
* After TODO 1 (`return Objects.hash(regNo);`): `Unique students: 2`.

### Predictions and observed (three tests)
| Test | Prediction | Observed |
|---|---|---|
| 1. 1,000 scans of 10 students, no `hashCode` vs fixed | broken ≈ 1,000 entries (identity hash differs for every object), fixed = 10 | broken = 1000, fixed = 10 |
| 2. `contains(equal object)` | broken: false even though `equals` is true; fixed: true | false / true |
| 3. mutate `regNo` after insert (hash uses `regNo`) | `contains(m)` becomes false, and an "equal" new object is added again | before true, after **false**; set size 1 → 2 after adding equal B200 |

### Explanations
* **Why duplicates appear:** `Object.hashCode()` is identity-based, so two equal students (same `regNo`) get different hash codes and land in different buckets; `HashSet` never calls `equals` on them. The contract (*equal objects must have equal hash codes*) is broken.
* **How HashSet locates an element:** (1) compute `hashCode()`, spread it (`h ^ (h>>>16)`), index = (n-1) & hash; (2) in that bucket compare stored hash, then `==` or `equals()`. `equals` is only used among objects that share a bucket, so correctness *depends on* the hash.
* **Mutation after insertion:** the element stays in the bucket for its old hash; lookups compute the new hash, go to another bucket and miss it (Test 3) – the object becomes unreachable (cannot be found or removed; effectively a leak) and a duplicate can be inserted. Hash keys should be immutable (`final` fields – the given class already uses `final`).
* **Correction and verification:** override `hashCode()` using exactly the fields that `equals` uses (`regNo`) – `Objects.hash(regNo)` (or `regNo.hashCode()`). Verify with: set size after duplicate adds, `contains(new equal object)`, and the assertion `a.equals(b) ⇒ a.hashCode()==b.hashCode()`. Note: `name` must **not** be included in the hash because `equals` ignores it.

### Alternative concept and complexity analysis
* `HashSet<Student>`: add/contains O(1) average (O(log n) worst case in Java 8+ with treeified buckets, O(n) before); needs correct `hashCode`; space O(n) + table (load factor 0.75).
* `TreeSet<Student>` with `Comparator.comparing(s -> s.regNo)`: needs **no hashCode**, only a consistent comparator (equality = `compare()==0`); add/contains **O(log n)** (red-black tree); space O(n) with 3 pointers + colour per node (more per entry than HashSet's table slot) but gives sorted iteration.
* `HashMap<String, Student>` keyed by `regNo`: uses `String`'s correct `hashCode/equals`, so the Student class does not even need them; O(1) average, `putIfAbsent(regNo, s)` de-duplicates and also lets you look up the student by ID. Space ≈ HashSet (a `HashSet` *is* a `HashMap` internally) plus the value reference.

---

## Q4 – Choosing a Map for a Pharmacy Inventory (`Task04/Q4Pharmacy.java`)

### Design reasoning
Hot operation: get(code) – hundreds of times a day. Evening: report in code order, and list in insertion order. `HashMap`: O(1) lookup, no order. `LinkedHashMap`: O(1) lookup + insertion order. `TreeMap`: O(log n) lookup + sorted keys. `Hashtable`: legacy synchronised O(1), no order.

### Measurements (average of 5 repetitions after 2 warm-ups; 100,000 random lookups; n = 1,000 … 500,000)
| n | Structure | 100k lookups (ms) | ns/lookup | code-order report (ms) | insertion-order list (ms) |
|---|---|---|---|---|---|
| 1,000 | HashMap | 5.09 | 51 | 1.30 (sorted) | 0.05 |
| | LinkedHashMap | 7.50 | 75 | 0.84 (sorted) | 0.02 |
| | TreeMap | 16.85 | 169 | 0.08 | – |
| | Sorted array + binarySearch | 9.54 | 95 | 0.05 | – |
| 100,000 | HashMap | 21.75 | 218 | 11.79 (sorted) | 3.57 |
| | LinkedHashMap | 5.33 | 53 | 20.60 (sorted) | 6.86 |
| | TreeMap | 39.93 | 399 | 5.29 | – |
| | Hashtable | 4.33 | 43 | 4.25 (sorted) | 3.43 |
| | Sorted array + binarySearch | 11.79 | 118 | 1.40 | – |
| 500,000 | HashMap | 40.09 | 401 | 34.76 (sorted) | 34.98 |
| | LinkedHashMap | 10.82 | 108 | 203.68 (sorted) | 57.53 |
| | TreeMap | 83.64 | 836 | 30.07 | – |
| | Hashtable | 9.22 | 92 | 35.10 (sorted) | 5.48 |
| | Sorted array + binarySearch | 19.61 | 196 | 12.06 | – |

(Full table with the 10,000 row is in `Task04/output.txt`.) Order check: `TreeMap` iterates in code order; `LinkedHashMap` in insertion order (`insertionOrderOK=true`); `HashMap` order is neither (shown in the last lines of the output). `HashMap`/`Hashtable` need an explicit sort for the code-order report. *Caveat:* HashMap vs LinkedHashMap use the same lookup algorithm, so differences like 401 vs 108 ns are measurement noise (JIT, GC, memory layout), not a real 4× gap – treat only the order of magnitude as evidence.

### Acceptability criterion and decision
I define an acceptable lookup as **≤ 1 ms** per request at the interface (a human cannot perceive less than ~100 ms, and 1 ms leaves ample margin for the rest of the system), and ≤ 10 µs inside the program. Every structure meets this by 3–4 orders of magnitude even at 500,000 medicines (≤ ≈0.8 µs), so lookup speed does not discriminate; **the requirements on ordering do**. I would deploy **`LinkedHashMap<String/Integer, Medicine>`** (O(1) lookup + free insertion-order list) and produce the code-order report by sorting the keys once in the evening (O(n log n); 12–35 ms for 100k–500k entries, run once a day). If the code-order report/range queries (e.g. "codes 1000–2000") become frequent, I would switch to `TreeMap` (≈2× slower lookups, still < 1 µs) rather than maintain two maps.
**Where the remaining cost comes from:** hashing/comparing keys, boxing of `Integer` keys, pointer chasing through `Entry` and `Medicine` objects (cache misses grow with n – ns/lookup rises up to ≈8× (HashMap) from 1,000 to 500,000 entries even though complexity is O(1)), and for TreeMap the ≈log₂n = 19 comparisons per lookup.
**Why not Hashtable in new code:** every method is `synchronized` (one lock for the whole table, contention under threads), it is legacy (pre-Collections, uses `Enumeration`), disallows null keys/values, and offers no ordering. It was *not* slow here only because there was no contention. Use `HashMap` for single-threaded code or `ConcurrentHashMap` for concurrent code.

### Alternative: sorted array + `Arrays.binarySearch`
Lookup O(log n); **insertion O(n)** (binary search finds the slot in O(log n) but elements must be shifted); the code-order report is O(n) with no sort (already sorted) – the fastest report in my data (1.4 ms at 100k, 12 ms at 500k); insertion-order list is *not* available unless stored separately. Space: O(n), the most compact (a primitive `int[]` of codes has no per-entry objects). Compared with: HashMap – O(1) lookup, O(1) insert, report needs O(n log n), space O(n) with ≈0.75 load factor overhead; TreeMap – O(log n) for lookup/insert, O(n) sorted report, extra node object per entry. The sorted array is excellent when the inventory is (almost) read-only, poor when stock items are added frequently.

---

## Q5 – Derive a Generic Ranking Component from a Principle (`Task05/Q5Ranking.java`)

### Design (derived before coding)
Principle: the comparison must be a *total preorder consistent with equality*: antisymmetric (`sgn(cmp(a,b)) = −sgn(cmp(b,a))`), transitive, and ties compare as 0. Therefore:
1. **Comparator vs Comparable:** `Team` ranking depends on context (league table: points → goal difference → name; fair-play table; alphabetical list), so I choose **separate `Comparator<Team>` objects**; `Comparable<Team>` would fix a single "natural" order inside the class and would be reused wrongly for other rankings. (A natural order is appropriate only when one order is obvious and permanent – e.g. `String`, `Integer`.) I kept a `Comparable` variant (`RankedTeam`) for the alternative question.
2. **Multi-field comparison:**
   ```java
   Comparator<Team> BY_RANK = Comparator.comparingInt((Team t) -> t.points).reversed()
        .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
        .thenComparing(t -> t.name);          // final tie-breaker => deterministic total order
   ```
   `comparingInt` uses `Integer.compare` (no overflow); `.reversed()` is applied to the single-key comparator **before** `thenComparing` (a trailing `.reversed()` would reverse all keys).
3. **Generic class:** `Leaderboard<T>` stores a `Comparator<? super T>` (a *consumer* of T ⇒ `super`), so a `Comparator<Object>` or a comparator written for a parent type works for `Leaderboard<SubTeam>`. For a natural-order board use a bounded type: `<T extends Comparable<? super T>>` (see `best(...)`).
4. **Wildcard:** `void addAll(List<? extends T> more)` – a *producer* of T: allows adding a `List<Team>` or `List<SubclassOfTeam>` to a `Leaderboard<Team>`; `List<Team>` alone would reject `List<SubTeam>`. `ranking()` returns a new sorted copy (`TODO 1`): `copy = new ArrayList<>(items); copy.sort(order);` leaving `items` untouched.

### Tests
**Data set 1 (ties at every level):** 3 teams tied on points+GD, 2 tied on points with lower GD, 2 tied on points+GD (7,3), 1 tied on points (7,−1), 1 low. Result: `Alpha, Charlie, Delta (10,5) | Bravo, Echo (10,2) | Foxtrot, Golf (7,3) | Hotel (7,−1) | India (3,0)`; identical when the input is reversed; `items` unchanged; comparator consistency check (all pairs/triples) = true.
**Data set 2 (designed to break faulty comparators):** teams with points `Integer.MAX_VALUE`, 0, −2,000,000,000.
* `(a,b) -> b.points - a.points` overflows: `compare(Max, Neg)` returned **+147,483,649** (should be negative) → consistency check **false**; sorting 2,000 random-int teams produced an incorrectly sorted list (for some inputs TimSort throws "Comparison method violates its general contract!").
* Comparator ignoring the tie-breakers: ranking of the tied teams depends on input order (`Delta, Alpha, Charlie…` vs `Echo, Bravo, Charlie, Alpha, Delta…` for reversed input) – not a valid deterministic ranking.

### Correctness argument
`BY_RANK` is a lexicographic composition of three total orders; each key uses `Integer.compare`/`String.compareTo` (no overflow, antisymmetric, transitive) and the last key (name) makes the order total for distinct names; lexicographic composition of total preorders is a total preorder, so sort results are unique and independent of input order (verified).

### Alternatives & complexity
* **`Comparable<Team>`**: same single sort, O(n log n) time, O(n) extra space (TimSort temporary array); but one fixed order; hard to reuse for players/colleges unless each class defines its own natural order.
* **Three stable sorts** (name, then goal difference, then points – least significant key first): produced the **same** ranking (verified) but needs **3 × O(n log n)** time and 3 passes (extra O(n) space each, reused sequentially); must be done in exactly the reverse key order – a common source of bugs. Measured on 200,000 teams: comparator chain **137.5 ms** vs three sorts **232.9 ms** (≈1.7×).
* **Reuse for players and colleges:** the comparator chain – generic `Leaderboard<T>` + any `Comparator<? super T>` – is easier to reuse; Comparable and repeated sorts are tied to Team's fields.

---

## Q6 – Priority, Order and Fairness in a Queue (`Task06/Triage.java`)

**Complete the Code (TODO 1):**
```java
private final PriorityQueue<Patient> queue = new PriorityQueue<>(
    Comparator.comparingInt((Patient p) -> p.severity).thenComparingLong(p -> p.arrival));
```
* **Why a plain Queue is insufficient:** `LinkedList`/`ArrayDeque` are FIFO – the first to arrive is served first regardless of severity. 
* **How the PriorityQueue enforces the rule:** a binary min-heap ordered by the comparator: `poll()` returns the smallest (lowest severity number) element.
* **Arrival order among equal severities:** `PriorityQueue` is *not stable* – equal elements may come out in any order. The `arrival` counter (a unique, increasing sequence number) is the tie-breaker, making the order total.

### Hand-verified instance (8 patients)
Admitted: Anil S3, Bina S1, Chitra S5, Dev S1, Esha S3, Farid S2, Gita S5, Hari S1. Hand-derived order: **Bina, Dev, Hari, Farid, Anil, Esha, Chitra, Gita**. Program output is identical. Printing the queue directly gave `[Bina(S1), Dev(S1), Farid(S2), Hari(S1), Esha(S3), Chitra(S5), Gita(S5), Anil(S3)]`: `toString()`/iterator traverse the **internal heap array**, which only guarantees parent ≤ children, not sorted order; the sorted order is obtained only by repeated `poll()`.

### Growth investigation
Prediction: PriorityQueue add and poll both O(log n) (add average ≈ O(1) for random data, poll needs sift-down, so ≈ log n); sorted LinkedList add O(n), poll O(1); five FIFO queues add O(1), poll O(1) (scan of ≤ 5 queues). Measured (ns per operation):

| Design | op | n=1,000 | n=10,000 | n=100,000 |
|---|---|---|---|---|
| PriorityQueue | add | 424 | 56 | 32 |
| | poll | 1,067 | 872 | 263 |
| Sorted LinkedList | add | 2,400 | 18,785 | (skipped: ≈ O(n²) total) |
| | poll | 29 | 14 | – |
| 5 FIFO queues | add | 46 | 34 | 30 |
| | poll | 754 | 9 | 50 |

(Small-n values include JIT warm-up noise. The clear signals: sorted LinkedList add grows ~8× per 10× n ⇒ O(n); heap and FIFO designs stay flat/near-logarithmic.) The prediction of O(log n) poll is supported only roughly – cache/JIT effects dominate at this scale; poll > add as predicted.

### Starvation and a fair rule
With strict priority a severity-5 patient is served only when no severity 1–4 patient is waiting; if severe patients keep arriving faster than they are served the S5 patient never reaches the head of the heap. Demo: 1 minor patient then 2 critical arrivals per tick and 1 served per tick: strict priority → **never served in 1,000 ticks** (−1); with aging → served at tick **40**. Fair rule used: key = `arrival + W·(severity−1)` (W = 10 ticks per severity level): this is a *static* key, so the heap needs no re-ordering and stays O(log n), and a patient can only be overtaken by patients who arrived less than 4·W ticks after it, so waiting time is bounded. A critical case still overtakes (arrival earlier by 10–40 ticks equivalent). (Alternative rule: periodically promote patients who waited > T by removing and re-inserting them – O(n) per sweep.) W is a clinical policy parameter.

### Deployment decision
A whole simulated day of 5,000 admits + 5,000 serves took ≈ 34.5 ms (≈ 7 µs per patient including object creation and JIT warm-up) – so the design easily serves a city hospital of 5,000 patients/day; even a 1,000× larger load would be fine. Remaining production concerns are not speed: thread-safety (use `PriorityBlockingQueue` or a lock), persistence and the aging policy.

### Alternative concept and complexity
* **Sorted `LinkedList`:** admit O(n) (scan to the insertion point), serve O(1); space O(n) with a node per element; simple but admit cost grows linearly (measured 8× from 1k to 10k).
* **Array of five FIFO queues:** admit O(1), serve O(1) (scan 5 queues – constant, K=5); space O(n); fastest and naturally FIFO within severity, but works only for a small fixed set of severities and makes aging awkward.
* **PriorityQueue:** admit O(log n), serve O(log n), space O(n) array; general (any priority/aging key). Since K=5 is tiny, the FIFO-array design is a legitimate, faster choice – I would use PriorityQueue when priorities are many or computed.

---

## Q7 – Diagnose, Redesign and Validate (`Task07/Q7Redesign.java`)
(The slides cut off the description of steps C–E; I followed the headings visible: A Diagnose, B Objective, C Design, then implement/compare, F Defend.)

Selected solution: **Q1 – e-ticket generation using `+` in a loop.**

### A. Diagnose the bottleneck (2 marks)
Q1 evidence: at 50 tickets × 2,000 fields `+` took 116 ms and **allocated 1.72 GB**, versus 1.6 ms and 5.3 MB for `StringBuilder`; time per field grows with ticket length ⇒ quadratic. Root cause: String immutability ⇒ each `s += x` allocates a new String and copies all previous characters (O(n²) characters copied); in addition `key + "=" + value + ";"` creates a temporary per field. The time is spent in array copying and in the allocator/GC, not in the actual logic.

### B. Design objective (2 marks)
For 10,000 × 50 fields **and** 50 × 2,000 fields: (1) allocate ≥ 5× fewer bytes than the original; (2) be at least as fast as the original on short tickets and ≥ 10× faster on long tickets; (3) produce byte-identical tickets (correctness preserved); (4) keep O(n) time per ticket.

### C. Redesign (4 marks) – not just "swap the class"
1. Replace repeated concatenation by appending to a single buffer (O(n) instead of O(n²)).
2. Remove per-field temporaries: `append(key).append('=').append(value).append(';')` instead of `append(key + "=" + value + ";")`.
3. Pre-size the buffer (`new StringBuilder(fields * 20)`) so it never regrows while building.
4. Reuse one `TicketWriter` per worker thread (`setLength(0)`), so per-ticket allocation is just the final `toString()`; thread-confined ⇒ no locking.
Predicted: allocation ≈ size of the ticket text only (≈ 25× less for 50 fields); time at least similar for short tickets, far faster for long.

### D/E. Implementation and comparison (same test conditions; avg of repeated runs after warm-up; allocated bytes via ThreadMXBean)
| Workload | Version | avg ms | allocated MB | vs original |
|---|---|---|---|---|
| 10,000 × 50 | Original (+) | 15.42 | 160.29 | 1.0× |
| | StringBuilder + temporaries | 13.17 | 44.33 | 1.2× faster, 3.6× less memory |
| | **Redesign** | 18.01 | **5.95** | 0.9× (not faster), **26.9× less memory** |
| 50 × 2,000 | Original (+) | 83.16 | 1,247.44 | 1.0× |
| | StringBuilder + temporaries | 1.89 | 7.85 | 44× faster, 159× less |
| | **Redesign** | 1.38 | **1.27** | **60× faster, ≈ 980× less memory** |

Identical output was verified with `equals` for all three versions on both workloads.

### F. Defend the final design (2 marks)
* **Prediction supported?** Memory and long-ticket predictions: yes. The short-ticket speed prediction ("at least as fast") was **not** supported: 18.0 ms vs 15.4 ms. The difference is within run-to-run variation on this machine (the JIT compiles the simple `+` loop for 50 fields very well, and 50 fields means the O(n²) copying is only ~25 KB per ticket), so I conclude *no measurable speed gain for short tickets*, not that the redesign is slower; repeated runs on your machine may differ.
* **Magnitude:** up to 60× faster and ≈ 980× less allocation on long tickets; 27× less allocation on short tickets (less GC pressure under load, which matters for a booking server even when single-ticket time is similar).
* **New costs/trade-offs:** a mutable writer must be confined to one thread (or each thread gets its own, e.g. `ThreadLocal`/per-worker field); a pre-size guess wastes memory when wrong; slightly less readable than `+`; `String.join` is an equally good, simpler alternative when the fields are already in an array/collection (lowest allocation in Q1).
* **When the original is still preferable:** a handful of short fields built once (e.g. one ticket on request) – the compiler/JIT already optimises single-expression concatenation and readability wins; premature optimisation is not justified.
* **Conclusion:** keep `+` for small, one-off strings; use the pre-sized per-thread `StringBuilder` (or `String.join`) for loops and high-volume generation.
