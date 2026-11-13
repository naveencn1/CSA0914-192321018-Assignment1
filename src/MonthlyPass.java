package parking.model;
import java.time.LocalDate;
public class MonthlyPass implements Pass {
    private final String regNo; private final LocalDate validTill;
    public MonthlyPass(String regNo, LocalDate validTill) { this.regNo = regNo.toUpperCase(); this.validTill = validTill; }
    @Override public boolean coversVehicle(Vehicle v, LocalDate on) {
        return v.getRegNo().equals(regNo) && !on.isAfter(validTill);
    }
}
