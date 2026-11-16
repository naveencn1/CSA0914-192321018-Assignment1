import java.util.*;

/** Q4 - Choosing a Map for a pharmacy inventory (lookup by code, report in code order, list in insertion order). */
public class Q4Pharmacy {
    static volatile long sink;

    record Medicine(int code, int stock) {}

    static int[] shuffledCodes(int n) {
        List<Integer> l = new ArrayList<>();
        for (int i = 0; i < n; i++) l.add(i * 3 + 1000);      // codes are not 0..n-1
        Collections.shuffle(l, new Random(7));
        return l.stream().mapToInt(Integer::intValue).toArray();
    }

    static void fill(Map<Integer, Medicine> m, int[] codes) {
        for (int c : codes) m.put(c, new Medicine(c, c % 50));
    }

    static boolean isAscending(Collection<Integer> keys) {
        int prev = Integer.MIN_VALUE;
        for (int k : keys) { if (k < prev) return false; prev = k; }
        return true;
    }
    static boolean sameOrder(Collection<Integer> keys, int[] inserted) {
        int i = 0;
        for (int k : keys) if (k != inserted[i++]) return false;
        return true;
    }

    public static void main(String[] args) {
        System.out.println("Java " + System.getProperty("java.version"));
        int[] sizes = {1_000, 10_000, 100_000, 500_000};
        final int LOOKUPS = 100_000;
        String[] names = {"HashMap", "LinkedHashMap", "TreeMap", "Hashtable", "SortedArray+binarySearch"};

        System.out.printf("%-26s %8s %16s %16s %14s %14s%n",
            "Structure", "n", "100k lookups ms", "ns / lookup", "code-order ms", "insert-order ms");
        for (int n : sizes) {
            int[] codes = shuffledCodes(n);
            Random r = new Random(3);
            int[] queries = new int[LOOKUPS];
            for (int i = 0; i < LOOKUPS; i++) queries[i] = codes[r.nextInt(n)];

            for (String name : names) {
                double lookupMs = 0, codeMs = 0, insMs = 0; boolean orderCode = false, orderIns = false;
                int reps = 5;
                for (int rep = 0; rep < reps + 2; rep++) {           // 2 warm-up repetitions
                    long t0, t1;
                    if (name.startsWith("Sorted")) {
                        int[] arr = codes.clone(); Arrays.sort(arr);
                        Medicine[] med = new Medicine[n];
                        for (int i = 0; i < n; i++) med[i] = new Medicine(arr[i], arr[i] % 50);
                        t0 = System.nanoTime();
                        long s = 0;
                        for (int q : queries) s += med[Arrays.binarySearch(arr, q)].stock();
                        sink += s; t1 = System.nanoTime();
                        double l = (t1 - t0) / 1e6;
                        t0 = System.nanoTime();
                        StringBuilder sb = new StringBuilder();
                        for (Medicine m : med) sb.append(m.code()).append(' ');
                        sink += sb.length(); t1 = System.nanoTime();
                        double c = (t1 - t0) / 1e6;
                        // insertion-order list is NOT available in a sorted array
                        if (rep >= 2) { lookupMs += l; codeMs += c; }
                        orderCode = true; orderIns = false;
                        continue;
                    }
                    Map<Integer, Medicine> m = switch (name) {
                        case "HashMap" -> new HashMap<>();
                        case "LinkedHashMap" -> new LinkedHashMap<>();
                        case "TreeMap" -> new TreeMap<>();
                        default -> new Hashtable<>();
                    };
                    fill(m, codes);
                    t0 = System.nanoTime();
                    long s = 0;
                    for (int q : queries) s += m.get(q).stock();
                    sink += s; t1 = System.nanoTime();
                    double l = (t1 - t0) / 1e6;

                    // Report 1: all medicines in code order
                    t0 = System.nanoTime();
                    List<Integer> codeOrder;
                    if (m instanceof TreeMap) codeOrder = new ArrayList<>(m.keySet());
                    else { codeOrder = new ArrayList<>(m.keySet()); Collections.sort(codeOrder); }
                    sink += codeOrder.size(); t1 = System.nanoTime();
                    double c = (t1 - t0) / 1e6;
                    // Report 2: medicines in the order they were added
                    t0 = System.nanoTime();
                    List<Integer> insOrder = new ArrayList<>(m.keySet());
                    sink += insOrder.size(); t1 = System.nanoTime();
                    double ins = (t1 - t0) / 1e6;

                    if (rep >= 2) { lookupMs += l; codeMs += c; insMs += ins; }
                    orderCode = isAscending(codeOrder);           // after sorting where needed
                    orderIns = sameOrder(insOrder, codes);        // does map's own iteration give insertion order?
                }
                System.out.printf("%-26s %8d %16.2f %16.1f %14.2f %14.2f   codeOrderOK=%b insertionOrderOK=%b%n",
                    name, n, lookupMs / reps, lookupMs / reps * 1e6 / LOOKUPS, codeMs / reps,
                    insMs / reps, orderCode, orderIns);
            }
            System.out.println();
        }

        // order check using the maps' OWN iteration order (no extra sort)
        int[] codes = shuffledCodes(10);
        System.out.println("Own iteration order of keys (10 medicines, inserted in shuffled order):");
        System.out.println("  inserted      : " + Arrays.toString(codes));
        for (String name : new String[]{"HashMap", "LinkedHashMap", "TreeMap"}) {
            Map<Integer, Medicine> m = name.equals("HashMap") ? new HashMap<>()
                : name.equals("LinkedHashMap") ? new LinkedHashMap<>() : new TreeMap<>();
            fill(m, codes);
            System.out.printf("  %-14s: %s%n", name, m.keySet());
        }
    }
}
