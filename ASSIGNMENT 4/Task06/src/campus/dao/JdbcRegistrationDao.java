package campus.dao;

import campus.model.EventInfo;
import campus.model.Registration;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** MySQL implementation using JDBC. Only this class knows SQL. All statements are parameterised and closed automatically. */
public class JdbcRegistrationDao implements RegistrationDao {
    private static final String SELECT_BASE =
        "SELECT r.reg_id, r.student_id, r.student_name, r.email, r.event_id, e.name, r.registered_at "
      + "FROM registrations r JOIN events e ON e.event_id = r.event_id ";

    private final String url, user, password;

    public JdbcRegistrationDao(String url, String user, String password) {
        this.url = url; this.user = user; this.password = password;
    }

    /** Reads db.properties (url, user, password) from the given path. */
    public static JdbcRegistrationDao fromProperties(String path) throws IOException {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(path)) { p.load(in); }
        return new JdbcRegistrationDao(p.getProperty("url"), p.getProperty("user"), p.getProperty("password"));
    }

    private Connection open() throws SQLException { return DriverManager.getConnection(url, user, password); }

    @Override
    public List<EventInfo> findEvents() {
        try (Connection c = open(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT event_id, name FROM events ORDER BY event_date")) {
            List<EventInfo> list = new ArrayList<>();
            while (rs.next()) list.add(new EventInfo(rs.getInt(1), rs.getString(2)));
            return list;
        } catch (SQLException e) { throw new DataAccessException("Could not load events", e); }
    }

    @Override
    public boolean exists(String studentId, int eventId) {
        String sql = "SELECT 1 FROM registrations WHERE student_id = ? AND event_id = ? LIMIT 1";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId); ps.setInt(2, eventId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new DataAccessException("Could not check duplicate", e); }
    }

    @Override
    public void insert(Registration r) {
        String sql = "INSERT INTO registrations (student_id, student_name, email, event_id, registered_at) VALUES (?,?,?,?,?)";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.studentId()); ps.setString(2, r.studentName()); ps.setString(3, r.email());
            ps.setInt(4, r.eventId()); ps.setTimestamp(5, Timestamp.valueOf(r.registeredAt()));
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DuplicateKeyException(e);                       // two people clicking at once still cannot create a duplicate
        } catch (SQLException e) { throw new DataAccessException("Could not save registration", e); }
    }

    @Override
    public List<Registration> findPage(Integer eventId, int limit, int offset) {
        String sql = SELECT_BASE + "WHERE (? IS NULL OR r.event_id = ?) ORDER BY r.reg_id LIMIT ? OFFSET ?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            bindEvent(ps, 1, eventId);
            ps.setInt(3, limit); ps.setInt(4, offset);
            return readAll(ps);
        } catch (SQLException e) { throw new DataAccessException("Could not load registrations", e); }
    }

    @Override
    public List<Registration> findAll(Integer eventId) {
        String sql = SELECT_BASE + "WHERE (? IS NULL OR r.event_id = ?) ORDER BY r.reg_id";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            bindEvent(ps, 1, eventId);
            return readAll(ps);
        } catch (SQLException e) { throw new DataAccessException("Could not load registrations", e); }
    }

    @Override
    public int count(Integer eventId) {
        String sql = "SELECT COUNT(*) FROM registrations WHERE (? IS NULL OR event_id = ?)";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            bindEvent(ps, 1, eventId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { throw new DataAccessException("Could not count registrations", e); }
    }

    private static void bindEvent(PreparedStatement ps, int index, Integer eventId) throws SQLException {
        if (eventId == null) { ps.setNull(index, Types.INTEGER); ps.setNull(index + 1, Types.INTEGER); }
        else { ps.setInt(index, eventId); ps.setInt(index + 1, eventId); }
    }

    private static List<Registration> readAll(PreparedStatement ps) throws SQLException {
        List<Registration> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Registration(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getInt(5), rs.getString(6), rs.getTimestamp(7).toLocalDateTime()));
            }
        }
        return list;
    }
}
