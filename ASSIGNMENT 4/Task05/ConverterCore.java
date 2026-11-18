import java.util.LinkedHashMap;
import java.util.Map;

/** Pure conversion logic - no GUI code. Rates are fixed demo values (units per 1 USD). */
public final class ConverterCore {
    private static final Map<String, Double> PER_USD = new LinkedHashMap<>();
    static {
        PER_USD.put("USD", 1.0);
        PER_USD.put("EUR", 0.92);
        PER_USD.put("GBP", 0.79);
        PER_USD.put("INR", 83.0);
        PER_USD.put("JPY", 150.0);
    }

    private ConverterCore() { }

    public static String[] currencies() {
        return PER_USD.keySet().toArray(new String[0]);
    }

    /** Parses and validates the amount text. Throws IllegalArgumentException with a user-friendly message. */
    public static double parseAmount(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter an amount.");
        }
        double value;
        try {
            value = Double.parseDouble(text.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("'" + text.trim() + "' is not a valid number.");
        }
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Amount must be a finite number.");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        return value;
    }

    public static double convert(double amount, String from, String to) {
        return amount / PER_USD.get(from) * PER_USD.get(to);
    }
}
