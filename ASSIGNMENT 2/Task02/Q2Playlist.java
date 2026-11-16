import java.util.*;

/** Q2 - Music playlist: ArrayList vs LinkedList (and ArrayDeque for the "Up Next" queue). */
public class Q2Playlist {
    record Song(int id) {}
    static volatile long sink;

    static List<Song> fill(List<Song> l, int n) {
        for (int i = 0; i < n; i++) l.add(new Song(i));
        return l;
    }

    interface Op { void run(List<Song> l, int n, int ops); }

    // average microseconds PER OPERATION over 'reps' repetitions on a fresh list
    static double perOpMicros(String type, int n, int ops, int reps, Op op) {
        long total = 0;
        for (int r = 0; r < reps + 1; r++) {            // first repetition = warm-up
            List<Song> l = fill(type.equals("ArrayList") ? new ArrayList<>() : new LinkedList<>(), n);
            long t0 = System.nanoTime();
            op.run(l, n, ops);
            long dt = System.nanoTime() - t0;
            if (r > 0) total += dt;
        }
        return total / (double) reps / ops / 1000.0;
    }

    public static void main(String[] args) {
        int[] sizes = {1_000, 10_000, 100_000};
        Random rnd = new Random(42);
        Map<String, Op> tests = new LinkedHashMap<>();
        tests.put("add at end", (l, n, ops) -> { for (int i = 0; i < ops; i++) l.add(new Song(i)); });
        tests.put("insert at front", (l, n, ops) -> { for (int i = 0; i < ops; i++) l.add(0, new Song(i)); });
        tests.put("insert in middle", (l, n, ops) -> { for (int i = 0; i < ops; i++) l.add(l.size() / 2, new Song(i)); });
        tests.put("remove at front", (l, n, ops) -> { for (int i = 0; i < ops; i++) l.remove(0); });
        tests.put("remove in middle", (l, n, ops) -> { for (int i = 0; i < ops; i++) l.remove(l.size() / 2); });
        tests.put("get k-th (random)", (l, n, ops) -> {
            Random r = new Random(1);
            long s = 0; for (int i = 0; i < ops; i++) s += l.get(r.nextInt(n)).id(); sink += s; });
        tests.put("iterate whole list", (l, n, ops) -> {
            long s = 0; for (int i = 0; i < ops; i++) for (Song x : l) s += x.id(); sink += s; });

        System.out.println("Java " + System.getProperty("java.version"));
        System.out.println("Values = average microseconds per operation (iterate: per full pass)\n");
        System.out.printf("%-20s %-10s", "Operation", "List");
        for (int n : sizes) System.out.printf("%12s", "n=" + n);
        System.out.println();
        for (var e : tests.entrySet()) {
            for (String type : new String[]{"ArrayList", "LinkedList"}) {
                System.out.printf("%-20s %-10s", e.getKey(), type);
                for (int n : sizes) {
                    int ops = e.getKey().startsWith("iterate") ? 20 : Math.min(2000, n / 2);
                    if (e.getKey().startsWith("add at end")) ops = 2000;
                    if (e.getKey().startsWith("get") && type.equals("LinkedList") && n >= 100_000) ops = 500;
                    System.out.printf("%12.3f", perOpMicros(type, n, ops, 5, e.getValue()));
                }
                System.out.println();
            }
        }

        // ---- Changed condition: "Up Next" queue (add at both ends, remove from front) ----
        System.out.println("\n=== Up Next queue: add at both ends + remove from front (microseconds per op) ===");
        System.out.printf("%-14s%12s%12s%12s%n", "Structure", "n=1000", "n=10000", "n=100000");
        for (String type : new String[]{"ArrayList", "LinkedList", "ArrayDeque"}) {
            System.out.printf("%-14s", type);
            for (int n : sizes) {
                int ops = 5000; double total = 0; int reps = 5;
                for (int r = 0; r < reps + 1; r++) {
                    long dt;
                    if (type.equals("ArrayDeque")) {
                        ArrayDeque<Song> d = new ArrayDeque<>();
                        for (int i = 0; i < n; i++) d.addLast(new Song(i));
                        long t0 = System.nanoTime();
                        for (int i = 0; i < ops; i++) { d.addFirst(new Song(i)); d.addLast(new Song(i)); d.pollFirst(); }
                        dt = System.nanoTime() - t0;
                    } else {
                        List<Song> l = fill(type.equals("ArrayList") ? new ArrayList<>() : new LinkedList<>(), n);
                        long t0 = System.nanoTime();
                        for (int i = 0; i < ops; i++) { l.add(0, new Song(i)); l.add(new Song(i)); l.remove(0); }
                        dt = System.nanoTime() - t0;
                    }
                    if (r > 0) total += dt;
                }
                System.out.printf("%12.3f", total / reps / (ops * 3.0) / 1000.0);
            }
            System.out.println();
        }
    }
}
