import parking.model.*;
import parking.service.*;
import java.time.*;

/** Task 11: normal, boundary and invalid cases plus the hand-calculated day total. */
public class Task11Tests {
    static int pass = 0, fail = 0;
    static void check(String name, Object expected, Object actual) {
        boolean ok = expected.equals(actual);
        if (ok) pass++; else fail++;
        System.out.printf("%-4s | %-46s | expected=%-8s actual=%-8s%n", ok ? "PASS" : "FAIL", name, expected, actual);
    }
    static LocalDateTime t(int h, int m) { return LocalDateTime.of(2026, 11, 12, h, m); }

    public static void main(String[] args) {
        System.out.println("=== Normal operation ===");
        ParkingLot lot = new ParkingLot(new int[]{2, 1}, new StandardPricing());
        lot.enter(new TwoWheeler("TN01A1"), t(8, 0));
        lot.enter(new Car("TN02B2"), t(8, 0));
        lot.enter(new ElectricCar("TN03C3"), t(8, 0));
        check("two-wheeler 2h", 20.0, lot.exit("TN01A1", t(10, 0)));
        check("car 2h10m (3 started hours)", 90.0, lot.exit("TN02B2", t(10, 10)));
        check("EV parking 1h", 30.0, lot.exit("TN03C3", t(9, 0)));

        System.out.println("=== Boundary cases ===");
        ParkingLot b = new ParkingLot(new int[]{1}, new StandardPricing());
        b.enter(new Car("TN10X1"), t(8, 0));
        boolean full = false;
        try { b.enter(new Car("TN10X2"), t(8, 0)); } catch (ParkingException e) { full = true; }
        check("last slot taken -> next entry rejected", true, full);
        check("zero-minute stay is free", 0.0, b.exit("TN10X1", t(8, 0)));
        b.enter(new Car("TN11"), t(8, 0));
        check("exactly 60 min = 1 hour", 30.0, b.exit("TN11", t(9, 0)));
        b.enter(new Car("TN12"), t(8, 0));
        check("61 min = 2 hours (slab boundary)", 60.0, b.exit("TN12", t(9, 1)));
        b.enter(new Car("TN13"), t(0, 0));
        check("daily cap Rs 200", 200.0, b.exit("TN13", t(23, 59)));

        System.out.println("=== Invalid cases ===");
        ParkingLot v = new ParkingLot(new int[]{3}, new StandardPricing());
        v.enter(new Car("TN20"), t(8, 0));
        boolean dup = false;
        try { v.enter(new Car("tn20"), t(8, 5)); } catch (ParkingException e) { dup = true; }
        check("duplicate registration rejected", true, dup);
        check("invalid slot release(9,9)", false, v.release(9, 9));
        check("release already-free slot", false, v.release(0, 2));
        boolean noTicket = false;
        try { v.exit("UNKNOWN", t(9, 0)); } catch (ParkingException e) { noTicket = true; }
        check("exit unknown vehicle rejected", true, noTicket);
        boolean badReg = false;
        try { new Car("  "); } catch (IllegalArgumentException e) { badReg = true; }
        check("blank registration rejected", true, badReg);

        System.out.println("=== Equality (Task 7) ===");
        check("same regNo => equal across subclasses", true, new Car("TN02B2").equals(new ElectricCar("TN02B2")));

        System.out.println("=== Hand-calculated day total ===");
        ParkingLot d = new ParkingLot(new int[]{3, 3}, new StandardPricing());
        d.enter(new TwoWheeler("A"), t(9, 0));   // 2h -> 20
        d.enter(new Car("B"), t(9, 0));           // 3h10 -> 4h -> 120
        d.enter(new ElectricCar("C"), t(9, 0));   // 1h -> 30
        d.exit("A", t(11, 0)); d.exit("B", t(12, 10)); d.exit("C", t(10, 0));
        check("day total (20+120+30 by hand)", 170.0, d.totalCollection());

        System.out.println("\nRESULT: " + pass + " passed, " + fail + " failed");
    }
}
