import java.util.*;

public class Triage {
    private long counter = 0;

    // TODO 1 (completed): lowest severity number first, then earliest arrival first
    private final PriorityQueue<Patient> queue = new PriorityQueue<>(
        Comparator.comparingInt((Patient p) -> p.severity)
                  .thenComparingLong(p -> p.arrival));

    void admit(String name, int severity) {
        queue.add(new Patient(name, severity, counter++));
    }

    Patient next() { return queue.poll(); }

    public static void main(String[] args) {
        // ---------- Hand-verifiable instance (8 patients) ----------
        Triage t = new Triage();
        t.admit("Anil", 3);   // arrival 0
        t.admit("Bina", 1);   // 1
        t.admit("Chitra", 5); // 2
        t.admit("Dev", 1);    // 3
        t.admit("Esha", 3);   // 4
        t.admit("Farid", 2);  // 5
        t.admit("Gita", 5);   // 6
        t.admit("Hari", 1);   // 7
        System.out.println("Printing the PriorityQueue directly (heap order, NOT treatment order): " + t.queue);
        System.out.println("Expected by hand : Bina(S1) Dev(S1) Hari(S1) Farid(S2) Anil(S3) Esha(S3) Chitra(S5) Gita(S5)");
        StringBuilder sb = new StringBuilder("Program output   :");
        Patient p;
        while ((p = t.next()) != null) sb.append(' ').append(p);
        System.out.println(sb);

        // ---------- Growth investigation ----------
        System.out.println("\nGrowth: average ns per operation (add / poll), random severities");
        System.out.printf("%-34s %10s %10s %10s%n", "Design", "n=1,000", "n=10,000", "n=100,000");
        String[] designs = {"PriorityQueue (binary heap)", "Sorted LinkedList", "5 FIFO queues (ArrayDeque[])"};
        int[] sizes = {1_000, 10_000, 100_000};
        for (int d = 0; d < designs.length; d++) {
            StringBuilder add = new StringBuilder(String.format("%-34s", designs[d] + " add"));
            StringBuilder pol = new StringBuilder(String.format("%-34s", designs[d] + " poll"));
            for (int n : sizes) {
                if (d == 1 && n > 10_000) { add.append(String.format("%10s", "skipped")); pol.append(String.format("%10s", "skipped")); continue; }
                double[] r = bench(d, n);
                add.append(String.format("%10.0f", r[0])); pol.append(String.format("%10.0f", r[1]));
            }
            System.out.println(add); System.out.println(pol);
        }

        // ---------- Starvation and aging ----------
        System.out.println("\nStarvation demo: 1 minor patient (S5) then every tick 2 new critical patients arrive, 1 is served per tick");
        System.out.println("  strict priority : minor patient served at tick = " + starvation(false));
        System.out.println("  with aging      : minor patient served at tick = " + starvation(true));

        // ---------- Deployment: 5,000 patients per day ----------
        long t0 = System.nanoTime();
        Triage big = new Triage();
        Random r = new Random(1);
        for (int i = 0; i < 5000; i++) big.admit("P" + i, 1 + r.nextInt(5));
        int served = 0; while (big.next() != null) served++;
        System.out.printf("%nA whole day (5,000 admits + 5,000 serves) took %.2f ms (served=%d)%n", (System.nanoTime() - t0) / 1e6, served);
    }

    // ---------- benchmark helpers ----------
    static volatile Object sink;
    /** returns {ns per add, ns per poll} */
    static double[] bench(int design, int n) {
        int reps = 5; double add = 0, poll = 0;
        for (int rep = 0; rep < reps + 2; rep++) {
            Random r = new Random(9);
            Patient[] ps = new Patient[n];
            for (int i = 0; i < n; i++) ps[i] = new Patient("P", 1 + r.nextInt(5), i);
            Comparator<Patient> cmp = Comparator.comparingInt((Patient p) -> p.severity).thenComparingLong(p -> p.arrival);
            Object store = switch (design) {
                case 0 -> new PriorityQueue<Patient>(cmp);
                case 1 -> new LinkedList<Patient>();
                default -> { ArrayDeque<Patient>[] q = new ArrayDeque[6]; for (int i = 1; i <= 5; i++) q[i] = new ArrayDeque<>(); yield q; }
            };
            long t0 = System.nanoTime();
            for (Patient p : ps) addTo(design, store, p, cmp);
            long t1 = System.nanoTime();
            Patient last = null;
            for (int i = 0; i < n; i++) last = pollFrom(design, store);
            long t2 = System.nanoTime();
            sink = last;
            if (rep >= 2) { add += (t1 - t0) / (double) n; poll += (t2 - t1) / (double) n; }
        }
        return new double[]{add / reps, poll / reps};
    }
    @SuppressWarnings("unchecked")
    static void addTo(int d, Object s, Patient p, Comparator<Patient> cmp) {
        if (d == 0) ((PriorityQueue<Patient>) s).add(p);
        else if (d == 1) {                      // keep the list sorted: scan for insertion point
            LinkedList<Patient> l = (LinkedList<Patient>) s;
            ListIterator<Patient> it = l.listIterator();
            while (it.hasNext()) { if (cmp.compare(it.next(), p) > 0) { it.previous(); break; } }
            it.add(p);
        } else ((ArrayDeque<Patient>[]) s)[p.severity].addLast(p);
    }
    @SuppressWarnings("unchecked")
    static Patient pollFrom(int d, Object s) {
        if (d == 0) return ((PriorityQueue<Patient>) s).poll();
        if (d == 1) return ((LinkedList<Patient>) s).pollFirst();
        for (ArrayDeque<Patient> q : ((ArrayDeque<Patient>[]) s)) if (q != null && !q.isEmpty()) return q.pollFirst();
        return null;
    }

    /**
     * Aging rule implemented as a STATIC key: key = arrivalTick + WAIT_PER_LEVEL * (severity-1).
     * A severity-5 patient is overtaken only by patients arriving within 4*WAIT_PER_LEVEL ticks
     * before it is served, so its wait is bounded - and the heap still works in O(log n).
     */
    static int starvation(boolean aging) {
        final int W = 10;
        PriorityQueue<Patient> pq = new PriorityQueue<>(aging
            ? Comparator.comparingLong((Patient p) -> p.arrival + (long) W * (p.severity - 1)).thenComparingLong(p -> p.arrival)
            : Comparator.comparingInt((Patient p) -> p.severity).thenComparingLong(p -> p.arrival));
        long clock = 0;
        pq.add(new Patient("Minor", 5, clock++));
        for (int tick = 1; tick <= 1000; tick++) {
            pq.add(new Patient("C" + tick + "a", 1, clock++));
            pq.add(new Patient("C" + tick + "b", 1, clock++));
            Patient served = pq.poll();
            if (served.name.equals("Minor")) return tick;
        }
        return -1; // never served within 1000 ticks
    }
}

class Patient {
    final String name; final int severity; final long arrival;
    Patient(String name, int severity, long arrival) {
        this.name = name; this.severity = severity; this.arrival = arrival;
    }
    @Override public String toString() { return name + "(S" + severity + ")"; }
}

