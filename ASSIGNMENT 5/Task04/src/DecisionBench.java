import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** B4 evidence for the final decision: text file vs serialization vs database at 100x scale (800,000 passes, 12 campuses). */
public class DecisionBench {
    static final int N = 800_000, CAMPUSES = 12;
    static final String[] STOPS = {"Sriperumbudur","Guindy","Tambaram","Porur","Poonamallee","Avadi","Kundrathur","Vandalur"};

    static class SerPass implements Serializable {
        private static final long serialVersionUID = 1L;
        String passId, registerNo, name, routeNo, stop, valid; int amount; byte campus;
    }
    static SerPass make(int i) {
        SerPass p = new SerPass(); int id = 100000 + i;
        p.passId = "P" + id; p.registerNo = "19232" + (1000 + id % 9000); p.name = "Student " + id;
        p.routeNo = String.valueOf(1 + id % 20); p.stop = STOPS[id % STOPS.length]; p.valid = "2027-03-31";
        p.amount = 6000 + id % 700; p.campus = (byte) (i % CAMPUSES); return p;
    }
    static String line(SerPass p) { return String.join("|", p.passId, p.registerNo, p.name, p.routeNo, p.stop, p.valid, String.valueOf(p.amount)); }
    static long usedMB() { System.gc(); Runtime r = Runtime.getRuntime(); return (r.totalMemory() - r.freeMemory()) >> 20; }
    static double ms(long t0) { return (System.nanoTime() - t0) / 1e6; }
    static long mb(File f) { return f.length() >> 20; }

