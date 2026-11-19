import java.io.*;
import java.nio.file.*;
import java.util.*;

/** B1 measurements: (1) line counting 1/10/50 MB; (2) search time 1k..100k; (3) scan vs HashMap. */
public class Bench01 {
    static String[] STOPS = {"Sriperumbudur","Guindy","Tambaram","Porur","Poonamallee","Avadi","Kundrathur","Vandalur"};

    static File makeFileOfSize(String path, long targetBytes) throws IOException {
        File f = new File(path);
        try (BufferedWriter w = new BufferedWriter(new FileWriter(f))) {
            long written = 0; int i = 1;
            while (written < targetBytes) {
                String line = new PassRecord("P" + (100000 + i), "19232" + (1000 + i % 9000), "Student" + i,
                        String.valueOf(1 + i % 20), STOPS[i % STOPS.length], "2027-03-31", 6000 + i % 700).toLine();
                w.write(line); w.newLine();
                written += line.length() + 1; i++;
            }
        }
        return f;
    }

    static void makeRecords(String path, int n) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(path))) {
            for (int i = 1; i <= n; i++) {
                w.write(new PassRecord("P" + (100000 + i), "19232" + (1000 + i % 9000), "Student" + i,
                        String.valueOf(1 + i % 20), STOPS[i % STOPS.length], "2027-03-31", 6000 + i % 700).toLine());
                w.newLine();
            }
        }
    }

    static long usedMB() { Runtime r = Runtime.getRuntime(); return (r.totalMemory() - r.freeMemory()) / (1024 * 1024); }

    public static void main(String[] a) throws Exception {
        Files.createDirectories(Paths.get("data/bench"));
        StringBuilder csv = new StringBuilder();
        System.out.println("=== PART 1: line counting (ms), 3 runs each, median reported ===");
        System.out.printf("%-8s %-10s %-14s %-12s %-14s %-14s %-16s%n", "size", "lines", "unbuf_ms", "buf64k_ms", "readLine_ms", "readAll_ms", "read() calls unbuf/buf");
        csv.append("size_mb,lines,unbuffered_ms,buffered64k_ms,bufferedReader_ms,readAllLines_ms,calls_unbuffered,calls_buffered,readAllLines_heap_mb\n");
        for (int mb : new int[]{1, 10, 50}) {
            File f = makeFileOfSize("data/bench/log_" + mb + "mb.txt", mb * 1024L * 1024L);
            long[] cu = {0}, cb = {0}; long lines = 0;
            long[] tu = new long[3], tb = new long[3], tr = new long[3], ta = new long[3];
            for (int k = 0; k < 3; k++) {
                long t = System.nanoTime(); lines = LineCounter.unbuffered(f.getPath(), cu); tu[k] = (System.nanoTime() - t) / 1_000_000;
                t = System.nanoTime(); LineCounter.buffered(f.getPath(), cb); tb[k] = (System.nanoTime() - t) / 1_000_000;
                t = System.nanoTime(); LineCounter.bufferedReader(f.getPath()); tr[k] = (System.nanoTime() - t) / 1_000_000;
                System.gc(); long before = usedMB();
                t = System.nanoTime(); List<String> all = Files.readAllLines(f.toPath()); ta[k] = (System.nanoTime() - t) / 1_000_000;
                long heap = usedMB() - before; if (k == 2) { csvHeap = heap; } if (all.size() != lines) throw new IllegalStateException();
            }
            Arrays.sort(tu); Arrays.sort(tb); Arrays.sort(tr); Arrays.sort(ta);
            System.out.printf("%-8s %-10d %-14d %-12d %-14d %-14d %,d / %,d%n", mb + "MB", lines, tu[1], tb[1], tr[1], ta[1], cu[0], cb[0]);
            csv.append(String.format("%d,%d,%d,%d,%d,%d,%d,%d,%d%n", mb, lines, tu[1], tb[1], tr[1], ta[1], cu[0], cb[0], csvHeap));
        }
        Files.writeString(Paths.get("results/line_counting.csv"), csv.toString());
        System.out.println("(readAllLines retained heap for the 50MB file ~" + csvHeap + " MB; buffered counting uses a fixed 64 KB buffer)");

        System.out.println();
        System.out.println("=== PART 2: search time vs file size, and scan vs HashMap (q = 1,000 searches) ===");
        StringBuilder c2 = new StringBuilder("n,scan_one_find_worst_ms,scan_1000_finds_ms,hashmap_build_ms,hashmap_1000_finds_ms,hashmap_heap_mb\n");
        System.out.printf("%-9s %-22s %-20s %-18s %-22s %-12s%n", "n", "1 scan (worst) ms", "1000 scans ms", "map build ms", "1000 map finds ms", "map heap MB");
        Random rnd = new Random(42);
        for (int n : new int[]{1000, 5000, 10000, 50000, 100000}) {
            String p = "data/bench/passes_" + n + ".txt"; makeRecords(p, n);
            PassStore s = new PassStore(p);
            String worst = "P" + (100000 + n);                     // last record = worst case
            s.find(worst);                                          // warm-up
            long t = System.nanoTime(); s.find(worst); double one = (System.nanoTime() - t) / 1e6;
            int q = 1000; String[] ids = new String[q];
            for (int i = 0; i < q; i++) ids[i] = "P" + (100001 + rnd.nextInt(n));
            t = System.nanoTime(); for (String id : ids) s.find(id); double scanQ = (System.nanoTime() - t) / 1e6;
            System.gc(); long h0 = usedMB();
            t = System.nanoTime(); Map<String, PassRecord> m = s.loadAll(); double build = (System.nanoTime() - t) / 1e6;
            long heap = usedMB() - h0;
            t = System.nanoTime(); for (String id : ids) if (m.get(id) == null) throw new IllegalStateException(); double mapQ = (System.nanoTime() - t) / 1e6;
            System.out.printf("%-9d %-22.2f %-20.1f %-18.1f %-22.3f %-12d%n", n, one, scanQ, build, mapQ, heap);
            c2.append(String.format("%d,%.2f,%.1f,%.1f,%.3f,%d%n", n, one, scanQ, build, mapQ, heap));
        }
        Files.writeString(Paths.get("results/search_scaling.csv"), c2.toString());
    }
    static long csvHeap = 0;
}
