# Task 11 – Implement, Test and Defend
Run: `Task11Tests` (15 tests: normal, boundary, invalid, equality, hand-calculated total 20+120+30 = 170).
Limitation: slots are not typed; allocation is a linear scan.
Alternative: HashMap keyed by registration gives O(1) exit lookup vs O(n) scan for array/ArrayList (see Benchmarks).
