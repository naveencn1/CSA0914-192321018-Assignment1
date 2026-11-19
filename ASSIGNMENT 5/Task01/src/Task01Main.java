import java.io.*;
import java.nio.file.*;
import java.util.*;

/** B1 functional tests: normal, missing id, damaged line, missing file, route listing. */
public class Task01Main {
    public static void main(String[] args) throws Exception {
        String f = args.length > 0 ? args[0] : "data/passes_sample.txt";
        Files.createDirectories(Paths.get(f).getParent());
        Files.deleteIfExists(Paths.get(f));
        PassStore s = new PassStore(f);

        System.out.println("== TEST 1: add three passes (append + fsync) ==");
        s.add(new PassRecord("P1001", "192321018", "V V Naveen Kumar", "12", "Sriperumbudur", "2027-03-31", 6500));
        // P1002 is written damaged by hand: only 5 fields (simulates truncated/corrupted line)
        try (FileWriter w = new FileWriter(f, true)) { w.write("P1002|192321019|Anita|12|Guindy\n"); }
        s.add(new PassRecord("P1003", "192321020", "Ravi", "7", "Tambaram", "2027-03-31", 6200));

        System.out.println("== TEST 2: find existing id P1003 (normal case) ==");
        System.out.println(s.find("P1003"));
        System.out.println("== TEST 3: find damaged P1002 (expected: null) ==");
        System.out.println(s.find("P1002"));
        System.out.println("== TEST 4: find non-existent id P9999 (expected: null) ==");
        System.out.println(s.find("P9999"));
        System.out.println("== TEST 5: search continues past a damaged line (P1003 sits AFTER damaged P1002) ==");
        System.out.println(s.find("P1003") != null ? "PASS: P1003 found after damaged line" : "FAIL");
        System.out.println("== TEST 6: list route 12 ==");
        int[] bad = {0};
        for (PassRecord r : s.listByRoute("12", bad)) System.out.println(r);
        System.out.println("damaged lines skipped while listing: " + bad[0]);
        System.out.println("== TEST 7: missing file (expected: null, no exception) ==");
        System.out.println(new PassStore("data/does_not_exist.txt").find("P1001"));
    }
}
