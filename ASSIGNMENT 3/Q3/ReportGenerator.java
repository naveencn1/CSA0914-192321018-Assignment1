// Name: V V Naveen Kumar | Register No: 192321018 | Course: Programming in Java (CSA09) | Assignment 3 (CO3)
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;

/** A reader that counts how many instances are currently open (our "open file handle" meter). */
class TrackedReader extends BufferedReader {
    static int open = 0;
    private boolean closed = false;
    TrackedReader(String file) throws IOException { super(new FileReader(file)); open++; }
    @Override public void close() throws IOException {
        if (!closed) { closed = true; open--; super.close(); }
    }
}
class TrackedWriter extends PrintWriter {
    static int open = 0;
    private boolean closed = false;
    TrackedWriter(String file) throws IOException { super(new FileWriter(file)); open++; }
    @Override public void close() { if (!closed) { closed = true; open--; super.close(); } }
}

/** Q3 - Daily sales report: three ways to manage the same resources. */
public class ReportGenerator {
    static final Logger LOG = Logger.getLogger("report");

    /** Sums the amounts in one store file. Logs WHICH file failed (Student A: logging). */
    static double sumStore(BufferedReader r, String name) throws IOException {
        double total = 0; int line = 0; String s;
        try {
            while ((s = r.readLine()) != null) { line++; total += Double.parseDouble(s.trim()); }
        } catch (NumberFormatException e) {
            LOG.severe("File " + name + " failed at line " + line + ": " + e.getMessage());
            throw e;
        }
        return total;
    }

    /* ---------- Version 1: manual close() at the end of the try, no finally (leaks on failure) ---------- */
    static void manualLeaky(List<String> files, String out) throws IOException {
        TrackedWriter w = new TrackedWriter(out);
        List<TrackedReader> readers = new ArrayList<>();
        for (String f : files) readers.add(new TrackedReader(f));    // all opened up-front
        double grand = 0;
        for (int i = 0; i < files.size(); i++) {
            double t = sumStore(readers.get(i), files.get(i));       // <-- exception here skips all close() calls below
            w.println(name(files.get(i)) + "," + t);
            grand += t;
        }
        w.println("TOTAL," + grand);
        for (TrackedReader r : readers) r.close();
        w.close();
    }

    /* ---------- Version 2: correct try-catch-finally with manual close() ---------- */
    static void manualFinally(List<String> files, String out) throws IOException {
        TrackedWriter w = null;
        List<TrackedReader> readers = new ArrayList<>();
        try {
            w = new TrackedWriter(out);
            for (String f : files) readers.add(new TrackedReader(f));
            double grand = 0;
            for (int i = 0; i < files.size(); i++) {
                double t = sumStore(readers.get(i), files.get(i));
                w.println(name(files.get(i)) + "," + t);
                grand += t;
            }
            w.println("TOTAL," + grand);
        } finally {
            for (TrackedReader r : readers) {
                try { r.close(); } catch (IOException ignored) { }   // one failing close must not stop the others
            }
            if (w != null) w.close();
        }
    }

    /* ---------- Version 3: try-with-resources, one store at a time ---------- */
    static void withResources(List<String> files, String out) throws IOException {
        try (TrackedWriter w = new TrackedWriter(out)) {
            double grand = 0;
            for (String f : files) {
                try (TrackedReader r = new TrackedReader(f)) {       // closed automatically, even on exception
                    double t = sumStore(r, f);
                    w.println(name(f) + "," + t);
                    grand += t;
                }
            }
            w.println("TOTAL," + grand);
        }
    }

    static String name(String path) { return Paths.get(path).getFileName().toString(); }

    @FunctionalInterface interface Job { void run(List<String> files, String out) throws IOException; }

    static void attempt(String label, Job job, List<String> files, String out) {
        TrackedReader.open = 0; TrackedWriter.open = 0;
        String result;
        try { job.run(files, out); result = "completed"; }
        catch (Exception e) { result = "FAILED with " + e.getClass().getSimpleName(); }
        System.out.printf("%-26s %-28s open readers=%d, open writers=%d%n", label, result, TrackedReader.open, TrackedWriter.open);
    }

    /* ---------- Suppressed exception demo (Student B) ---------- */
    static class Res implements AutoCloseable {
        final String n; Res(String n) { this.n = n; System.out.println("open  " + n); }
        public void close() throws Exception { System.out.println("close " + n); throw new IllegalStateException("close failed for " + n); }
    }
    static void suppressedDemo() {
        try (Res a = new Res("A"); Res b = new Res("B")) {
            System.out.println("body throws");
            throw new RuntimeException("body failure");
        } catch (Exception e) {
            System.out.println("primary   : " + e);
            for (Throwable s : e.getSuppressed()) System.out.println("suppressed: " + s);
        }
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("sales");
        List<String> good = new ArrayList<>(), bad = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Path p = dir.resolve("store" + i + ".txt");
            Files.write(p, Arrays.asList("100.50", "200", "50.25", String.valueOf(i * 10)));
            good.add(p.toString()); bad.add(p.toString());
        }
        Path corrupt = dir.resolve("store3.txt");                       // store 3 holds a bad line part-way
        Files.write(corrupt, Arrays.asList("100.50", "N/A", "50.25"));

        System.out.println("=== Same input, all three versions: reports must be identical ===");
        String o1 = dir.resolve("r1.csv").toString(), o2 = dir.resolve("r2.csv").toString(), o3 = dir.resolve("r3.csv").toString();
        Files.write(corrupt, Arrays.asList("100.50", "200", "50.25", "30"));    // temporarily repair for the "good" run
        attempt("manualLeaky (good input)", ReportGenerator::manualLeaky, good, o1);
        attempt("manualFinally (good)", ReportGenerator::manualFinally, good, o2);
        attempt("withResources (good)", ReportGenerator::withResources, good, o3);
        String r1 = Files.readString(Path.of(o1)), r2 = Files.readString(Path.of(o2)), r3 = Files.readString(Path.of(o3));
        System.out.println("Reports identical: " + (r1.equals(r2) && r2.equals(r3)));
        System.out.print(r3);

        System.out.println("\n=== Failing input (store3 has the line 'N/A'): count what is left open ===");
        Files.write(corrupt, Arrays.asList("100.50", "N/A", "50.25"));
        LOG.setUseParentHandlers(true);
        attempt("manualLeaky (bad input)", ReportGenerator::manualLeaky, bad, o1);
        attempt("manualFinally (bad)", ReportGenerator::manualFinally, bad, o2);
        attempt("withResources (bad)", ReportGenerator::withResources, bad, o3);

        System.out.println("\n=== Suppressed exceptions ===");
        suppressedDemo();
    }
}
