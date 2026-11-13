# Task 10 – Packages and Access Modifiers
Packages: `parking.model`, `parking.service`. Fields private; `ratePerHour()` protected; public API minimal. Validation: blank registration, negative minutes/kWh, invalid slot, exit before entry. Wrapper pitfall: `Integer == Integer` compares references; use `equals`.
Alternative: public fields + caller validation repeats the same check in many places and allows invalid state.
Run: `Task10Demo`.
