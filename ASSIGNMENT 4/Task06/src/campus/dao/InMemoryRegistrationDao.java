package campus.dao;

import campus.model.EventInfo;
import campus.model.Registration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** Fake DAO used for the --demo mode, unit tests and the UI-side benchmark. Behaves like the MySQL version. */
public class InMemoryRegistrationDao implements RegistrationDao {
    private final List<EventInfo> events = List.of(
            new EventInfo(1, "Hackathon 2026"), new EventInfo(2, "Tech Talk: AI Safety"),
            new EventInfo(3, "Cultural Fest"), new EventInfo(4, "Sports Day"), new EventInfo(5, "Career Fair"));
    private final List<Registration> rows = new ArrayList<>();
    private final Set<String> keys = new HashSet<>();
    private long nextId = 1;

    /** Adds n synthetic rows (like seed_100k.sql). */
    public InMemoryRegistrationDao(int syntheticRows) {
        for (int n = 1; n <= syntheticRows; n++) {
            int ev = 1 + (n % 5);
            add(new Registration(0, String.format("T%06d", n), "Test Student " + n, "test" + n + "@campus.edu",
                    ev, nameOf(ev), LocalDateTime.now()));
        }
    }

    private String nameOf(int id) { return events.get(id - 1).name(); }
    private static String key(String s, int e) { return s.toUpperCase(Locale.ROOT) + "|" + e; }

    private synchronized void add(Registration r) {
        rows.add(new Registration(nextId++, r.studentId(), r.studentName(), r.email(), r.eventId(), r.eventName(), r.registeredAt()));
        keys.add(key(r.studentId(), r.eventId()));
    }

    @Override public List<EventInfo> findEvents() { return events; }
    @Override public synchronized boolean exists(String s, int e) { return keys.contains(key(s, e)); }

    @Override
    public synchronized void insert(Registration r) {
        if (keys.contains(key(r.studentId(), r.eventId()))) throw new DuplicateKeyException(null);
        add(new Registration(0, r.studentId(), r.studentName(), r.email(), r.eventId(), nameOf(r.eventId()), r.registeredAt()));
    }

    private List<Registration> filtered(Integer eventId) {
        return eventId == null ? rows : rows.stream().filter(r -> r.eventId() == eventId).collect(Collectors.toList());
    }

    @Override
    public synchronized List<Registration> findPage(Integer eventId, int limit, int offset) {
        List<Registration> f = filtered(eventId);
        int from = Math.min(offset, f.size());
        return new ArrayList<>(f.subList(from, Math.min(f.size(), from + limit)));
    }

    @Override public synchronized List<Registration> findAll(Integer eventId) { return new ArrayList<>(filtered(eventId)); }
    @Override public synchronized int count(Integer eventId) { return filtered(eventId).size(); }
}
