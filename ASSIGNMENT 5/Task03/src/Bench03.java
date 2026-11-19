import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** B3 timings: Statement / PreparedStatement / batch / multi-row / LOAD DATA, auto-commit vs one transaction. */
public class Bench03 {
    interface Job { int run(PassLoader l, List<PassRecord> rows) throws Exception; }

    static double time(Connection c, boolean tx, List<PassRecord> rows, Job j) throws Exception {
        PassLoader.truncate(c);
        PassLoader l = new PassLoader(c);
        long t = System.nanoTime();
        if (tx) c.setAutoCommit(false);
        int n = j.run(l, rows);
        if (tx) { c.commit(); c.setAutoCommit(true); }
        double ms = (System.nanoTime() - t) / 1e6;
        if (PassLoader.count(c) != rows.size() || n != rows.size()) throw new IllegalStateException("row count mismatch");
        return ms;
    }

    public static void main(String[] args) throws Exception {
        int[] sizes = {1000, 10000, 100000};
        StringBuilder csv = new StringBuilder("rows,method,commit_mode,ms\n");
        String urlBulk  = PassLoader.URL;                              // driver default: bulk protocol for batches
        String urlPlain = PassLoader.URL + "?useBulkStmts=false";      // classic one-statement-per-row batch
        try (Connection c = DriverManager.getConnection(urlBulk, PassLoader.USER, PassLoader.PASS);
             Connection cp = DriverManager.getConnection(urlPlain, PassLoader.USER, PassLoader.PASS)) {
            // warm-up so JIT / connection are not charged to the first measured row
            List<PassRecord> w = PassLoader.generate(500, 900000);
            time(c, false, w, (l, r) -> l.insertWithPrepared(r));
            time(c, false, w, (l, r) -> l.insertWithBatch(r));

            System.out.printf("%-34s %-12s %10s %10s %10s%n", "method", "commit", "1,000", "10,000", "100,000");
            Object[][] plan = {
                {"Statement",                     false, (Job) (l, r) -> l.insertWithStatement(r), c},
                {"PreparedStatement",             false, (Job) (l, r) -> l.insertWithPrepared(r),  c},
                {"PreparedStatement batch(1000)", false, (Job) (l, r) -> l.insertWithBatch(r),     c},
                {"Statement",                     true,  (Job) (l, r) -> l.insertWithStatement(r), c},
                {"PreparedStatement",             true,  (Job) (l, r) -> l.insertWithPrepared(r),  c},
                {"PreparedStatement batch(1000)", true,  (Job) (l, r) -> l.insertWithBatch(r),     c},
                {"batch(1000) useBulkStmts=false",false, (Job) (l, r) -> l.insertWithBatch(r),     cp},
                {"batch(1000) useBulkStmts=false",true,  (Job) (l, r) -> l.insertWithBatch(r),     cp},
                {"multi-row INSERT (1000/stmt)",  true,  (Job) (l, r) -> l.insertMultiRow(r),      c},
            };
            for (Object[] p : plan) {
                String name = (String) p[0]; boolean tx = (Boolean) p[1]; Job job = (Job) p[2]; Connection conn = (Connection) p[3];
                double[] ms = new double[sizes.length];
                for (int i = 0; i < sizes.length; i++) {
                    List<PassRecord> rows = PassLoader.generate(sizes[i], 100000);
                    ms[i] = time(conn, tx, rows, job);
                    csv.append(String.format("%d,%s,%s,%.1f%n", sizes[i], name, tx ? "1 transaction" : "auto-commit", ms[i]));
                }
                System.out.printf("%-34s %-12s %10.0f %10.0f %10.0f   (ms)%n", name, tx ? "1 txn" : "auto-commit", ms[0], ms[1], ms[2]);
            }

            // LOAD DATA LOCAL INFILE (server-side bulk loader), if the server allows it
            double[] ld = new double[sizes.length]; boolean ok = true;
            try {
                try (Statement s = c.createStatement()) { s.execute("SET GLOBAL local_infile=1"); } catch (SQLException ignore) {}
                try (Connection cl = DriverManager.getConnection(PassLoader.URL + "?allowLocalInfile=true", PassLoader.USER, PassLoader.PASS)) {
                    for (int i = 0; i < sizes.length; i++) {
                        List<PassRecord> rows = PassLoader.generate(sizes[i], 100000);
                        File f = File.createTempFile("passes", ".tsv");
                        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
                            for (PassRecord r : rows) { bw.write(r.toLine().replace('|', '\t')); bw.newLine(); }
                        }
                        PassLoader.truncate(cl);
                        long t = System.nanoTime();
                        try (Statement s = cl.createStatement()) {
                            s.execute("LOAD DATA LOCAL INFILE '" + f.getAbsolutePath().replace("\\", "/") + "' INTO TABLE bus_pass FIELDS TERMINATED BY '\\t' LINES TERMINATED BY '\\n'");
                        }
                        ld[i] = (System.nanoTime() - t) / 1e6;
                        if (PassLoader.count(cl) != sizes[i]) throw new IllegalStateException("LOAD DATA row mismatch");
                        csv.append(String.format("%d,LOAD DATA LOCAL INFILE,server bulk load,%.1f%n", sizes[i], ld[i]));
                        f.delete();
                    }
                }
            } catch (Exception e) { ok = false; System.out.println("LOAD DATA not run: " + e.getMessage().split("\n")[0]); }
            if (ok) System.out.printf("%-34s %-12s %10.0f %10.0f %10.0f   (ms)%n", "LOAD DATA LOCAL INFILE", "bulk", ld[0], ld[1], ld[2]);
            PassLoader.truncate(c);
        }
        Files.writeString(Paths.get("results/jdbc_insert_times.csv"), csv.toString());
    }
}
