// Name: V V Naveen Kumar | Register No: 192321018 | Course: Programming in Java (CSA09) | Assignment 3 (CO3)
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/** Q6 alternatives: ArrayBlockingQueue instead of wait/notify, tryLock(timeout) instead of lock ordering. */
public class AlternativeDesigns {

    static class Acct { final int id; int balance; final ReentrantLock lock = new ReentrantLock(); Acct(int i, int b) { id = i; balance = b; } }

    /** Returns true if done, false if it gave up (caller may retry). No deadlock: never waits forever. */
    static boolean transfer(Acct from, Acct to, int amt) throws InterruptedException {
        if (from.lock.tryLock(50, TimeUnit.MILLISECONDS)) {
            try {
                if (to.lock.tryLock(50, TimeUnit.MILLISECONDS)) {
                    try { from.balance -= amt; to.balance += amt; return true; }
                    finally { to.lock.unlock(); }
                }
            } finally { from.lock.unlock(); }
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        // Part B: producer-consumer with a blocking queue (capacity 5)
        BlockingQueue<Integer> q = new ArrayBlockingQueue<>(5);
        int[] got = new int[2];
        Thread cook = new Thread(() -> { try { for (int t = 1; t <= 20; t++) q.put(t); } catch (InterruptedException e) { } });
        Thread[] st = new Thread[2];
        for (int s = 0; s < 2; s++) { final int id = s; st[s] = new Thread(() -> { try { for (int k = 0; k < 10; k++) got[id] += q.take(); } catch (InterruptedException e) { } }); }
        cook.start(); for (Thread t : st) t.start();
        cook.join(); for (Thread t : st) t.join();
        System.out.println("ArrayBlockingQueue: sum of tokens = " + (got[0] + got[1]) + " (expected 210)");

        // Part A: the deadlock-prone pair, but with tryLock + retry
        Acct a = new Acct(1, 1000), b = new Acct(2, 1000);
        int[] retries = new int[2];
        Thread t1 = new Thread(() -> { try { for (int i = 0; i < 1000; i++) while (!transfer(a, b, 1)) retries[0]++; } catch (InterruptedException e) { } });
        Thread t2 = new Thread(() -> { try { for (int i = 0; i < 1000; i++) while (!transfer(b, a, 1)) retries[1]++; } catch (InterruptedException e) { } });
        t1.start(); t2.start(); t1.join(); t2.join();
        System.out.println("tryLock transfers: total = " + (a.balance + b.balance) + " (expected 2000), retries = " + (retries[0] + retries[1]));
    }
}
