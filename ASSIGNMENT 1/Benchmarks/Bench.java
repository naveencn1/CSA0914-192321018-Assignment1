import java.util.*;

/** Measurements for the "Alternative Concept and Complexity Analysis" sections. */
public class Bench {
    record Ticket(String reg, int floor, int slot) {}

    static long time(Runnable r) { long t = System.nanoTime(); r.run(); return System.nanoTime() - t; }

    public static void main(String[] a) {
        System.out.println("n,array_scan_us,arraylist_scan_us,hashmap_us   (find LAST vehicle by regNo, 2000 lookups avg)");
        for (int n : new int[]{1_000, 10_000, 100_000}) {
            Ticket[] arr = new Ticket[n]; List<Ticket> list = new ArrayList<>(); Map<String, Ticket> map = new HashMap<>();
            for (int i = 0; i < n; i++) { Ticket t = new Ticket("TN" + i, i / 50, i % 50); arr[i] = t; list.add(t); map.put(t.reg(), t); }
            String key = "TN" + (n - 1);
            int reps = 2000; final int[] sink = {0};
            // warm-up
            for (int w = 0; w < 300; w++) { for (Ticket t : arr) if (t.reg().equals(key)) { sink[0]++; break; } }
            long ta = time(() -> { for (int r = 0; r < reps; r++) for (Ticket t : arr) if (t.reg().equals(key)) { sink[0]++; break; } });
            long tl = time(() -> { for (int r = 0; r < reps; r++) for (int i = 0; i < list.size(); i++) if (list.get(i).reg().equals(key)) { sink[0]++; break; } });
            long th = time(() -> { for (int r = 0; r < reps; r++) if (map.get(key) != null) sink[0]++; });
            System.out.printf("%d,%.2f,%.2f,%.3f%n", n, ta / 1000.0 / reps, tl / 1000.0 / reps, th / 1000.0 / reps);
        }

        System.out.println("\nfloors x slots, scan_allocate_us (building is full except last slot), stack_allocate_us");
        int[][] cfg = {{1, 20}, {3, 50}, {5, 100}, {5, 2000}};
        for (int[] c : cfg) {
            int F = c[0], S = c[1];
            String[][] slots = new String[F][S];
            for (String[] row : slots) Arrays.fill(row, "X");
            slots[F - 1][S - 1] = null;
            int reps = 20_000; final int[] sink = {0};
            long ts = time(() -> { for (int r = 0; r < reps; r++) {
                outer: for (int f = 0; f < F; f++) for (int s = 0; s < S; s++)
                    if (slots[f][s] == null) { sink[0]++; break outer; } } });
            ArrayDeque<Integer> free = new ArrayDeque<>(); free.push(F * S - 1);
            long tk = time(() -> { for (int r = 0; r < reps; r++) { int x = free.pop(); sink[0] += x & 1; free.push(x); } });
            System.out.printf("%dx%d,%.3f,%.3f%n", F, S, ts / 1000.0 / reps, tk / 1000.0 / reps);
        }

        System.out.println("\nObject-per-vehicle vs parallel String arrays: memory estimate for 1,000,000 vehicles");
        Runtime rt = Runtime.getRuntime(); System.gc();
        long m0 = rt.totalMemory() - rt.freeMemory();
        Ticket[] objs = new Ticket[1_000_000];
        for (int i = 0; i < objs.length; i++) objs[i] = new Ticket("TN" + i, i, i);
        System.gc(); long m1 = rt.totalMemory() - rt.freeMemory();
        System.out.println("objects approx bytes/vehicle: " + (m1 - m0) / objs.length);
        objs = null; System.gc(); long m2 = rt.totalMemory() - rt.freeMemory();
        String[] reg = new String[1_000_000]; String[] type = new String[1_000_000]; String[] ent = new String[1_000_000];
        for (int i = 0; i < reg.length; i++) { reg[i] = "TN" + i; type[i] = "CAR"; ent[i] = "08:00"; }
        System.gc(); long m3 = rt.totalMemory() - rt.freeMemory();
        System.out.println("parallel String[] approx bytes/vehicle: " + (m3 - m2) / reg.length);
        if (sinkDummy(objs, reg, type, ent)) System.out.println();
    }
    static boolean sinkDummy(Object... o) { return o.length == 0; }
}
