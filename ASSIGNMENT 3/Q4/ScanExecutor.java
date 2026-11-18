// Name: V V Naveen Kumar | Register No: 192321018 | Course: Programming in Java (CSA09) | Assignment 3 (CO3)
import java.util.*;
import java.util.concurrent.*;

/** Q4 alternative: the same stations with a fixed thread pool, and a timing comparison for n scripts / p threads. */
public class ScanExecutor {
    static void scanOne() { try { Thread.sleep(2); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Future<Integer>> fs = new ArrayList<>();
        int[] scripts = {20, 18, 22, 16};
        for (int s = 0; s < 4; s++) {
            final int id = s;
            fs.add(pool.submit(() -> {                       // Callable<Integer> returns the count - no shared counter needed
                for (int i = 0; i < scripts[id]; i++) scanOne();
                return scripts[id];
            }));
        }
        int total = 0;
        for (Future<Integer> f : fs) total += f.get();       // get() blocks like join()
        pool.shutdown();
        System.out.println("Total (executor) = " + total + " (expected 76)");

        int n = 400;
        System.out.println("\nn = " + n + " scripts, 2 ms each. Time with p pool threads (expect about n/p * 2 ms):");
        for (int p : new int[]{1, 2, 4, 8}) {
            ExecutorService ex = Executors.newFixedThreadPool(p);
            long t0 = System.nanoTime();
            List<Future<?>> fl = new ArrayList<>();
            for (int i = 0; i < n; i++) fl.add(ex.submit(ScanExecutor::scanOne));
            for (Future<?> f : fl) f.get();
            long ms = (System.nanoTime() - t0) / 1_000_000;
            ex.shutdown();
            System.out.printf("  p=%d  %4d ms   (ideal %d ms)%n", p, ms, n / p * 2);
        }
    }
}
