import java.sql.*;
import java.util.*;

/** B3 functional part: 10-row load with each approach, SELECT + ResultSetMetaData, failure/rollback test. */
public class Task03Main {
    public static void main(String[] args) throws Exception {
        try (Connection c = DriverManager.getConnection(PassLoader.URL, PassLoader.USER, PassLoader.PASS)) {
            PassLoader L = new PassLoader(c);

            System.out.println("=== PART 1: insert 10 records with each approach ===");
            String[] names = {"Statement", "PreparedStatement", "PreparedStatement batch"};
            for (int k = 0; k < 3; k++) {
                PassLoader.truncate(c);
                List<PassRecord> rows = PassLoader.generate(10, 1000);
                int n = k == 0 ? L.insertWithStatement(rows) : k == 1 ? L.insertWithPrepared(rows) : L.insertWithBatch(rows);
                System.out.printf("%-26s inserted=%d  rows in table=%d%n", names[k], n, PassLoader.count(c));
            }

            System.out.println("\n=== PART 2: SELECT * + ResultSetMetaData (table holds the 10 batch-inserted rows) ===");
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT * FROM bus_pass ORDER BY pass_id")) {
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                System.out.println("-- column metadata --");
                for (int i = 1; i <= cols; i++)
                    System.out.printf("%d. %-14s %-10s java=%-20s size=%-4d nullable=%s%n", i, md.getColumnName(i),
                            md.getColumnTypeName(i), md.getColumnClassName(i), md.getPrecision(i),
                            md.isNullable(i) == ResultSetMetaData.columnNoNulls ? "NO" : "YES");
                System.out.println("-- rows --");
                while (rs.next()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i <= cols; i++) sb.append(rs.getString(i)).append(i < cols ? " | " : "");
                    System.out.println(sb);
                }
            }

            System.out.println("\n=== PART 3: invalid amount HALFWAY through 1,000 rows (row 500 has amount = -1) ===");
            List<PassRecord> bad = PassLoader.generate(1000, 5000);
            PassRecord r = bad.get(499);
            bad.set(499, new PassRecord(r.passId, r.registerNo, r.name, r.routeNo, r.boardingStop, r.validUntil, -1));

            PassLoader.truncate(c);
            System.out.println("-- A) AUTO-COMMIT, batches of 100 so the failure lands mid-way --");
            c.setAutoCommit(true);
            try (PreparedStatement ps = c.prepareStatement(PassLoader.INSERT_SQL)) {
                int pending = 0;
                for (PassRecord x : bad) {
                    PassLoader.bind(ps, x); ps.addBatch();
                    if (++pending == 100) { ps.executeBatch(); pending = 0; }
                }
                if (pending > 0) ps.executeBatch();
            } catch (SQLException e) {
                System.out.println("failure: " + firstLine(e));
            }
            System.out.println("rows left in table after failure (auto-commit): " + PassLoader.count(c) + "   <-- PARTIAL DATA, inconsistent");

            PassLoader.truncate(c);
            System.out.println("-- B) SINGLE TRANSACTION (commit/rollback) --");
            try { L.insertAllOrNothing(bad); }
            catch (SQLException e) { System.out.println("failure: " + firstLine(e) + "\nrollback executed"); }
            System.out.println("rows left in table after failure (transaction): " + PassLoader.count(c) + "   <-- all-or-nothing");

            PassLoader.truncate(c);
            List<PassRecord> good = PassLoader.generate(1000, 5000);
            System.out.println("-- C) same transaction code with VALID data --");
            System.out.println("inserted=" + L.insertAllOrNothing(good) + " rows in table=" + PassLoader.count(c));
        }
    }
    static String firstLine(Exception e) { String m = e.getMessage(); return m == null ? e.toString() : m.split("\n")[0]; }
}
