import java.sql.*;

/** B2: unsafe string-concatenated login vs PreparedStatement login. */
public class LoginDemo {
    static final String URL  = "jdbc:mariadb://localhost:3306/csa09_assignment5";
    static final String USER = "csa09", PASS = "csa09pass";

    /** UNSAFE: user input is joined directly into the SQL text. */
    static boolean unsafeLogin(Connection c, String u, String p, StringBuilder sqlOut) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = '" + u + "' AND password = '" + p + "'";
        sqlOut.append(sql);
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next();
        } catch (SQLException e) { sqlOut.append("   [SQL ERROR: ").append(e.getMessage()).append("]"); return false; }
    }

    /** SAFE: SQL structure is fixed; input is bound as DATA, never parsed as SQL. */
    static boolean safeLogin(Connection c, String u, String p) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u);
            ps.setString(2, p);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public static void main(String[] args) throws Exception {
        String[][] cases = {
            {"T1 valid credentials",               "naveen", "sim@ts2026"},
            {"T2 wrong password (plain)",          "naveen", "wrong"},
            {"T3 ATTACK  ' OR '1'='1 in password", "naveen", "x' OR '1'='1"},
            {"T4 ATTACK  comment-out password",    "admin' -- ", "anything"},
            {"T5 ATTACK  unknown user + tautology","nosuchuser' OR 1=1 -- ", "x"},
            {"T6 ATTACK  quote only (syntax probe)","a'", "b"},
        };
        try (Connection c = DriverManager.getConnection(URL, USER, PASS)) {
            System.out.printf("%-40s | %-8s | %-8s%n", "TEST", "UNSAFE", "PREPARED");
            System.out.println("-".repeat(64));
            for (String[] t : cases) {
                StringBuilder sql = new StringBuilder();
                boolean u = unsafeLogin(c, t[1], t[2], sql);
                boolean s = safeLogin(c, t[1], t[2]);
                System.out.printf("%-40s | %-8s | %-8s%n", t[0], u ? "ACCEPTED" : "rejected", s ? "ACCEPTED" : "rejected");
                System.out.println("    unsafe SQL sent: " + sql);
            }
        }
    }
}
