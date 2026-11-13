package parking.service;
import parking.model.*;
import java.time.*;

/** Task 12: all pricing decisions live behind one abstraction. */
public interface PricingRule {
    double fee(Vehicle v, LocalDateTime in, LocalDateTime out);
}
