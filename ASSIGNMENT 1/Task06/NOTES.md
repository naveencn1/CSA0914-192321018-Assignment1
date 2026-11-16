# Task 6 – Inheritance Hierarchy
```
        <<interface>> Chargeable            Vehicle (abstract)
              ^   ^                        /    |      |      \
              |   +------ EBike    TwoWheeler  Car   EBike  DeliveryVan
              |                                |
              +----------------------- ElectricCar (extends Car)
```
Type: hierarchical (Vehicle -> several subclasses) + multilevel (Vehicle -> Car -> ElectricCar).
Constructor order for `new ElectricCar(..)`: ElectricCar called -> Car called -> Vehicle body -> Car body -> ElectricCar body.
Alternative: composition (Vehicle has-a VehicleType): new type = new data row; fee O(1) in both; one extra reference per object.
Run: `Task06Demo`.
