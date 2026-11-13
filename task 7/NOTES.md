# Task 7 – Override and Specialise Behaviour
`ratePerHour()` / `category()` overridden in every subclass; `EBike` overrides `calculateFee` (flat Rs 20). Every override uses `@Override`. `final` on `regNo` and the slots array. `toString`, `equals`, `hashCode` use the registration number, so the same plate is equal across subclasses.
Alternative: one switch on a type field -> every new type edits shared code; overriding needs 0 existing classes changed.
Run: `Task07Demo`.
