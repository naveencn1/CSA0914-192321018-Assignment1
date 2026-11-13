package parking.model;

/** Abstract superclass of every vehicle (Tasks 6, 7, 9, 10). */
public abstract class Vehicle {
    private final String regNo;

    protected Vehicle(String regNo) {
        if (regNo == null || regNo.isBlank())
            throw new IllegalArgumentException("Registration number required");
        this.regNo = regNo.trim().toUpperCase();
    }

    public String getRegNo() { return regNo; }

    /** Rate per started hour. Subclasses specialise this. */
    protected abstract double ratePerHour();

    /** Category label used by pricing rules and reports. */
    public abstract String category();

    /** Overloaded (compile-time polymorphism): minutes version. */
    public double calculateFee(int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("Minutes cannot be negative");
        int hours = (minutes + 59) / 60;          // ceiling using integer arithmetic only
        return hours * ratePerHour();
    }

    /** Overloaded: clock-time version (same-day stay). */
    public double calculateFee(java.time.LocalTime in, java.time.LocalTime out) {
        int minutes = (int) java.time.Duration.between(in, out).toMinutes();
        return calculateFee(minutes);
    }

    @Override public boolean equals(Object o) {
        return o instanceof Vehicle v && regNo.equals(v.regNo);
    }
    @Override public int hashCode() { return regNo.hashCode(); }
    @Override public String toString() { return category() + "[" + regNo + "]"; }
}
