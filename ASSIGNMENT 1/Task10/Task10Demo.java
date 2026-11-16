import parking.model.*;
import parking.service.*;
import java.time.*;

/** Task 10: encapsulation, validation, wrapper-class pitfall. */
public class Task10Demo {
    static void tryIt(String label, Runnable r) {
        try { r.run(); System.out.println(label + " -> accepted"); }
        catch (RuntimeException e) { System.out.println(label + " -> rejected: " + e.getMessage()); }
    }
    public static void main(String[] args) {
        LocalDateTime t0 = LocalDateTime.of(2026, 11, 12, 8, 0);
        tryIt("blank registration       ", () -> new Car(" "));
        tryIt("negative minutes         ", () -> new Car("C1").calculateFee(-10));
        tryIt("negative kWh             ", () -> new ElectricCar("E1").chargingFee(-1));
        tryIt("ticket with floor = -1   ", () -> new ParkingTicket(new Car("C2"), -1, 0, t0));
        tryIt("exit before entry        ", () -> { ParkingTicket t = new ParkingTicket(new Car("C3"), 0, 0, t0); t.close(t0.minusHours(1)); });
        System.out.println("release(9,9) on a lot -> " + new ParkingLot(new int[]{2}, new StandardPricing()).release(9, 9));

        Integer a = 1000, b = 1000;
        System.out.println("\nInteger a = 1000, b = 1000");
        System.out.println("a == b      -> " + (a == b) + "   (compares references - pitfall)");
        System.out.println("a.equals(b) -> " + a.equals(b) + "   (compares values - correct)");
    }
}
