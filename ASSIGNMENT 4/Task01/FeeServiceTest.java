import java.math.BigDecimal;

public class FeeServiceTest {
    static int failed = 0;
    static void check(boolean ok, String n) { System.out.println((ok ? "PASS " : "FAIL ") + n); if (!ok) failed++; }
    static boolean invalid(FeeService s, String id, String amt, String mode, String expectedPart) {
        try { s.pay(new PaymentRequest(id, amt, mode)); return false; }
        catch (ValidationException e) { return e.getMessage().contains(expectedPart); }
    }

    public static void main(String[] a) {
        FeeService s = new FeeService(new InMemoryFeeRepository());
        PaymentResult r = s.pay(new PaymentRequest("STU2024001", "2500.50", "UPI"));
        check(r.balanceAfter().compareTo(new BigDecimal("47499.50")) == 0, "valid payment reduces balance to 47499.50");
        check(r.receiptNo() == 1001, "first receipt number is 1001");
        check(s.receiptText(r).contains("STU2024001"), "receipt text contains student id");
        check(invalid(s, "ab", "100", "Cash", "6-12"), "short id rejected");
        check(invalid(s, "STU2024001", "abc", "Cash", "must be a number"), "non-numeric amount rejected");
        check(invalid(s, "STU2024001", "-10", "Cash", "greater than zero"), "negative amount rejected");
        check(invalid(s, "STU2024001", "10.123", "Cash", "2 decimal"), "3 decimals rejected");
        check(invalid(s, "STU2024001", "100", "Bitcoin", "payment mode"), "unknown mode rejected");
        check(invalid(s, "NOSUCH123", "100", "Cash", "No fee account"), "unknown student rejected");
        check(invalid(s, "STU2024002", "99999", "Cash", "exceeds"), "overpayment rejected");
        check(invalid(s, "x' OR '1'='1", "100", "Cash", "6-12"), "SQL-injection style id rejected");
        System.out.println(failed == 0 ? "ALL TESTS PASSED" : failed + " FAILED");
        System.exit(failed);
    }
}
