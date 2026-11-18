// Name: V V Naveen Kumar | Register No: 192321018 | Course: Programming in Java (CSA09) | Assignment 3 (CO3)
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;

/** Q5 - Race experiments and a comparison of five booking strategies. */
public class BookingExperiment {

    interface Screen { boolean book(int seat); int sold(); }

    /** Counts how many successful bookings each seat received (should be 1 at most). */
    abstract static class Base implements Screen {
        final int[] salesPerSeat; Base(int s) { salesPerSeat = new int[s]; }
        public int sold() { int c = 0; for (int x : salesPerSeat) c += x; return c; }
        /** Work that does NOT need the lock: pricing, formatting a ticket, etc. (simulated by a small loop). */
        static long nonCritical(int seat) { long h = seat; for (int i = 0; i < 3000; i++) h = h * 31 + i; return h; }
    }

    static class Unsafe extends Base {
        final boolean[] booked; final boolean widenWindow;
        Unsafe(int s, boolean widen) { super(s); booked = new boolean[s]; widenWindow = widen; }
        public boolean book(int seat) {
            nonCritical(seat);
            if (!booked[seat]) {                 // CHECK
                if (widenWindow) Thread.yield(); // gives another thread time to slip in between check and act
                booked[seat] = true;             // ACT
                salesPerSeat[seat]++;            // (not atomic either, but fine for counting duplicates here)
                return true;
            }
            return false;
        }
    }
    static class SyncMethod extends Base {
        final boolean[] booked; SyncMethod(int s) { super(s); booked = new boolean[s]; }
        public synchronized boolean book(int seat) {            // lock = this, held for the WHOLE method
            nonCritical(seat);
            if (!booked[seat]) { booked[seat] = true; salesPerSeat[seat]++; return true; }
            return false;
        }
    }
    static class SyncBlock extends Base {
        final boolean[] booked; SyncBlock(int s) { super(s); booked = new boolean[s]; }
        public boolean book(int seat) {
            nonCritical(seat);                                   // outside the lock
            synchronized (this) {                                // lock held only for the critical section
                if (!booked[seat]) { booked[seat] = true; salesPerSeat[seat]++; return true; }
            }
            return false;
        }
    }
    static class AtomicSeats extends Base {
        final AtomicBoolean[] booked;
        AtomicSeats(int s) { super(s); booked = new AtomicBoolean[s]; for (int i = 0; i < s; i++) booked[i] = new AtomicBoolean(); }
        public boolean book(int seat) {
            nonCritical(seat);
            if (booked[seat].compareAndSet(false, true)) { synchronized (salesPerSeat) { salesPerSeat[seat]++; } return true; }
            return false;
        }
    }
    static class LockSeats extends Base {
        final boolean[] booked; final ReentrantLock lock = new ReentrantLock();
        LockSeats(int s) { super(s); booked = new boolean[s]; }
        public boolean book(int seat) {
            nonCritical(seat);
            lock.lock();
            try { if (!booked[seat]) { booked[seat] = true; salesPerSeat[seat]++; return true; } return false; }
            finally { lock.unlock(); }
        }
    }

    /** Runs `attempts` booking attempts over `threads` threads; returns elapsed ms. */
    static long run(Screen s, int seats, int attempts, int threads) throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        Future<?>[] fs = new Future<?>[threads];
        for (int t = 0; t < threads; t++) {
            final int off = t;
            fs[t] = ex.submit(() -> {
                try { go.await(); } catch (InterruptedException e) { return; }
                for (int i = off; i < attempts; i += threads) s.book(i % seats);
            });
        }
        long t0 = System.nanoTime(); go.countDown();
        for (Future<?> f : fs) f.get();
        long ms = (System.nanoTime() - t0) / 1_000;   // microseconds
        ex.shutdown();
        return ms;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== A. Reproduce double booking (unsynchronised, 10 runs, 10 seats, 100 users) ===");
        int totalDouble = 0;
        for (int r = 1; r <= 10; r++) {
            Unsafe u = new Unsafe(10, true);
            run(u, 10, 100, 8);
            int sold = u.sold();
            totalDouble += Math.max(0, sold - 10);
            System.out.printf("  run %2d: seats sold = %3d  (expected 10)  %s%n", r, sold, sold > 10 ? "<-- DOUBLE BOOKING" : "");
        }
        System.out.println("  total extra (double) bookings over 10 runs: " + totalDouble);

        System.out.println("\n=== B. Synchronised versions never double-book (10 runs each) ===");
        for (String name : new String[]{"SyncMethod", "SyncBlock", "AtomicBoolean", "ReentrantLock"}) {
            int worst = 0;
            for (int r = 0; r < 10; r++) {
                Screen s = switch (name) {
                    case "SyncMethod" -> new SyncMethod(10);
                    case "SyncBlock" -> new SyncBlock(10);
                    case "AtomicBoolean" -> new AtomicSeats(10);
                    default -> new LockSeats(10);
                };
                run(s, 10, 100, 8);
                worst = Math.max(worst, s.sold());
            }
            System.out.printf("  %-14s max seats sold in 10 runs = %d (expected 10)%n", name, worst);
        }

        System.out.println("\n=== C. Time for 10,000 booking attempts, 1,000 seats, 8 threads (median of 7 runs, microseconds) ===");
        int seats = 1000, attempts = 10_000, threads = 8;
        String[] names = {"Unsafe (wrong!)", "SyncMethod", "SyncBlock", "AtomicBoolean", "ReentrantLock"};
        for (String name : names) {
            long[] ms = new long[7];
            for (int r = 0; r < 7 + 3; r++) {          // 3 warm-up runs, 7 measured
                Screen s = switch (name) {
                    case "Unsafe (wrong!)" -> new Unsafe(seats, false);
                    case "SyncMethod" -> new SyncMethod(seats);
                    case "SyncBlock" -> new SyncBlock(seats);
                    case "AtomicBoolean" -> new AtomicSeats(seats);
                    default -> new LockSeats(seats);
                };
                long t = run(s, seats, attempts, threads);
                if (r >= 3) ms[r - 3] = t;
            }
            java.util.Arrays.sort(ms);
            System.out.printf("  %-16s %6d us  (%.2f ms)%n", name, ms[3], ms[3] / 1000.0);
        }
    }
}
