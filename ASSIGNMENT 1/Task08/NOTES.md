# Task 8 – Polymorphism (+ Complete the Code)
Run `FeeDemo` (expected output: Rs 30.0 / 90.0 / 210.0 / true) and `DispatchDemo` (overloads + dynamic dispatch).
Trace: declared type Vehicle, actual object Car -> JVM calls Car.ratePerHour() = 30 -> 3 hours x 30 = Rs 90.
Last line `true`: equals compares only regNo, so Car("TN02B2") equals ElectricCar("TN02B2"); correct for this system because a plate cannot be parked twice.
Alternative: instanceof chains cost up to k checks per call vs O(1) dispatch.
