package parking.model;

/** Capability: a vehicle that can be charged at a charging slot (Task 9). */
public interface Chargeable {
    double RATE_PER_KWH = 12.0;
    double chargingFee(int kWh);

    /** default method: shared validation reused by implementers. */
    default void checkKwh(int kWh) {
        if (kWh < 0) throw new IllegalArgumentException("kWh cannot be negative");
    }
}
