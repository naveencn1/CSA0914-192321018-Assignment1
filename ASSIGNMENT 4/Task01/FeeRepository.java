import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/** Data-access boundary. The service depends on this interface only, so the database can be swapped or faked. */
public interface FeeRepository {
    /** Outstanding balance for the student, or empty if the student does not exist. */
    Optional<BigDecimal> findBalance(String studentId);

    /** Records the payment and reduces the balance atomically. Returns the new receipt number. */
    long recordPayment(String studentId, BigDecimal amount, String mode, LocalDateTime when);
}
