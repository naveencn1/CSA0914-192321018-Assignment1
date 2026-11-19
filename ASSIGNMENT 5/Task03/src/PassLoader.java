import java.sql.*;
import java.util.*;

/** JDBC loader for bus passes: Statement vs PreparedStatement vs batch (TODO 1-3 completed). */
public class PassLoader {
    static final String URL  = "jdbc:mariadb://localhost:3306/csa09_assignment5";
    static final String USER = "csa09", PASS = "csa09pass";
    static final int BATCH_SIZE = 1000;

    private final Connection con;
    public PassLoader(Connection con) { this.con = con; }

    /** TODO 1 (completed): one Statement per row; SQL text built by concatenation, parsed by server EVERY time. */
    public int insertWithStatement(List<PassRecord> rows) throws SQLException {
        int n = 0;
        try (Statement st = con.createStatement()) {
            for (PassRecord r : rows) {
                String sql = "INSERT INTO bus_pass VALUES ('" + r.passId + "','" + r.registerNo + "','"
                        + r.name.replace("'", "''") + "','" + r.routeNo + "','" + r.boardingStop + "','"
                        + r.validUntil + "'," + r.amountPaid + ")";
                n += st.executeUpdate(sql);
            }
        }
        return n;
    }

    /** TODO 2 (completed): one PreparedStatement, reused; one network round trip per row. */
    public int insertWithPrepared(List<PassRecord> rows) throws SQLException {
        int n = 0;
        try (PreparedStatement ps = con.prepareStatement(INSERT_SQL)) {
            for (PassRecord r : rows) {
                bind(ps, r);
                n += ps.executeUpdate();
            }
        }
        return n;
    }

    /** TODO 3 (completed): addBatch() and executeBatch() every 1,000 rows (+ final partial batch). */
    public int insertWithBatch(List<PassRecord> rows) throws SQLException {
        int n = 0, pending = 0;
        try (PreparedStatement ps = con.prepareStatement(INSERT_SQL)) {
            for (PassRecord r : rows) {
                bind(ps, r);
                ps.addBatch();
                if (++pending == BATCH_SIZE) { n += sum(ps.executeBatch()); pending = 0; }
            }
            if (pending > 0) n += sum(ps.executeBatch());     // remaining rows
        }
        return n;
    }

    /** Alternative: multi-row INSERT ... VALUES (...),(...),... built in chunks of 1,000 rows. */
    public int insertMultiRow(List<PassRecord> rows) throws SQLException {
        int n = 0;
        for (int from = 0; from < rows.size(); from += BATCH_SIZE) {
            int to = Math.min(from + BATCH_SIZE, rows.size());
            StringBuilder sql = new StringBuilder("INSERT INTO bus_pass VALUES ");
            for (int i = from; i < to; i++) sql.append(i > from ? ",(?,?,?,?,?,?,?)" : "(?,?,?,?,?,?,?)");
            try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
                int p = 1;
                for (int i = from; i < to; i++) {
                    PassRecord r = rows.get(i);
                    ps.setString(p++, r.passId); ps.setString(p++, r.registerNo); ps.setString(p++, r.name);
                    ps.setString(p++, r.routeNo); ps.setString(p++, r.boardingStop);
                    ps.setDate(p++, java.sql.Date.valueOf(r.validUntil)); ps.setInt(p++, r.amountPaid);
                }
                n += ps.executeUpdate();
            }
        }
        return n;
    }

    /** ALL-OR-NOTHING: batch insert inside ONE transaction; any failure rolls back every row. */
    public int insertAllOrNothing(List<PassRecord> rows) throws SQLException {
        boolean old = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            int n = insertWithBatch(rows);
            con.commit();
            return n;
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(old);
        }
    }

    static final String INSERT_SQL = "INSERT INTO bus_pass VALUES (?,?,?,?,?,?,?)";
    static void bind(PreparedStatement ps, PassRecord r) throws SQLException {
        ps.setString(1, r.passId); ps.setString(2, r.registerNo); ps.setString(3, r.name);
        ps.setString(4, r.routeNo); ps.setString(5, r.boardingStop);
        ps.setDate(6, java.sql.Date.valueOf(r.validUntil)); ps.setInt(7, r.amountPaid);
    }
    static int sum(int[] counts) { int s = 0; for (int c : counts) s += (c == Statement.SUCCESS_NO_INFO ? 1 : c); return s; }

    // ---- helpers shared by the Task03/Task04 programs ----
    static final String[] STOPS = {"Sriperumbudur","Guindy","Tambaram","Porur","Poonamallee","Avadi","Kundrathur","Vandalur"};
    public static List<PassRecord> generate(int n, int startId) {
        List<PassRecord> l = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int id = startId + i;
            l.add(new PassRecord("P" + id, "19232" + (1000 + id % 9000), "Student " + id, String.valueOf(1 + id % 20),
                    STOPS[id % STOPS.length], "2027-03-31", 6000 + id % 700));
        }
        return l;
    }
    public static int count(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM bus_pass")) { rs.next(); return rs.getInt(1); }
    }
    public static void truncate(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) { s.execute("TRUNCATE TABLE bus_pass"); }
    }
}
