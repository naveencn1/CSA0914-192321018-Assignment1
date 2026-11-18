package campus.tools;

import campus.dao.JdbcRegistrationDao;
import campus.dao.RegistrationDao;
import campus.model.Registration;
import campus.service.RegistrationService;
import java.util.List;

/**
 * Full load vs one page, measured against the real MySQL database.
 *   java -cp "out:lib/*" campus.tools.LoadBenchmark db.properties   -> MySQL with seed_100k.sql loaded
 * Put the printed numbers in the report (they depend on your machine and database).
 */
public class LoadBenchmark {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) { System.out.println("Usage: java -cp out:lib/* campus.tools.LoadBenchmark db.properties   (needs MySQL with seed_100k.sql loaded)"); return; }
        RegistrationDao dao = JdbcRegistrationDao.fromProperties(args[0]);
        System.out.println("DAO: " + dao.getClass().getSimpleName());
        for (int run = 1; run <= 3; run++) {
            System.gc();
            long memBefore = used();
            long t0 = System.nanoTime();
            List<Registration> all = dao.findAll(null);
            double fullMs = (System.nanoTime() - t0) / 1e6;
            long fullMem = used() - memBefore;
            int n = all.size();
            all = null;

            System.gc();
            memBefore = used();
            t0 = System.nanoTime();
            List<Registration> page = dao.findPage(null, RegistrationService.PAGE_SIZE, 0);
            double pageMs = (System.nanoTime() - t0) / 1e6;
            long pageMem = used() - memBefore;
            t0 = System.nanoTime();
            List<Registration> deep = dao.findPage(null, RegistrationService.PAGE_SIZE, Math.max(0, n - RegistrationService.PAGE_SIZE));
            double deepMs = (System.nanoTime() - t0) / 1e6;
            System.out.printf("run %d: full load %d rows = %.1f ms, ~%d KB | page 1 (%d rows) = %.1f ms, ~%d KB | last page (%d rows) = %.1f ms%n",
                    run, n, fullMs, fullMem / 1024, page.size(), pageMs, Math.max(0, pageMem) / 1024, deep.size(), deepMs);
        }
    }

    private static long used() { Runtime r = Runtime.getRuntime(); return r.totalMemory() - r.freeMemory(); }
}
