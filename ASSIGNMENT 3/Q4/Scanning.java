// Name: V V Naveen Kumar | Register No: 192321018 | Course: Programming in Java (CSA09) | Assignment 3 (CO3)
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Q4 - Four scanner stations: Thread subclass, Runnable, run() vs start(), priorities, daemon threads. */
public class Scanning {
    static final AtomicInteger total = new AtomicInteger();
    static final List<String> finishOrder = Collections.synchronizedList(new ArrayList<>());

    static void scan(String name, int scripts, int msPerScript) {
        for (int i = 0; i < scripts; i++) {
            try { Thread.sleep(msPerScript, ThreadLocalRandom.current().nextInt(0, 900_000)); }   // small random jitter like real scanners
             catch (InterruptedException e) { return; }   // TIMED_WAITING
            total.incrementAndGet();
        }
        finishOrder.add(name);
        System.out.println("  " + name + " finished: " + scripts + " scripts (thread " + Thread.currentThread().getName() + ")");
    }

    /** Way 1: extend Thread. */
    static class StationThread extends Thread {
        final int scripts, ms;
        StationThread(String name, int scripts, int ms) { super(name); this.scripts = scripts; this.ms = ms; }
        @Override public void run() { scan(getName(), scripts, ms); }
    }

    /** Way 2: implement Runnable (class stays free to extend something else; task separate from thread). */
    static class StationTask implements Runnable {
        final String name; final int scripts, ms;
        StationTask(String name, int scripts, int ms) { this.name = name; this.scripts = scripts; this.ms = ms; }
        @Override public void run() { scan(name, scripts, ms); }
    }

    static String runOnce(boolean verbose) throws InterruptedException {
        total.set(0); finishOrder.clear();
        Thread[] st = {
            new StationThread("Station-1", 20, 5),                                  // by extending Thread
            new StationThread("Station-2", 18, 5),
            new Thread(new StationTask("Station-3", 20, 5), "Station-3"),           // by implementing Runnable
            new Thread(new StationTask("Station-4", 19, 5), "Station-4"),
        };
        if (verbose) for (Thread t : st) System.out.println("  " + t.getName() + " state before start = " + t.getState());   // NEW
        for (Thread t : st) t.start();                                              // -> RUNNABLE
        if (verbose) System.out.println("  isAlive right after start: " + st[0].isAlive() + ", state = " + st[0].getState());
        Thread.sleep(20);
        if (verbose) System.out.println("  state while scanning = " + st[0].getState());                 // TIMED_WAITING
        for (Thread t : st) t.join();                                               // main WAITS here
        if (verbose) System.out.println("  isAlive after join: " + st[0].isAlive() + ", state = " + st[0].getState()); // TERMINATED
        System.out.println("  TOTAL scanned = " + total.get() + " (expected 77)");   // safe only because of join()
        return String.join(" > ", finishOrder);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 1. Normal run with life-cycle states ===");
        System.out.println("  order: " + runOnce(true));

        System.out.println("\n=== 2. run() instead of start(): no new thread ===");
        Thread t = new Thread(() -> System.out.println("  running in thread: " + Thread.currentThread().getName()), "Worker");
        t.run();     // executes in main!
        t.start();   // executes in Worker
        t.join();

        System.out.println("\n=== 3. Ten runs - order of finishing (Student B) ===");
        PrintStreamMute.mute();
        List<String> orders = new ArrayList<>();
        for (int i = 1; i <= 10; i++) orders.add(runOnce(false));
        PrintStreamMute.unmute();
        for (int i = 0; i < orders.size(); i++) System.out.printf("  run %2d: %s%n", i + 1, orders.get(i));
        System.out.println("  distinct orders: " + new HashSet<>(orders).size());

        System.out.println("\n=== 4. Priorities (hint only - the scheduler may ignore it) ===");
        PrintStreamMute.mute();
        List<String> pr = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            total.set(0); finishOrder.clear();
            Thread a = new StationThread("Station-1", 10, 5), b = new StationThread("Station-2", 10, 5);
            a.setPriority(Thread.MIN_PRIORITY); b.setPriority(Thread.MAX_PRIORITY);
            a.start(); b.start(); a.join(); b.join();
            pr.add(String.join(" > ", finishOrder));
        }
        PrintStreamMute.unmute();
        System.out.println("  Station-1 priority 1 vs Station-2 priority 10, same work, 5 runs: " + pr);

        System.out.println("\n=== 5. Daemon thread: JVM does not wait for it ===");
        Thread d = new Thread(() -> {
            try { Thread.sleep(300); } catch (InterruptedException e) { }
            System.out.println("  daemon station finished (you will NOT see this line)");
        }, "Daemon-Station");
        d.setDaemon(true);
        d.start();
        System.out.println("  main() ends now; daemon still alive = " + d.isAlive());
    }

    /** Silences station messages when we only want the order. */
    static class PrintStreamMute {
        static final java.io.PrintStream real = System.out;
        static void mute()   { System.setOut(new java.io.PrintStream(java.io.OutputStream.nullOutputStream())); }
        static void unmute() { System.setOut(real); }
    }
}
