/** Headless logic test (no window needed): java ConverterCoreTest */
public class ConverterCoreTest {
    static int failed = 0;
    static void check(boolean ok, String name) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }
    static boolean rejects(String s) { try { ConverterCore.parseAmount(s); return false; } catch (IllegalArgumentException e) { return true; } }

    public static void main(String[] a) {
        check(Math.abs(ConverterCore.convert(100, "USD", "INR") - 8300.0) < 1e-9, "100 USD -> 8300 INR");
        check(Math.abs(ConverterCore.convert(83, "INR", "USD") - 1.0) < 1e-9, "83 INR -> 1 USD");
        check(Math.abs(ConverterCore.convert(5, "EUR", "EUR") - 5.0) < 1e-9, "same currency unchanged");
        check(ConverterCore.parseAmount(" 12.5 ") == 12.5, "parse with spaces");
        check(rejects(""), "empty rejected");
        check(rejects("abc"), "text rejected");
        check(rejects("-5"), "negative rejected");
        check(rejects("0"), "zero rejected");
        check(rejects("NaN"), "NaN rejected");
        check(rejects("Infinity"), "Infinity rejected");
        System.out.println(failed == 0 ? "ALL TESTS PASSED" : failed + " FAILED");
        System.exit(failed);
    }
}
