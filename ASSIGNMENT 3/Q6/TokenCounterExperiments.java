// Name: V V Naveen Kumar | Register No: 192321018 | Course: Programming in Java (CSA09) | Assignment 3 (CO3)
import java.util.*;

/** Q6 Student A: (1) record counter size over time; (2) while -> if experiment; (3) notify() vs notifyAll(). */
public class TokenCounterExperiments {

    static class Counter {
        final int[] buffer = new int[5]; int count = 0, in = 0, out = 0;
        final boolean useWhile, useNotifyAll;
        int maxSeen = 0, minSeen = 0; final List<Integer> trace = new ArrayList<>();
        Counter(boolean w, boolean na) { useWhile = w; useNotifyAll = na; }
        void wake() { if (useNotifyAll) notifyAll(); else notify(); }

        synchronized void put(int token) throws InterruptedException {
            if (useWhile) { while (count == buffer.length) wait(); } else { if (count == buffer.length) wait(); }
            buffer[in] = token; in = (in + 1) % buffer.length; count++;
            maxSeen = Math.max(maxSeen, count); trace.add(count); wake();
        }
        synchronized int take() throws InterruptedException {
            if (useWhile) { while (count == 0) wait(); } else { if (count == 0) wait(); }
            int token = buffer[out]; out = (out + 1) % buffer.length; count--;
            minSeen = Math.min(minSeen, count); trace.add(count); wake();
            return token;
        }
    }

    /** returns "OK", "WRONG(sum=..)", or "HANG" */
    static String trial(boolean useWhile, boolean notifyAll, int consumers, Counter[] holder) throws Exception {
        Counter c = new Counter(useWhile, notifyAll); holder[0] = c;
        int perConsumer = 20 / consumers;
        int[] got = new int[consumers];
        Thread cook = new Thread(() -> { try { for (int t = 1; t <= 20; t++) c.put(t); } catch (InterruptedException e) { } });
        Thread[] st = new Thread[consumers];
        for (int s = 0; s < consumers; s++) {
            final int id = s;
            st[s] = new Thread(() -> { try { for (int k = 0; k < perConsumer; k++) got[id] += c.take(); } catch (InterruptedException e) { } });
        }
        cook.setDaemon(true); for (Thread t : st) t.setDaemon(true);
        cook.start(); for (Thread t : st) t.start();
        cook.join(1000); for (Thread t : st) t.join(1000);
        boolean hung = cook.isAlive(); for (Thread t : st) hung |= t.isAlive();
        if (hung) { cook.interrupt(); for (Thread t : st) t.interrupt(); return "HANG"; }
        int sum = Arrays.stream(got).sum();
        return (sum == 210 && c.minSeen >= 0 && c.maxSeen <= 5) ? "OK" : "WRONG(sum=" + sum + ", min=" + c.minSeen + ", max=" + c.maxSeen + ")";
    }

    static void campaign(String label, boolean useWhile, boolean notifyAll, int consumers, int trials) throws Exception {
        Map<String, Integer> tally = new TreeMap<>(); Counter[] h = new Counter[1];
        for (int i = 0; i < trials; i++) tally.merge(trial(useWhile, notifyAll, consumers, h).replaceAll("\\(.*", ""), 1, Integer::sum);
        System.out.printf("  %-40s %s%n", label, tally);
    }

    public static void main(String[] args) throws Exception {
        Counter[] h = new Counter[1];
        System.out.println("=== 1. Counter size over time (while + notifyAll, 2 consumers) ===");
        System.out.println("  result: " + trial(true, true, 2, h));
        System.out.println("  size after each put/take: " + h[0].trace);
        System.out.println("  max size = " + h[0].maxSeen + " (limit 5), min size = " + h[0].minSeen + " (limit 0)");

        System.out.println("\n=== 2. Change while -> if, 300 trials each (2 consumers) ===");
        campaign("while + notifyAll", true, true, 2, 300);
        campaign("if    + notifyAll", false, true, 2, 300);

        System.out.println("\n=== 3. notify() instead of notifyAll(), 4 consumers, 100 trials ===");
        campaign("while + notifyAll (4 consumers)", true, true, 4, 100);
        campaign("while + notify    (4 consumers)", true, false, 4, 100);
    }
}
