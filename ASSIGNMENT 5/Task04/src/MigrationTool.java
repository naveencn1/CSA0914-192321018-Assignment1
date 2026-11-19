import java.io.*;
import java.sql.*;
import java.util.*;

/** B4: migrate a text pass file into MySQL/MariaDB using batches of 1,000 inside ONE transaction. */
public class MigrationTool {
    static final int BATCH = 1000;

    public static void main(String[] args) throws Exception {
        String in = args[0], rejectsFile = args[1];
        try (Connection c = DriverManager.getConnection(PassLoader.URL, PassLoader.USER, PassLoader.PASS)) {
            report(c);
            try (Statement s = c.createStatement()) { s.execute("TRUNCATE TABLE bus_pass_migrated"); }

            long t0 = System.nanoTime();
            int lineNo = 0, valid = 0, rejected = 0, pending = 0;
            Map<String, Integer> reasons = new TreeMap<>();
            Set<String> seen = new HashSet<>();
            c.setAutoCommit(false);                                   // ONE transaction for the whole migration
            try (BufferedReader br = new BufferedReader(new FileReader(in));
                 PrintWriter rej = new PrintWriter(new FileWriter(rejectsFile));
                 PreparedStatement ps = c.prepareStatement("INSERT INTO bus_pass_migrated VALUES (?,?,?,?,?,?,?)")) {
                rej.println("line_no|reason|raw_line");
                String line;
                while ((line = br.readLine()) != null) {
                    lineNo++;
                    String why = PassValidator.validate(line, seen);
                    if (why != null) {                                // report records that fail validation
                        rejected++; rej.println(lineNo + "|" + why + "|" + line);
                        reasons.merge(why.replaceAll("\\s*[('].*$|\\s+P?[0-9].*$", "").trim(), 1, Integer::sum);
                        continue;
                    }
                    PassRecord r = PassRecord.parse(line);
                    PassLoader.bind(ps, r);
                    ps.addBatch(); valid++;
                    if (++pending == BATCH) { ps.executeBatch(); pending = 0; }
                }
                if (pending > 0) ps.executeBatch();
                c.commit();
            } catch (Exception e) {
                c.rollback();                                         // any DB failure -> nothing half-migrated
                System.out.println("MIGRATION FAILED, rolled back: " + e.getMessage());
                throw e;
            } finally { c.setAutoCommit(true); }
            double secs = (System.nanoTime() - t0) / 1e9;

            int inDb;
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM bus_pass_migrated")) { rs.next(); inDb = rs.getInt(1); }
            System.out.println("\n=== MIGRATION RESULT ===");
            System.out.printf("lines read=%d  loaded=%d  rejected=%d  rows in DB=%d  consistent=%s%n", lineNo, valid, rejected, inDb, (inDb == valid && valid + rejected == lineNo));
            System.out.printf("time=%.2f s  (%.0f records/s)%n", secs, valid / secs);
            System.out.println("rejection summary: " + reasons);
            System.out.println("rejected lines written to: " + rejectsFile);
        }
    }

    /** DatabaseMetaData report: product, driver version, tables. */
    static void report(Connection c) throws SQLException {
        DatabaseMetaData md = c.getMetaData();
        System.out.println("=== DatabaseMetaData ===");
        System.out.println("Database product : " + md.getDatabaseProductName() + " " + md.getDatabaseProductVersion());
        System.out.println("Driver           : " + md.getDriverName() + " " + md.getDriverVersion()
                + " (JDBC " + md.getJDBCMajorVersion() + "." + md.getJDBCMinorVersion() + ")");
        System.out.println("URL / user       : " + md.getURL() + " / " + md.getUserName());
        System.out.println("Supports transactions: " + md.supportsTransactions() + ", batch updates: " + md.supportsBatchUpdates());
        System.out.println("Tables in catalog '" + c.getCatalog() + "':");
        try (ResultSet rs = md.getTables(c.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (rs.next()) System.out.println("  - " + rs.getString("TABLE_NAME"));
        }
    }
}