    public static void main(String[] a) throws Exception {
        Files.createDirectories(Paths.get("data/bench"));
        StringBuilder csv = new StringBuilder("experiment,metric,value\n");
        Random rnd = new Random(7);
        File text = new File("data/bench/all_passes_800k.txt");

        System.out.println("=== 1. BUILD 800,000 passes (" + CAMPUSES + " campuses) in text and serialized forms ===");
        long t = System.nanoTime();
        try (BufferedWriter w = new BufferedWriter(new FileWriter(text))) { for (int i = 0; i < N; i++) { w.write(line(make(i))); w.newLine(); } }
        System.out.printf("text file write            : %8.0f ms   size=%d MB%n", ms(t), mb(text));
        csv.append(String.format("build,text_write_ms,%.0f%nbuild,text_size_mb,%d%n", ms(t), mb(text)));

        t = System.nanoTime(); long perCampusBytes = 0;
        for (int c = 0; c < CAMPUSES; c++) {
            ArrayList<SerPass> l = new ArrayList<>();
            for (int i = c; i < N; i += CAMPUSES) l.add(make(i));
            File f = new File("data/bench/campus_" + c + ".ser");
            try (ObjectOutputStream o = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(f)))) { o.writeObject(l); }
            perCampusBytes += f.length();
        }
        System.out.printf("12 per-campus .ser write   : %8.0f ms   total size=%d MB%n", ms(t), perCampusBytes >> 20);
        csv.append(String.format("build,campus_ser_write_ms,%.0f%nbuild,campus_ser_total_mb,%d%n", ms(t), perCampusBytes >> 20));

        System.out.println("\n=== 2. LOOKUP of one pass by ID (random ids) ===");
        PassStore store = new PassStore(text.getPath());
        store.find("P100001");                                               // warm-up
        int Q = 20; double scanTotal = 0;
        for (int i = 0; i < Q; i++) { String id = "P" + (100000 + rnd.nextInt(N)); long t0 = System.nanoTime(); if (store.find(id) == null) throw new IllegalStateException(); scanTotal += ms(t0); }
        double scanAvg = scanTotal / Q;
        System.out.printf("text scan, avg of %d finds : %8.1f ms per find  -> 1,000 finds ~ %.1f s%n", Q, scanAvg, scanAvg);
        csv.append(String.format("lookup,text_scan_avg_ms,%.1f%n", scanAvg));

        long h0 = usedMB(); t = System.nanoTime();
        Map<String, PassRecord> map = store.loadAll(); double mapLoad = ms(t); long mapHeap = usedMB() - h0;
        t = System.nanoTime(); for (int i = 0; i < 1000; i++) if (map.get("P" + (100000 + rnd.nextInt(N))) == null) throw new IllegalStateException(); double mapQ = ms(t);
        System.out.printf("HashMap: load=%.0f ms, heap~%d MB, 1,000 finds=%.2f ms%n", mapLoad, mapHeap, mapQ);
        csv.append(String.format("lookup,hashmap_load_ms,%.0f%nlookup,hashmap_heap_mb,%d%nlookup,hashmap_1000_finds_ms,%.2f%n", mapLoad, mapHeap, mapQ));
        map = null;

        // serialization: one single combined file vs 12 files
        ArrayList<SerPass> all = new ArrayList<>(N);
        for (int i = 0; i < N; i++) all.add(make(i));
        File one = new File("data/bench/all.ser");
        t = System.nanoTime();
        try (ObjectOutputStream o = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(one)))) { o.writeObject(all); }
        double oneWrite = ms(t); all = null;
        t = System.nanoTime(); Object ignore;
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(one)))) { ignore = in.readObject(); }
        double oneRead = ms(t); ignore = null;
        System.out.printf("serialization single file  : write=%.0f ms, size=%d MB, full read (needed for ANY lookup)=%.0f ms%n", oneWrite, mb(one), oneRead);
        csv.append(String.format("lookup,ser_single_write_ms,%.0f%nlookup,ser_single_size_mb,%d%nlookup,ser_single_read_ms,%.0f%n", oneWrite, mb(one), oneRead));

        t = System.nanoTime();
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream("data/bench/campus_0.ser")))) { ignore = in.readObject(); }
        double oneCampus = ms(t);
        t = System.nanoTime();
        for (int c = 0; c < CAMPUSES; c++)
            try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream("data/bench/campus_" + c + ".ser")))) { ignore = in.readObject(); }
        double allCampus = ms(t); ignore = null;
        System.out.printf("per-campus files           : one campus read=%.0f ms; search ACROSS all 12 campuses=%.0f ms%n", oneCampus, allCampus);
        csv.append(String.format("lookup,ser_one_campus_read_ms,%.0f%nlookup,ser_all_12_campuses_read_ms,%.0f%n", oneCampus, allCampus));

        System.out.println("\n=== 3. DATABASE (one shared table, PRIMARY KEY pass_id, INDEX(campus,route_no)) ===");
        try (Connection c = DriverManager.getConnection(PassLoader.URL, PassLoader.USER, PassLoader.PASS)) {
            try (Statement s = c.createStatement()) { s.execute("TRUNCATE TABLE decision_pass"); }
            c.setAutoCommit(false); t = System.nanoTime();
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO decision_pass VALUES (?,?,?,?,?,?,?,?)")) {
                int pend = 0;
                for (int i = 0; i < N; i++) {
                    SerPass p = make(i);
                    ps.setString(1, p.passId); ps.setInt(2, p.campus); ps.setString(3, p.registerNo); ps.setString(4, p.name);
                    ps.setString(5, p.routeNo); ps.setString(6, p.stop); ps.setDate(7, java.sql.Date.valueOf(p.valid)); ps.setInt(8, p.amount);
                    ps.addBatch(); if (++pend == 1000) { ps.executeBatch(); pend = 0; }
                }
                if (pend > 0) ps.executeBatch();
            }
            c.commit(); c.setAutoCommit(true);
            double load = ms(t);
            System.out.printf("load 800,000 rows (1 txn, batches of 1000): %.0f ms%n", load);
            csv.append(String.format("db,load_800k_ms,%.0f%n", load));

            try (PreparedStatement ps = c.prepareStatement("SELECT register_no FROM decision_pass WHERE pass_id=?")) {
                ps.setString(1, "P100001"); ps.executeQuery().close();          // warm-up
                t = System.nanoTime();
                for (int i = 0; i < 1000; i++) { ps.setString(1, "P" + (100000 + rnd.nextInt(N))); try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw new IllegalStateException(); } }
                double q = ms(t);
                System.out.printf("1,000 random PK lookups     : %.0f ms total  (%.2f ms each)%n", q, q / 1000);
                csv.append(String.format("db,pk_1000_lookups_ms,%.0f%n", q));
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM decision_pass WHERE campus=? AND route_no=?")) {
                ps.setInt(1, 3); ps.setString(2, "4"); t = System.nanoTime();
                try (ResultSet rs = ps.executeQuery()) { rs.next(); System.out.printf("route listing (campus 3, route 4) via index: %.1f ms, %d rows%n", ms(t), rs.getInt(1)); }
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM decision_pass WHERE register_no=?")) {
                ps.setString(1, "192321018"); t = System.nanoTime();
                try (ResultSet rs = ps.executeQuery()) { rs.next(); System.out.printf("lookup by UNINDEXED register_no (full table scan): %.1f ms%n", ms(t)); }
            }
        }

        System.out.println("\n=== 4. CONCURRENT COUNTERS: 8 counters x 500 passes = 4,000 issues, each takes the NEXT issue number ===");
        int T = 8, PER = 500;
        // (a) text file, no coordination: read last number, then append (classic lost-update race)
        File cf = new File("data/bench/issues_unsafe.txt"); Files.deleteIfExists(cf.toPath()); cf.createNewFile();
        t = System.nanoTime(); runThreads(T, () -> { for (int i = 0; i < PER; i++) issueTextUnsafe(cf); });
        double tUnsafe = ms(t); int[] r = analyse(cf);
        System.out.printf("(a) text file, no locking  : %6.0f ms  lines=%d  DISTINCT issue numbers=%d  DUPLICATES=%d%n", tUnsafe, r[0], r[1], r[0] - r[1]);
        // (b) text file with a mutual-exclusion lock around read+append (correct, but serialises all counters)
        File cl = new File("data/bench/issues_locked.txt"); Files.deleteIfExists(cl.toPath()); cl.createNewFile();
        final Object LOCK = new Object();
        t = System.nanoTime(); runThreads(T, () -> { for (int i = 0; i < PER; i++) synchronized (LOCK) { issueTextUnsafe(cl); } });
        double tLocked = ms(t); r = analyse(cl);
        System.out.printf("(b) text file + lock       : %6.0f ms  lines=%d  DISTINCT issue numbers=%d  DUPLICATES=%d%n", tLocked, r[0], r[1], r[0] - r[1]);
        // (c) database: AUTO_INCREMENT primary key, one connection per counter
        double tDb; int dbRows, dbDistinct;
        try (Connection c0 = DriverManager.getConnection(PassLoader.URL, PassLoader.USER, PassLoader.PASS)) {
            try (Statement s = c0.createStatement()) { s.execute("TRUNCATE TABLE issue_counter"); }
            AtomicInteger id = new AtomicInteger();
            t = System.nanoTime();
            runThreads(T, () -> {
                int me = id.incrementAndGet();
                try (Connection c = DriverManager.getConnection(PassLoader.URL, PassLoader.USER, PassLoader.PASS);
                     PreparedStatement ps = c.prepareStatement("INSERT INTO issue_counter(counter_id, register_no) VALUES (?,?)")) {
                    for (int i = 0; i < PER; i++) { ps.setInt(1, me); ps.setString(2, "R" + me + "-" + i); ps.executeUpdate(); }
                } catch (SQLException e) { throw new RuntimeException(e); }
            });
            tDb = ms(t);
            try (Statement s = c0.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*), COUNT(DISTINCT issue_no) FROM issue_counter")) { rs.next(); dbRows = rs.getInt(1); dbDistinct = rs.getInt(2); }
        }
        System.out.printf("(c) database AUTO_INCREMENT: %6.0f ms  rows=%d   DISTINCT issue numbers=%d  DUPLICATES=%d%n", tDb, dbRows, dbDistinct, dbRows - dbDistinct);
        csv.append(String.format("concurrency,unsafe_text_ms,%.0f%nconcurrency,unsafe_text_duplicates,%d%nconcurrency,locked_text_ms,%.0f%nconcurrency,db_ms,%.0f%nconcurrency,db_duplicates,%d%n",
                tUnsafe, analyse(cf)[0] - analyse(cf)[1], tLocked, tDb, dbRows - dbDistinct));
        Files.writeString(Paths.get("results/decision_measurements.csv"), csv.toString());
    }

    /** read the highest issue number in the file, then append number+1 (not atomic!). */
    static void issueTextUnsafe(File f) {
        try {
            int last = 0;
            try (BufferedReader br = new BufferedReader(new FileReader(f))) { String s, prev = null; while ((s = br.readLine()) != null) prev = s; if (prev != null) last = Integer.parseInt(prev.split("\\|")[0]); }
            Thread.yield();                                            // models real latency between "read number" and "write pass"
            try (FileWriter w = new FileWriter(f, true)) { w.write((last + 1) + "|counter\n"); }
        } catch (IOException e) { throw new RuntimeException(e); }
    }
    static int[] analyse(File f) throws IOException {
        List<String> ls = Files.readAllLines(f.toPath()); Set<String> d = new HashSet<>();
        for (String s : ls) if (!s.isBlank()) d.add(s.split("\\|")[0]);
        return new int[]{ls.size(), d.size()};
    }
    static void runThreads(int n, Runnable r) throws InterruptedException {
        Thread[] ts = new Thread[n]; for (int i = 0; i < n; i++) { ts[i] = new Thread(r); ts[i].start(); } for (Thread x : ts) x.join();
    }
}
