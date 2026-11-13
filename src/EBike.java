package parking.model;
/** Task 12: flat fee regardless of duration. */
public class EBike extends Vehicle implements Chargeable {
    public EBike(String regNo) { super(regNo); }
    @Override protected double ratePerHour() { return 5; }
    @Override public String category() { return "EBIKE"; }
    @Override public double calculateFee(int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("Minutes cannot be negative");
        return minutes == 0 ? 0 : 20;             // flat Rs 20 per visit
    }
    @Override public double chargingFee(int kWh) { checkKwh(kWh); return kWh * RATE_PER_KWH; }
}
