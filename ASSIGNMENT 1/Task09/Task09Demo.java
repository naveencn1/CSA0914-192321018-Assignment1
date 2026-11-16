import parking.model.*;
import java.util.*;

/** Task 9: abstract class + interface (default method, multiple behaviours). */
public class Task09Demo {
    public static void main(String[] args) {
        // Vehicle v = new Vehicle("X");   // compile error: Vehicle is abstract
        List<Chargeable> chargers = List.of(new ElectricCar("EV1"), new EBike("EB1"));
        for (Chargeable c : chargers)
            System.out.println(c.getClass().getSimpleName() + " 10 kWh -> Rs " + c.chargingFee(10));

        try { chargers.get(0).chargingFee(-5); }
        catch (IllegalArgumentException e) { System.out.println("default method checkKwh rejects bad input: " + e.getMessage()); }

        Pass none = Pass.none();   // static interface method
        System.out.println("Pass.none() covers any vehicle? " + none.coversVehicle(new Car("C1"), java.time.LocalDate.now()));
        System.out.println("ElectricCar is a Car (class inheritance) and Chargeable (interface): two behaviours, one object.");
    }
}
