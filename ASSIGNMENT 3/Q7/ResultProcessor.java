// Name: V V Naveen Kumar | Register No: 192321018 | Course: Programming in Java (CSA09) | Assignment 3 (CO3)
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.*;

/**
 * Q7 - Semester result processing: ExecutorService + Callable + Future + concurrent collections.
 * Usage:  java ResultProcessor test | bench | alt
 *
 * Failure categories (each handled, none stops the run):
 *   MISSING  - file listed in the manifest but not on disk          (NoSuchFileException)
 *   EMPTY    - file has no content                                  (BadScoreFileException)
 *   FORMAT   - wrong number of fields or a non-numeric mark         (BadScoreFileException)
 *   RANGE    - a mark outside 0..100                                (BadScoreFileException)
 *   UNEXPECTED - anything else (a bug); recorded, never ignored
 */
public class ResultProcessor {

    static class BadScoreFileException extends Exception {
        final String category;
        BadScoreFileException(String category, String msg) { super(msg); this.category = category; }
    }
    record Result(String file, String studentId, int total, double percent, String grade) { }

    /** Simulated per-file I/O latency in ms (set 0 for the CPU-bound experiment). */
    static volatile int latencyMs = 1;

    // ---- shared results: concurrent collections ----
    static class Store {
        final ConcurrentHashMap<String, Result> successes = new ConcurrentHashMap<>();       // key = file name, no lost updates
        final ConcurrentHashMap<String, String> failures = new ConcurrentHashMap<>();        // file -> category
        final ConcurrentHashMap<String, LongAdder> failureCounts = new ConcurrentHashMap<>();// low-contention counters
        void ok(Result r) { successes.put(r.file(), r); }
        void fail(String file, String category) { failures.put(file, category); failureCounts.computeIfAbsent(category, k -> new LongAdder()).increment(); }
        int handled() { return successes.size() + failures.size(); }
    }

