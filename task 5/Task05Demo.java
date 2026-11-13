import parking.model.*;
import parking.service.*;
import java.time.LocalDateTime;

/** Task 5: static counter, two facilities, passing objects to methods. */
public class Task05Demo {
    static void closeTicket(ParkingTicket t, LocalDateTime out) { t.close(out); }   // reference is passed

    public static void main(String[] args) {
        ParkingTicket.resetCounterForTests();
        LocalDateTime in = LocalDateTime.of(2026, 11, 12, 8, 0);
        ParkingLot north = new ParkingLot(new int[]{2}, new StandardPricing());
        ParkingLot south = new ParkingLot(new int[]{2}, new StandardPricing());

        System.out.println("North gate: " + north.enter(new Car("N1"), in));
        System.out.println("South gate: " + south.enter(new Car("S1"), in));
        System.out.println("North gate: " + north.enter(new Car("N2"), in));
        System.out.println("-> ticket numbers 1,2,3 are unique across both gates because the counter is static.");

        ParkingTicket t = new ParkingTicket(new Car("X"), 0, 0, in);
        closeTicket(t, in.plusHours(1));
        System.out.println("\nAfter closeTicket(t): exit time seen by caller = " + t.getExit() + "  (object reference passed)");
        t = null;   // no reference left -> ticket eligible for garbage collection
        System.out.println("t set to null -> the ParkingTicket object is now eligible for garbage collection.");
    }
}
