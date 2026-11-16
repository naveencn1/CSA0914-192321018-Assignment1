import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Q3 - hashCode/equals contract.
 * NOTE: the slide shows only part of the program (the main method is cut off), so the
 * main() below is reconstructed to match the expected output "Unique students: 2".
 *
 * RUN 1 : keep TODO 1 commented out  -> duplicates appear (Unique students: 3)
 * RUN 2 : un-comment TODO 1          -> Unique students: 2
 */
public class Q3Contract {
    public static void main(String[] args) {
        // ---- Program required by the slide ----
        Set<Student> scans = new HashSet<>();
        scans.add(new Student("21CS001", "Asha"));
        scans.add(new Student("21CS001", "Asha"));   // same student scanned at another gate
        scans.add(new Student("21CS002", "Ravi"));
        System.out.println("Unique students: " + scans.size());

        // ---- Test 1: duplicates with and without hashCode ----
        System.out.println("\nTest 1 - 1000 scans of 10 distinct students");
        Set<BrokenStudent> broken = new HashSet<>();
        Set<Student> fixed = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            broken.add(new BrokenStudent("R" + (i % 10)));
            fixed.add(new Student("R" + (i % 10), "N"));
        }
        System.out.println("  Predicted: broken=1000 (almost), fixed=10");
        System.out.println("  Observed : broken=" + broken.size() + ", fixed=" + fixed.size());

        // ---- Test 2: contains() on equal object ----
        System.out.println("\nTest 2 - contains(equal object)");
        BrokenStudent b1 = new BrokenStudent("X1");
        Set<BrokenStudent> bs = new HashSet<>(); bs.add(b1);
        System.out.println("  broken.contains(new BrokenStudent(\"X1\")) = "
            + bs.contains(new BrokenStudent("X1")) + "  (equals says true, HashSet looks in wrong bucket)");
        Set<Student> fs = new HashSet<>(); fs.add(new Student("X1", "A"));
        System.out.println("  fixed.contains(new Student(\"X1\",\"B\"))   = " + fs.contains(new Student("X1", "B")));

        // ---- Test 3: mutating a field used in hashCode after insertion ----
        System.out.println("\nTest 3 - change regNo AFTER adding to the HashSet");
        MutableStudent m = new MutableStudent("A100");
        Set<MutableStudent> ms = new HashSet<>(); ms.add(m);
        System.out.println("  before change contains(m) = " + ms.contains(m));
        m.regNo = "B200";
        System.out.println("  after  change contains(m) = " + ms.contains(m) + "  (lost: stored in old bucket)");
        System.out.println("  set size = " + ms.size() + ", but contains(new MutableStudent(\"B200\")) = "
            + ms.contains(new MutableStudent("B200")));
        ms.add(new MutableStudent("B200"));
        System.out.println("  adding an 'equal' B200 now gives size = " + ms.size() + " (duplicate!)");
    }
}

class Student {
    private final String regNo;
    private final String name;

    Student(String regNo, String name) { this.regNo = regNo; this.name = name; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;
        return regNo.equals(((Student) o).regNo);
    }

    // TODO 1 (completed): hashCode must use exactly the fields used by equals()
    @Override public int hashCode() { return Objects.hash(regNo); }   // <-- comment this out for RUN 1
}

/** Mutable-key demonstration: hashCode depends on a field that can change. */
class MutableStudent {
    String regNo;
    MutableStudent(String regNo) { this.regNo = regNo; }
    @Override public boolean equals(Object o) {
        return o instanceof MutableStudent m && regNo.equals(m.regNo);
    }
    @Override public int hashCode() { return regNo.hashCode(); }
}

/** Same as Student but WITHOUT hashCode (the junior developer's version). */
class BrokenStudent {
    final String regNo;
    BrokenStudent(String regNo) { this.regNo = regNo; }
    @Override public boolean equals(Object o) {
        return o instanceof BrokenStudent b && regNo.equals(b.regNo);
    }
}

