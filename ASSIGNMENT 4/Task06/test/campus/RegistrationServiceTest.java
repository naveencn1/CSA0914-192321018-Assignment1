package campus;

import campus.dao.InMemoryRegistrationDao;
import campus.model.EventInfo;
import campus.service.*;
import java.nio.file.*;
import java.util.List;

/** Headless tests for the service layer: normal, invalid and duplicate registrations + CSV export + paging. */
public class RegistrationServiceTest {
    static int failed = 0;
    static void check(boolean ok, String n) { System.out.println((ok ? "PASS " : "FAIL ") + n); if (!ok) failed++; }

    public static void main(String[] a) throws Exception {
        InMemoryRegistrationDao dao = new InMemoryRegistrationDao(0);
        RegistrationService s = new RegistrationService(dao);
        EventInfo hack = s.events().get(0), talk = s.events().get(1);

        // NORMAL
        check(s.register("s200001", "Asha Raman", "asha@campus.edu", hack).studentId().equals("S200001"), "normal registration succeeds (id upper-cased)");
        check(s.count(null) == 1, "count is 1 after one registration");

        // INVALID
        for (String[] bad : new String[][] {{"ab", "Asha", "a@b.co", "6-12"}, {"S200002", "A1", "a@b.co", "Name must"},
                {"S200002", "Asha", "not-an-email", "e-mail"}, {"S200002", "Asha", "a@b.co", null}}) {
            try {
                s.register(bad[0], bad[1], bad[2], bad[3] == null ? null : hack);
                check(false, "invalid input rejected: " + String.join("|", bad[0], bad[1], bad[2]));
            } catch (ValidationException e) {
                check(bad[3] == null ? e.getMessage().contains("Select an event") : e.getMessage().contains(bad[3]),
                      "invalid input rejected: " + String.join("|", bad[0], bad[1], bad[2]));
            }
        }
        check(s.count(null) == 1, "nothing saved for invalid input");

        // DUPLICATE
        try { s.register("S200001", "Asha Raman", "asha@campus.edu", hack); check(false, "duplicate rejected"); }
        catch (DuplicateRegistrationException e) { check(e.getMessage().contains("already registered"), "duplicate for same event rejected"); }
        check(s.register("S200001", "Asha Raman", "asha@campus.edu", talk) != null, "same student, DIFFERENT event is allowed");
        check(s.count(null) == 2, "count is 2");

        // FILTER + PAGING
        InMemoryRegistrationDao big = new InMemoryRegistrationDao(1000);
        RegistrationService bs = new RegistrationService(big);
        check(bs.count(null) == 1000, "1000 synthetic rows");
        check(bs.page(null, 0).size() == 50 && bs.page(null, 19).size() == 50 && bs.page(null, 20).isEmpty(), "paging: 20 pages of 50");
        check(bs.count(1) == 200, "filter by event 1 gives 200 rows (1000 / 5)");
        check(RegistrationService.pageCount(1000) == 20 && RegistrationService.pageCount(0) == 1, "pageCount");

        // EXPORT
        Path tmp = Files.createTempFile("reg", ".csv");
        int rows = s.exportCsv(tmp, null);
        List<String> lines = Files.readAllLines(tmp);
        check(rows == 2 && lines.size() == 3 && lines.get(0).startsWith("RegId,StudentId"), "CSV export: header + 2 rows");
        check(RegistrationService.csv("Smith, John").equals("\"Smith, John\""), "CSV escapes commas");
        Files.delete(tmp);

        System.out.println(failed == 0 ? "ALL TESTS PASSED" : failed + " FAILED");
        System.exit(failed);
    }
}
