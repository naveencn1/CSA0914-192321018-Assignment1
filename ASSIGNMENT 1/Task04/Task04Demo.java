import parking.model.*;
import java.time.LocalDateTime;

/** Task 4: constructors, this, copy constructor, references. */
public class Task04Demo {
    public static void main(String[] args) {
        ParkingTicket.resetCounterForTests();
        Vehicle car = new Car("TN02B2");
        LocalDateTime in = LocalDateTime.of(2026, 11, 12, 8, 0);

        ParkingTicket t1 = new ParkingTicket(car, 0, 0, in);          // parameterised constructor
        ParkingTicket copy = new ParkingTicket(t1);                    // copy constructor
        ParkingTicket t2 = new ParkingTicket(new Car("TN05E5"), 0, 1, in);

        System.out.println("t1   : " + t1);
        System.out.println("copy : " + copy + "   (same ticket number: duplicate print)");
        System.out.println("t2   : " + t2 + "   (new ticket gets the next number)");
        System.out.println("t1 == copy ? " + (t1 == copy) + "   (different objects on the heap)");
        System.out.println("t1.getVehicle() == copy.getVehicle() ? " + (t1.getVehicle() == copy.getVehicle())
                + "   (same Vehicle referenced)");

        try { new ParkingTicket(null, 0, 0, in); }
        catch (IllegalArgumentException e) { System.out.println("Invalid object rejected by constructor: " + e.getMessage()); }
    }
}
