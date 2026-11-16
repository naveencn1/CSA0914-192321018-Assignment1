import parking.model.*;
import parking.service.*;
import java.time.*;

/** Task 12: changed requirements - new categories, passes, 5 floors with different sizes. */
public class Task12Demo {
    static int pass = 0, fail = 0;
    static void check(String name, Object expected, Object actual) {
        boolean ok = expected.equals(actual);
        if (ok) pass++; else fail++;
        System.out.printf("%-4s | %-46s | expected=%-8s actual=%-8s%n", ok ? "PASS" : "FAIL", name, expected, actual);
    }
    static LocalDateTime t(int h, int m) { return LocalDateTime.of(2026, 11, 12, h, m); }

    public static void main(String[] args) {
        ParkingLot n = new ParkingLot(new int[]{10, 20, 30, 40, 50}, new StandardPricing());   // jagged: 5 uneven floors
        n.addPass(new MonthlyPass("STAFF1", LocalDate.of(2026, 11, 30)));

        n.enter(new EBike("EB1"), t(10, 0));
        check("e-bike flat Rs 20 (3h)", 20.0, n.exit("EB1", t(13, 0)));
        n.enter(new DeliveryVan("VAN1"), t(10, 0));
        check("van 2h peak = 2 x 60", 120.0, n.exit("VAN1", t(12, 0)));
        n.enter(new DeliveryVan("VAN2"), t(16, 0));
        check("van 16:00-18:00 (1 peak + 1 off-peak)", 90.0, n.exit("VAN2", t(18, 0)));
        n.enter(new DeliveryVan("VAN3"), t(19, 0));
        check("van off-peak 1h = car rate", 30.0, n.exit("VAN3", t(20, 0)));
        n.enter(new Car("STAFF1"), t(8, 0));
        check("monthly-pass holder parks free", 0.0, n.exit("STAFF1", t(17, 0)));
        n.addPass(new DayPass("VIS1", LocalDate.of(2026, 11, 12)));
        n.addToCollection(DayPass.PRICE);
        n.enter(new Car("VIS1"), t(9, 0));
        check("one-day pass visitor parks free", 0.0, n.exit("VIS1", t(15, 0)));
        check("collection incl. Rs 100 day-pass sale", 360.0, n.totalCollection());

        System.out.println("\nRESULT: " + pass + " passed, " + fail + " failed");
    }
}
