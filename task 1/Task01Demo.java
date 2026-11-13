import parking.model.*;
import java.util.*;

/** Task 1: objects vs. parallel String arrays (alternative concept). */
public class Task01Demo {
    public static void main(String[] args) {
        // Object design
        Map<String, Vehicle> byReg = new HashMap<>();
        byReg.put("TN01A1", new TwoWheeler("TN01A1"));
        byReg.put("TN02B2", new Car("TN02B2"));
        byReg.put("TN03C3", new ElectricCar("TN03C3"));
        System.out.println("Object design  : find TN02B2 -> " + byReg.get("TN02B2") + "  (HashMap, O(1) average)");

        // Parallel-array design
        String[] regNo = {"TN01A1", "TN02B2", "TN03C3"};
        String[] type = {"TWO_WHEELER", "CAR", "EV"};
        String[] entry = {"08:00", "08:00", "08:00"};
        int idx = -1;
        for (int i = 0; i < regNo.length; i++) if (regNo[i].equals("TN02B2")) { idx = i; break; }
        System.out.println("Parallel arrays: find TN02B2 -> " + type[idx] + " entered " + entry[idx] + "  (linear scan, O(n))");
        System.out.println("Adding a new attribute needs a 4th array and edits to every insert/remove; with objects it is one new field.");

        System.out.println("\nCandidate classes: Vehicle(+subclasses), ParkingTicket, ParkingLot, PricingRule, Pass");
        System.out.println("Business rules  : one vehicle per slot, unique ticket numbers, no duplicate registration");
    }
}
