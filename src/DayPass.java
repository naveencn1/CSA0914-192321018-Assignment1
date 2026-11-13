package parking.model;
import java.time.LocalDate;
/** Task 12: visitor one-day pass (flat price paid on purchase; parking then free that day). */
public class DayPass implements Pass {
    public static final double PRICE = 100;
    private final String regNo; private final LocalDate day;
    public DayPass(String regNo, LocalDate day) { this.regNo = regNo.toUpperCase(); this.day = day; }
    @Override public boolean coversVehicle(Vehicle v, LocalDate on) {
        return v.getRegNo().equals(regNo) && on.equals(day);
    }
}
