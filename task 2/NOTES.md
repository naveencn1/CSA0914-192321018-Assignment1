# Task 2 – Choose the Data Representation
int for floor/slot; String for registration; LocalDateTime for times; int minutes for duration; double for fee.
Floors/slots: **jagged array** (`Vehicle[floors][]`) because floors differ in size.
Casting risk: `minutes / 60` (int division) gives 2 hours for 130 min -> Rs 60 (wrong). Fix: `(minutes + 59) / 60` -> 3 hours -> Rs 90.
Alternative: 1-D array with `index = floor*slotsPerFloor + slot` only works for equal-size floors; ArrayList<String[]> grows dynamically. Locate slot O(1) in all.
Run: `Task02Demo`.
