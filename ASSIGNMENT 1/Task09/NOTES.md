# Task 9 – Abstraction
`Vehicle` is abstract (no rate for a generic vehicle); `Chargeable` is an interface (capability shared by unrelated classes; a class may implement many interfaces but extend one class). Interface default method `checkKwh` and static method `Pass.none()`.
Alternative (Vehicle as interface, Chargeable as abstract class): interface cannot hold the regNo field/constructor; ElectricCar could no longer extend two classes.
Run: `Task09Demo`.
