import parking.model.*;
import java.util.*;

/** Task 7: overriding toString/equals/hashCode and category-specific fees. */
public class Task07Demo {
    public static void main(String[] args) {
        Vehicle a = new Car("TN02B2");
        Vehicle b = new ElectricCar("tn02b2");
        System.out.println("a.toString() = " + a + "   b.toString() = " + b);
        System.out.println("a.equals(b)  = " + a.equals(b) + "   (same registration number)");
        System.out.println("hashCodes equal = " + (a.hashCode() == b.hashCode()));

        Set<Vehicle> set = new HashSet<>(List.of(a));
        System.out.println("HashSet contains b? " + set.contains(b) + "   (equals + hashCode agree)");

        System.out.println("\nOverridden calculateFee(130 minutes):");
        Vehicle[] vs = { new TwoWheeler("A1"), new Car("B2"), new EBike("E3") };
        for (Vehicle v : vs) System.out.println("  " + v + " -> Rs " + v.calculateFee(130));
        System.out.println("EBike overrides calculateFee to a flat Rs 20; the others use the inherited slab with their own rate.");
    }
}
