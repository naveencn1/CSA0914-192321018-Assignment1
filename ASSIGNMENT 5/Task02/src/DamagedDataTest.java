import java.io.*;
import java.nio.file.*;

/** B2: adversarial bus-pass lines. Reports each damaged line with the reason it is rejected. */
public class DamagedDataTest {
    static String reason(String line) {
        if (line.isBlank()) return "empty/blank line";
        String[] f = line.split("\\|", -1);
        if (f.length != 7) return "wrong field count (" + f.length + " instead of 7)";
        if (f[0].isBlank()) return "blank pass ID (no key)";
        try { if (Integer.parseInt(f[6].trim()) < 0) return "negative amount"; }
        catch (NumberFormatException e) { return "non-numeric amount '" + f[6] + "'"; }
        return null;
    }
    public static void main(String[] args) throws Exception {
        String file = args.length > 0 ? args[0] : "data/damaged_passes.txt";
        int n = 0, ok = 0, bad = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                n++;
                String why = reason(line);
                boolean parsed = PassRecord.parse(line) != null;
                if (why == null && parsed) { ok++; System.out.printf("line %d  OK       %s%n", n, line); }
                else { bad++; System.out.printf("line %d  DAMAGED  %-70s -> %s%n", n, line.isEmpty() ? "<empty>" : line, why); }
            }
        }
        System.out.printf("%nlines read=%d valid=%d damaged=%d%n", n, ok, bad);
        PassStore s = new PassStore(file);
        System.out.println("find(P2008) after 6 damaged lines -> " + s.find("P2008"));
        System.out.println("find(P2003) damaged (non-numeric amount) -> " + s.find("P2003"));
        System.out.println("find(P2002) damaged (5 fields)           -> " + s.find("P2002"));
    }
}
