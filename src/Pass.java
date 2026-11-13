package parking.model;
/** Task 12: pass abstraction; MonthlyPass and DayPass implement it. */
public interface Pass {
    boolean coversVehicle(Vehicle v, java.time.LocalDate on);
    static Pass none() { return (v, d) -> false; }
}
