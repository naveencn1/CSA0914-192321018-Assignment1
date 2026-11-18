import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Fake repository for demo and tests (no database needed). */
public class InMemoryFeeRepository implements FeeRepository {
    private final Map<String, BigDecimal> balances = new HashMap<>();
    private long nextReceipt = 1001;

    public InMemoryFeeRepository() {
        balances.put("STU2024001", new BigDecimal("50000.00"));
        balances.put("STU2024002", new BigDecimal("12500.50"));
    }

    @Override public synchronized Optional<BigDecimal> findBalance(String id) { return Optional.ofNullable(balances.get(id)); }

    @Override
    public synchronized long recordPayment(String id, BigDecimal amount, String mode, LocalDateTime when) {
        balances.merge(id, amount.negate(), BigDecimal::add);
        return nextReceipt++;
    }
}
