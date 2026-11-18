package campus.service;

import campus.dao.DuplicateKeyException;
import campus.dao.RegistrationDao;
import campus.model.EventInfo;
import campus.model.Registration;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Business rules: validation, duplicate check, export. Knows nothing about Swing or SQL. */
public class RegistrationService {
    private static final Pattern STUDENT_ID = Pattern.compile("[A-Za-z0-9]{6,12}");
    private static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z .'-]{1,59}");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    public static final int PAGE_SIZE = 50;

    private final RegistrationDao dao;

    public RegistrationService(RegistrationDao dao) { this.dao = dao; }

    public List<EventInfo> events() { return dao.findEvents(); }

    /** Validate -> duplicate check -> insert. */
    public Registration register(String studentId, String name, String email, EventInfo event) {
        List<String> problems = new ArrayList<>();
        String id = studentId == null ? "" : studentId.trim().toUpperCase(Locale.ROOT);
        String n = name == null ? "" : name.trim();
        String mail = email == null ? "" : email.trim();
        if (!STUDENT_ID.matcher(id).matches()) problems.add("Student ID must be 6-12 letters or digits.");
        if (!NAME.matcher(n).matches()) problems.add("Name must be 2-60 letters (spaces, . ' - allowed).");
        if (!EMAIL.matcher(mail).matches()) problems.add("Enter a valid e-mail address, e.g. name@college.edu.");
        if (event == null) problems.add("Select an event.");
        if (!problems.isEmpty()) throw new ValidationException(problems);

        if (dao.exists(id, event.id())) throw new DuplicateRegistrationException(id, event.name());
        Registration r = new Registration(0, id, n, mail, event.id(), event.name(), LocalDateTime.now());
        try {
            dao.insert(r);
        } catch (DuplicateKeyException e) {          // lost a race with another committee member: DB rule is the last defence
            throw new DuplicateRegistrationException(id, event.name());
        }
        return r;
    }

    public List<Registration> page(Integer eventId, int pageIndex) { return dao.findPage(eventId, PAGE_SIZE, pageIndex * PAGE_SIZE); }
    public int count(Integer eventId) { return dao.count(eventId); }
    public static int pageCount(int total) { return Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE); }

    /** Writes every registration for the filter to a CSV file (UTF-8). Returns the number of rows written. */
    public int exportCsv(Path file, Integer eventId) throws IOException {
        List<Registration> all = dao.findAll(eventId);
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {
            out.println("RegId,StudentId,Name,Email,Event,RegisteredAt");
            for (Registration r : all) {
                out.println(String.join(",", String.valueOf(r.id()), csv(r.studentId()), csv(r.studentName()),
                        csv(r.email()), csv(r.eventName()), r.registeredAt().withNano(0).toString()));
            }
        }
        return all.size();
    }

    public static String csv(String s) {
        return (s.contains(",") || s.contains("\"") || s.contains("\n")) ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }
}
