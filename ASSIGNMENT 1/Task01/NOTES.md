# Task 1 – Understand the Problem and Identify the Objects
**Inputs:** registration number, vehicle category, entry/exit time, pass details, floor/slot layout. **Outputs:** allocated {floor, slot}, ticket with unique number, fee on exit, day collection.

| Candidate class | Responsibility |
|---|---|
| Vehicle (+ subclasses) | identity and rate; fee differs by type |
| ParkingTicket | immutable record of one stay |
| ParkingLot | slot grid, allocate/release, active tickets |
| PricingRule | all fee policy in one place |
| Pass | free-parking entitlement |

**Business rules:** one vehicle per slot; ticket numbers never reused; no duplicate registration; exit not before entry; zero-minute stay free; every started hour charged; pass holders free; daily max Rs 200.
**Assumptions:** one vehicle per slot, any free slot fits any vehicle, local times, case-insensitive registration.
**Alternative (parallel String arrays):** search O(n) vs O(1) average with HashMap; new attribute = new array vs one new field.
Run: `Task01Demo`. Full write-up in the report.
