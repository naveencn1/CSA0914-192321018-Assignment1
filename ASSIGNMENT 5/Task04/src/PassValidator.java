import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

/** Validation rules for migration. Returns null when the line is valid, otherwise the reason. */
public class PassValidator {
    public static String validate(String line, Set<String> seenIds) {
        if (line == null || line.isBlank()) return "empty line";
        String[] f = line.split("\\|", -1);
        if (f.length != 7) return "wrong field count (" + f.length + ")";
        if (f[0].isBlank()) return "blank pass ID";
        if (f[0].length() > 12 || f[1].length() > 12 || f[2].length() > 60 || f[3].length() > 4 || f[4].length() > 40)
            return "field too long for column";
        if (f[1].isBlank() || f[2].isBlank() || f[3].isBlank() || f[4].isBlank()) return "blank mandatory field";
        try { LocalDate.parse(f[5]); } catch (DateTimeParseException e) { return "invalid date '" + f[5] + "'"; }
        try { if (Integer.parseInt(f[6].trim()) < 0) return "negative amount"; }
        catch (NumberFormatException e) { return "non-numeric amount '" + f[6] + "'"; }
        if (!seenIds.add(f[0])) return "duplicate pass ID " + f[0];
        return null;
    }
}
