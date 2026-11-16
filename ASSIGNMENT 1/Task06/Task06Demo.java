/** Task 6: constructor execution order trace for ElectricCar. */
public class Task06Demo {
    static class Vehicle {
        Vehicle(String regNo) { System.out.println("  3) Vehicle constructor body  (regNo = " + regNo + ")"); }
    }
    static class Car extends Vehicle {
        Car(String regNo) { super(regNo); System.out.println("  4) Car constructor body"); }
    }
    static class ElectricCar extends Car {
        ElectricCar(String regNo) { super(regNo); System.out.println("  5) ElectricCar constructor body"); }
    }
    public static void main(String[] args) {
        System.out.println("new ElectricCar(\"TN03C3\")");
        System.out.println("  1) ElectricCar(String) called -> first line super(regNo)");
        System.out.println("  2) Car(String) called         -> first line super(regNo)");
        new ElectricCar("TN03C3");
        System.out.println("Parent constructors always finish before the child body runs.");
    }
}
