package campus.tools;

import campus.model.Registration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.DefaultTableModel;

/**
 * UI-side cost of showing 100,000 rows in one table model vs one page of 50 rows (no database involved, no window needed).
 * Complements LoadBenchmark, which measures the database side. Run: java -cp out campus.tools.TableModelBenchmark
 */
public class TableModelBenchmark {
    public static void main(String[] args) {
        System.out.printf("%-22s %-14s %-18s%n", "Approach", "Time (ms)", "Heap used (MB)");
        for (int run = 1; run <= 3; run++) {
            measure("Run " + run + " - all 100000", 100_000);
            measure("Run " + run + " - page of 50", 50);
        }
    }

    static void measure(String label, int rows) {
        System.gc();
        long before = used();
        long t0 = System.nanoTime();
        List<Registration> data = new ArrayList<>(rows);                    // what the DAO would return
        for (int n = 1; n <= rows; n++) {
            data.add(new Registration(n, String.format("T%06d", n), "Test Student " + n, "test" + n + "@campus.edu",
                    1 + n % 5, "Hackathon 2026", LocalDateTime.now()));
        }
        DefaultTableModel model = new DefaultTableModel(new Object[] {"Reg ID", "Student ID", "Name", "E-mail", "Event", "Registered at"}, 0);
        for (Registration r : data) {                                       // what MainFrame.done() does
            model.addRow(new Object[] {r.id(), r.studentId(), r.studentName(), r.email(), r.eventName(),
                    r.registeredAt().withNano(0).toString().replace('T', ' ')});
        }
        double ms = (System.nanoTime() - t0) / 1e6;
        long heap = used() - before;
        System.out.printf("%-22s %-14.1f %-18.2f (rows in model: %d)%n", label, ms, heap / 1048576.0, model.getRowCount());
    }

    private static long used() { Runtime r = Runtime.getRuntime(); return r.totalMemory() - r.freeMemory(); }
}
