import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Outcome of a successful payment. */
public record PaymentResult(long receiptNo, String studentId, BigDecimal amount, String mode,
                            BigDecimal balanceAfter, LocalDateTime paidAt) { }
