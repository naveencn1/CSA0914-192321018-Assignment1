import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * MySQL implementation. PreparedStatement (no SQL injection), try-with-resources (always closed),
 * one transaction for balance update + payment row.
 * Tables: fee_accounts(student_id PK, balance DECIMAL(10,2)), payments(receipt_no AUTO_INCREMENT PK,
 *         student_id, amount DECIMAL(10,2), mode VARCHAR(10), paid_at DATETIME)
 */
public class JdbcFeeRepository implements FeeRepository {
    private final String url, user, password;

    public JdbcFeeRepository(String url, String user, String password) {
        this.url = url; this.user = user; this.password = password;
    }

    @Override
    public Optional<BigDecimal> findBalance(String studentId) {
        String sql = "SELECT balance FROM fee_accounts WHERE student_id = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getBigDecimal(1)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read fee balance", e);
        }
    }

    @Override
    public long recordPayment(String studentId, BigDecimal amount, String mode, LocalDateTime when) {
        String upd = "UPDATE fee_accounts SET balance = balance - ? WHERE student_id = ?";
        String ins = "INSERT INTO payments(student_id, amount, mode, paid_at) VALUES (?,?,?,?)";
        try (Connection con = DriverManager.getConnection(url, user, password)) {
            con.setAutoCommit(false);
            try (PreparedStatement u = con.prepareStatement(upd);
                 PreparedStatement i = con.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
                u.setBigDecimal(1, amount); u.setString(2, studentId); u.executeUpdate();
                i.setString(1, studentId); i.setBigDecimal(2, amount); i.setString(3, mode);
                i.setTimestamp(4, Timestamp.valueOf(when));
                i.executeUpdate();
                try (ResultSet keys = i.getGeneratedKeys()) {
                    keys.next();
                    long receipt = keys.getLong(1);
                    con.commit();
                    return receipt;
                }
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not record payment", e);
        }
    }
}
