import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Business logic: validation, rules, receipt text. No Swing, no SQL. */
public class FeeService {
    public static final List<String> MODES = List.of("Cash", "Card", "UPI");
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9]{6,12}");
    private final FeeRepository repository;

    public FeeService(FeeRepository repository) { this.repository = repository; }

    /** Validates everything, then records the payment. Throws ValidationException or DataAccessException. */
    public PaymentResult pay(PaymentRequest req) {
        List<String> problems = new ArrayList<>();
        String id = req.studentId() == null ? "" : req.studentId().trim();
        BigDecimal amount = null;

        if (!ID.matcher(id).matches()) problems.add("Student ID must be 6-12 letters or digits.");
        if (req.mode() == null || !MODES.contains(req.mode())) problems.add("Choose a payment mode (Cash, Card or UPI).");
        try {
            amount = new BigDecimal(req.amountText() == null ? "" : req.amountText().trim());
            if (amount.signum() <= 0) { problems.add("Amount must be greater than zero."); amount = null; }
            else if (amount.scale() > 2) { problems.add("Amount can have at most 2 decimal places."); amount = null; }
        } catch (NumberFormatException e) {
            problems.add("Amount must be a number, e.g. 2500 or 2500.50.");
        }
        if (!problems.isEmpty()) throw new ValidationException(problems);

        BigDecimal balance = repository.findBalance(id)
                .orElseThrow(() -> new ValidationException(List.of("No fee account found for " + id + ".")));
        if (amount.compareTo(balance) > 0) {
            throw new ValidationException(List.of("Amount " + amount + " exceeds the outstanding balance " + balance + "."));
        }
        LocalDateTime now = LocalDateTime.now();
        long receiptNo = repository.recordPayment(id, amount, req.mode(), now);
        return new PaymentResult(receiptNo, id, amount, req.mode(), balance.subtract(amount), now);
    }

    public String receiptText(PaymentResult r) {
        return String.format("FEE RECEIPT%nReceipt No : %d%nStudent ID : %s%nAmount     : %s%nMode       : %s%n"
                + "Balance    : %s%nDate       : %s", r.receiptNo(), r.studentId(), r.amount().toPlainString(),
                r.mode(), r.balanceAfter().toPlainString(), r.paidAt().withNano(0));
    }
}
