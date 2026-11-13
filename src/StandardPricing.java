package parking.service;
import parking.model.*;
import java.time.*;

/** Daily cap Rs 200 per started day; first-hour/extra-hour slabs come from the vehicle's rate. */
public class StandardPricing implements PricingRule {
    public static final double DAILY_MAX = 200;
    @Override public double fee(Vehicle v, LocalDateTime in, LocalDateTime out) {
        long totalMin = Duration.between(in, out).toMinutes();
        if (totalMin < 0) throw new IllegalArgumentException("Exit before entry");
        if (totalMin == 0) return 0;
        // same-day, clock-aware stay -> use the vehicle's own (possibly peak-aware) logic
        if (in.toLocalDate().equals(out.toLocalDate()))
            return Math.min(v.calculateFee(in.toLocalTime(), out.toLocalTime()), DAILY_MAX);
        long days = totalMin / (24 * 60);
        int remMin = (int) (totalMin % (24 * 60));
        double fee = days * DAILY_MAX;
        if (remMin > 0) fee += Math.min(v.calculateFee(remMin), DAILY_MAX);
        return fee;
    }
}
