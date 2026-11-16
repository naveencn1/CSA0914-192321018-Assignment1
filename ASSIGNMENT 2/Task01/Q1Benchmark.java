import java.util.StringJoiner;

/** Q1 - String vs StringBuilder vs StringBuffer for e-ticket generation. */
public class Q1Benchmark {
    static volatile int sink; // prevents dead-code elimination

    static String[] makeFields(int n) {
        String[] f = new String[n];
        for (int i = 0; i < n; i++) f[i] = "FIELD" + i + "=VALUE" + (i * 7919 % 1000) + ";";
        return f;
    }

    static String viaPlus(String[] f) {
        String s = "";
        for (String x : f) s += x;            // a new String object every iteration
        return s;
    }
    static String viaBuilder(String[] f) {
        StringBuilder sb = new StringBuilder();
        for (String x : f) sb.append(x);
        return sb.toString();
    }
    static String viaBuffer(String[] f) {
        StringBuffer sb = new StringBuffer();
        for (String x : f) sb.append(x);
        return sb.toString();
    }
    static String viaJoin(String[] f) { return String.join("", f); }
    static String viaJoiner(String[] f) {
        StringJoiner j = new StringJoiner("");
        for (String x : f) j.add(x);
        return j.toString();
    }
    static String viaFormat(String[] f) {
        StringBuilder sb = new StringBuilder();
        for (String x : f) sb.append(String.format("%s", x));
        return sb.toString();
    }

    interface Builder { String build(String[] f); }

    /** average ms for one full workload (tickets x fields), after warm-up */
    static double time(Builder b, String[] f, int tickets, int runs) {
        for (int w = 0; w < 2; w++) for (int t = 0; t < tickets; t++) sink += b.build(f).length();
        long total = 0;
        for (int r = 0; r < runs; r++) {
            long t0 = System.nanoTime();
            for (int t = 0; t < tickets; t++) sink += b.build(f).length();
            total += System.nanoTime() - t0;
        }
        return total / (double) runs / 1e6;
    }

    static long usedMemory() {
        Runtime rt = Runtime.getRuntime();
        System.gc(); System.gc();
        return rt.totalMemory() - rt.freeMemory();
    }

    static long allocatedBytes(Builder b, String[] f, int tickets) {
        com.sun.management.ThreadMXBean mx =
            (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
        long id = Thread.currentThread().getId();
        long a0 = mx.getThreadAllocatedBytes(id);
        for (int t = 0; t < tickets; t++) sink += b.build(f).length();
        return mx.getThreadAllocatedBytes(id) - a0;
    }

    public static void main(String[] args) {
        // name, tickets, fields, runs
        Object[][] workloads = {
            {"A: 1 short ticket (8 fields)", 1, 8, 2000},
            {"B: 10,000 tickets x 50 fields", 10000, 50, 5},
            {"C (own): 50 huge tickets x 2000 fields", 50, 2000, 3}
        };
        System.out.println("Java " + System.getProperty("java.version"));
        for (Object[] w : workloads) {
            String name = (String) w[0]; int tickets = (int) w[1], n = (int) w[2], runs = (int) w[3];
            String[] f = makeFields(n);
            System.out.println("\n=== Workload " + name + " ===");
            System.out.println("Estimated intermediate Strings with '+': " + (long) tickets * n
                + "  | StringBuilder/StringBuffer: " + tickets + " (only the final toString)");
            Object[][] impl = {
                {"String +", (Builder) Q1Benchmark::viaPlus},
                {"StringBuilder", (Builder) Q1Benchmark::viaBuilder},
                {"StringBuffer", (Builder) Q1Benchmark::viaBuffer},
                {"String.join", (Builder) Q1Benchmark::viaJoin},
                {"StringJoiner", (Builder) Q1Benchmark::viaJoiner},
                {"String.format", (Builder) Q1Benchmark::viaFormat}};
            System.out.printf("%-15s %14s %18s %16s%n", "Approach", "avg time (ms)", "allocated (MB)", "heap after (MB)");
            for (Object[] i : impl) {
                Builder b = (Builder) i[1];
                double ms = time(b, f, tickets, runs);
                long alloc = allocatedBytes(b, f, tickets);
                long before = usedMemory();
                sink += b.build(f).length();
                long after = usedMemory();
                System.out.printf("%-15s %14.3f %18.2f %16.2f%n", i[0], ms, alloc / 1048576.0, after / 1048576.0);
            }
        }
    }
}
