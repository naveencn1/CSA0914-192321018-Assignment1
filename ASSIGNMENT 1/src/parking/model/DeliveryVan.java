package parking.model;
/** Task 12: double the car rate between 9 AM and 5 PM. */
public class DeliveryVan extends Vehicle {
    public static final double CAR_RATE = 30;
    public DeliveryVan(String regNo) { super(regNo); }
    @Override protected double ratePerHour() { return CAR_RATE; }
    @Override public String category() { return "VAN"; }
    /** Hour-by-hour: each started hour priced by the clock hour it starts in. */
    @Override public double calculateFee(java.time.LocalTime in, java.time.LocalTime out) {
        int minutes = (int) java.time.Duration.between(in, out).toMinutes();
        int hours = (minutes + 59) / 60;
        double fee = 0;
        for (int i = 0; i < hours; i++) {
            int startHour = (in.getHour() + i) % 24;
            boolean peak = startHour >= 9 && startHour < 17;
            fee += peak ? 2 * CAR_RATE : CAR_RATE;
        }
        return fee;
    }
}