    /** Read -> validate -> compute. Throws a precise exception for each failure. */
    static Result process(Path p) throws IOException, BadScoreFileException {
        if (latencyMs > 0) try { Thread.sleep(latencyMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        List<String> lines = Files.readAllLines(p);                    // NoSuchFileException if missing (an IOException)
        if (lines.isEmpty() || lines.get(0).isBlank()) throw new BadScoreFileException("EMPTY", "no content");
        String[] f = lines.get(0).trim().split(",");
        if (f.length != 6) throw new BadScoreFileException("FORMAT", "expected 6 fields, got " + f.length);
        int total = 0;
        for (int i = 1; i < 6; i++) {
            int m;
            try { m = Integer.parseInt(f[i].trim()); }
            catch (NumberFormatException e) { throw new BadScoreFileException("FORMAT", "mark not numeric: " + f[i]); }
            if (m < 0 || m > 100) throw new BadScoreFileException("RANGE", "mark out of range: " + m);
            total += m;
        }
        double pct = total / 5.0;
        String grade = pct >= 90 ? "A" : pct >= 75 ? "B" : pct >= 60 ? "C" : pct >= 40 ? "D" : "F";
        return new Result(p.getFileName().toString(), f[0], total, pct, grade);
    }

    /** One Callable per file. It NEVER lets an exception escape for known failures. */
    static Callable<Boolean> task(Path p, Store store) {
        return () -> {
            try { store.ok(process(p)); return true; }
            catch (BadScoreFileException e) { store.fail(p.getFileName().toString(), e.category); }
            catch (NoSuchFileException e)   { store.fail(p.getFileName().toString(), "MISSING"); }
            catch (IOException e)           { store.fail(p.getFileName().toString(), "IO"); }
            return false;
        };
    }

    /** Runs everything on a pool; returns elapsed ms. Futures catch even the failures we did not predict. */
    static long runPool(List<Path> files, int threads, Store store) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        long t0 = System.nanoTime();
        Map<Future<Boolean>, Path> futures = new LinkedHashMap<>();
        for (Path p : files) futures.put(pool.submit(task(p, store)), p);
        for (Map.Entry<Future<Boolean>, Path> e : futures.entrySet()) {
            try { e.getKey().get(); }
            catch (ExecutionException ex) { store.fail(e.getValue().getFileName().toString(), "UNEXPECTED"); }  // bug in a task
        }
        pool.shutdown();
        if (!pool.awaitTermination(1, TimeUnit.MINUTES)) throw new IllegalStateException("pool did not finish");
        return (System.nanoTime() - t0) / 1_000_000;
    }

    static void summary(String title, int expectedFiles, Store s) {
        System.out.println("--- " + title + " ---");
        System.out.println("  files expected : " + expectedFiles);
        System.out.println("  succeeded      : " + s.successes.size());
        System.out.println("  failed         : " + s.failures.size() + "  " + new TreeMap<>(s.failureCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().sum()))));
        System.out.println("  completeness   : succeeded + failed = " + s.handled() + (s.handled() == expectedFiles ? "  (OK - every file accounted for)" : "  (MISMATCH!)"));
    }

    // ---------- data generation ----------
    static List<Path> generate(Path dir, int n) throws IOException {
        Files.createDirectories(dir);
        List<Path> manifest = new ArrayList<>(n);
        Random r = new Random(42);
        for (int i = 1; i <= n; i++) {
            Path p = dir.resolve(String.format("s%05d.txt", i));
            manifest.add(p);
            int dice = r.nextInt(100);
            if (dice < 2) continue;                                                        // 2 % missing
            String body;
            if (dice < 3) body = "";                                                       // 1 % empty
            else if (dice < 4) body = "S" + i + ",80,abc,70,60,50";                        // 1 % non-numeric
            else if (dice < 5) body = "S" + i + ",80,70,60";                               // 1 % wrong field count
            else if (dice < 6) body = "S" + i + ",80,170,60,50,40";                        // 1 % out of range
            else body = "S" + i + "," + (40 + r.nextInt(61)) + "," + (40 + r.nextInt(61)) + "," + (40 + r.nextInt(61)) + "," + (40 + r.nextInt(61)) + "," + (40 + r.nextInt(61));
            Files.writeString(p, body);
        }
        return manifest;
    }

    static void test() throws Exception {
        System.out.println("=== Hand-checked set of 10 files ===");
        Path dir = Files.createTempDirectory("hand");
        String[][] spec = {
            {"h01.txt", "S1,90,90,90,90,90"},   // ok  A  (total 450)
            {"h02.txt", "S2,80,80,80,80,80"},   // ok  B
            {"h03.txt", "S3,70,70,70,70,70"},   // ok  C
            {"h04.txt", "S4,50,50,50,50,50"},   // ok  D
            {"h05.txt", "S5,10,10,10,10,10"},   // ok  F
            {"h06.txt", "S6,100,100,100,100,100"}, // ok A
            {"h07.txt", ""},                    // EMPTY
            {"h08.txt", "S8,1,2,x,4,5"},        // FORMAT
            {"h09.txt", "S9,1,2,3,4,500"},      // RANGE
        };
        List<Path> files = new ArrayList<>();
        for (String[] s : spec) { Path p = dir.resolve(s[0]); Files.writeString(p, s[1]); files.add(p); }
        files.add(dir.resolve("h10.txt"));      // MISSING (never created)
        for (int threads : new int[]{1, 4}) {
            Store st = new Store();
            runPool(files, threads, st);
            summary("pool of " + threads, 10, st);
            boolean right = st.successes.size() == 6 && st.failures.size() == 4
                    && st.failureCounts.get("EMPTY").sum() == 1 && st.failureCounts.get("FORMAT").sum() == 1
                    && st.failureCounts.get("RANGE").sum() == 1 && st.failureCounts.get("MISSING").sum() == 1
                    && st.successes.get("h01.txt").grade().equals("A") && st.successes.get("h05.txt").grade().equals("F")
                    && st.successes.get("h06.txt").total() == 500;
            System.out.println("  matches hand-checked expectation (6 ok; EMPTY 1, FORMAT 1, RANGE 1, MISSING 1): " + right);
        }
    }

    static void bench() throws Exception {
        Path dir = Files.createTempDirectory("scores");
        List<Path> files = generate(dir, 10_000);
        System.out.println("CPU cores available: " + Runtime.getRuntime().availableProcessors());
        for (int lat : new int[]{1, 0}) {
            latencyMs = lat;
            System.out.println("\n=== 10,000 files, " + (lat > 0 ? "I/O-bound (" + lat + " ms simulated latency per file)" : "CPU/disk-bound (no added latency)") + " ===");
            if (lat == 0) runPool(files, 2, new Store());                         // warm-up
            for (int threads : new int[]{1, 2, 4, 8, 16}) {
                Store st = new Store();
                long ms = runPool(files, threads, st);
                System.out.printf("  pool=%2d  %6d ms   ok=%d failed=%d  complete=%b%n", threads, ms, st.successes.size(), st.failures.size(), st.handled() == 10_000);
            }
            if (lat > 0) { Store st = new Store(); runPool(files, 8, st); summary("summary (pool 8)", 10_000, st); }
        }
    }

    static void alt() throws Exception {
        Path dir = Files.createTempDirectory("scores");
        List<Path> files = generate(dir, 10_000);
        latencyMs = 1;
        var mx = ManagementFactory.getThreadMXBean();

        System.out.println("=== Alternative 1: files.parallelStream() ===");
        mx.resetPeakThreadCount();
        Store s1 = new Store();
        long t0 = System.nanoTime();
        files.parallelStream().forEach(p -> { try { task(p, s1).call(); } catch (Exception e) { s1.fail(p.getFileName().toString(), "UNEXPECTED"); } });
        System.out.printf("  %d ms, peak JVM threads %d (common pool size = %d), ok=%d failed=%d complete=%b%n",
                (System.nanoTime() - t0) / 1_000_000, mx.getPeakThreadCount(), ForkJoinPool.commonPool().getParallelism(), s1.successes.size(), s1.failures.size(), s1.handled() == 10_000);

        System.out.println("\n=== Alternative 2: one thread per file (10,000 platform threads) ===");
        mx.resetPeakThreadCount();
        Store s2 = new Store();
        t0 = System.nanoTime();
        List<Thread> ts = new ArrayList<>();
        try {
            for (Path p : files) { Thread t = new Thread(() -> { try { task(p, s2).call(); } catch (Exception e) { s2.fail(p.getFileName().toString(), "UNEXPECTED"); } }); t.start(); ts.add(t); }
            for (Thread t : ts) t.join();
        } catch (OutOfMemoryError e) { System.out.println("  OutOfMemoryError after starting " + ts.size() + " threads: " + e.getMessage()); }
        System.out.printf("  %d ms, peak JVM threads %d, ok=%d failed=%d complete=%b%n",
                (System.nanoTime() - t0) / 1_000_000, mx.getPeakThreadCount(), s2.successes.size(), s2.failures.size(), s2.handled() == 10_000);
        System.out.println("  NOTE: the measured peak is low only because tiny tasks finish while others are still being created.");
        System.out.println("  Worst case if all were alive together: 10,000 threads x ~1 MB reserved stack = ~10 GB of address space (OutOfMemoryError risk).");

        System.out.println("\n=== Reference: ExecutorService with 8 threads ===");
        Store s3 = new Store();
        long ms = runPool(files, 8, s3);
        System.out.printf("  %d ms, 8 worker threads, ok=%d failed=%d complete=%b%n", ms, s3.successes.size(), s3.failures.size(), s3.handled() == 10_000);
    }

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "test";
        switch (mode) { case "test" -> test(); case "bench" -> bench(); case "alt" -> alt(); default -> System.out.println("use: test | bench | alt"); }
    }
}
