import java.lang.management.ManagementFactory;

/**
 * Q7 - Diagnose, Redesign and Validate.
 * Chosen solution: Q1 e-ticket generation (String '+' in a loop).
 *
 * Diagnosis (from Q1 evidence): '+' copies the whole partial ticket on every field => O(n^2) characters
 *   copied and ~n garbage Strings per ticket; at 50 tickets x 2000 fields it allocated ~1.7 GB.
 * Objective : for 10,000 tickets x 50 fields AND 50 tickets x 2000 fields, allocate >= 5x fewer bytes
 *   than the original, run faster, and produce byte-for-byte identical tickets.
 * Redesign  : one reusable, pre-sized StringBuilder per worker; chained appends (no key+"="+value temporaries);
 *   capacity derived from the number of fields; setLength(0) between tickets; only the final toString().
 */
public class Q7Redesign {
    static volatile int sink;

    static String[] keys(int n) { String[] k = new String[n]; for (int i = 0; i < n; i++) k[i] = "FIELD" + i; return k; }
    static int[] vals(int n) { int[] v = new int[n]; for (int i = 0; i < n; i++) v[i] = i * 7919 % 1000; return v; }

    // ---- ORIGINAL: '+' in a loop ----
    static String original(String[] k, int[] v) {
        String s = "";
        for (int i = 0; i < k.length; i++) s += k[i] + "=" + v[i] + ";";
        return s;
    }

    // ---- INTERMEDIATE: StringBuilder, but with temporaries inside append() ----
    static String naiveBuilder(String[] k, int[] v) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < k.length; i++) sb.append(k[i] + "=" + v[i] + ";");
        return sb.toString();
    }

    // ---- REDESIGN: reusable, pre-sized builder, chained appends ----
    static final class TicketWriter {
        private final StringBuilder sb;
        TicketWriter(int fields) { sb = new StringBuilder(fields * 20); }   // ~20 chars per field
        String build(String[] k, int[] v) {
            sb.setLength(0);                       // reuse the same buffer
            for (int i = 0; i < k.length; i++) sb.append(k[i]).append('=').append(v[i]).append(';');
            return sb.toString();
        }
    }

    interface Gen { String make(String[] k, int[] v); }

    static com.sun.management.ThreadMXBean mx() {
        return (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    }

    public static void main(String[] args) {
        System.out.println("Java " + System.getProperty("java.version"));
        int[][] workloads = {{10_000, 50, 5}, {50, 2000, 3}};      // tickets, fields, runs
        for (int[] w : workloads) {
            int tickets = w[0], n = w[1], runs = w[2];
            String[] k = keys(n); int[] v = vals(n);
            TicketWriter tw = new TicketWriter(n);
            Gen[] gens = { Q7Redesign::original, Q7Redesign::naiveBuilder, tw::build };
            String[] names = {"Original (+ in loop)", "StringBuilder + temporaries", "Redesign (reuse, presized, chained)"};

            // correctness validation
            String ref = original(k, v);
            boolean same = naiveBuilder(k, v).equals(ref) && tw.build(k, v).equals(ref);
            System.out.printf("%n=== %d tickets x %d fields | identical output: %b ===%n", tickets, n, same);
            System.out.printf("%-38s %12s %16s %16s%n", "Version", "avg ms", "allocated MB", "MB / 1000 tickets");

            double baseMs = 0, baseMb = 0;
            for (int g = 0; g < gens.length; g++) {
                for (int i = 0; i < 2; i++) for (int t = 0; t < tickets; t++) sink += gens[g].make(k, v).length(); // warm-up
                double ms = 0;
                for (int r = 0; r < runs; r++) {
                    long t0 = System.nanoTime();
                    for (int t = 0; t < tickets; t++) sink += gens[g].make(k, v).length();
                    ms += (System.nanoTime() - t0) / 1e6;
                }
                ms /= runs;
                long id = Thread.currentThread().threadId();
                long a0 = mx().getThreadAllocatedBytes(id);
                for (int t = 0; t < tickets; t++) sink += gens[g].make(k, v).length();
                double mb = (mx().getThreadAllocatedBytes(id) - a0) / 1048576.0;
                if (g == 0) { baseMs = ms; baseMb = mb; }
                System.out.printf("%-38s %12.2f %16.2f %16.3f   (x%.1f faster, x%.1f less memory)%n",
                    names[g], ms, mb, mb / tickets * 1000, baseMs / ms, baseMb / mb);
            }
        }
    }
}
