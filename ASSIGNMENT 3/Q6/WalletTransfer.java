// Name: V V Naveen Kumar | Register No: 192321018 | Course: Programming in Java (CSA09) | Assignment 3 (CO3)
import java.lang.management.*;
import java.util.*;

/** Q6 Part A - hostel wallet transfers: deadlock-prone version and lock-ordering version. */
public class WalletTransfer {
    static class Account {
        final int id; int balance;
        Account(int id, int balance) { this.id = id; this.balance = balance; }
    }

    /** Deadlock-prone: locks FROM then TO. A->B and B->A can each hold one lock and wait for the other. */
    static void transferDeadlockProne(Account from, Account to, int amt, long pauseMs) {
        synchronized (from) {
            pause(pauseMs);                       // widen the window so the deadlock is reliable
            synchronized (to) { from.balance -= amt; to.balance += amt; }
        }
    }

    /** Safe: always lock the account with the SMALLER id first - a global ordering. */
    static void transferOrdered(Account from, Account to, int amt, long pauseMs) {
        Account first = from.id < to.id ? from : to, second = from.id < to.id ? to : from;
        synchronized (first) {
            pause(pauseMs);
            synchronized (second) { from.balance -= amt; to.balance += amt; }
        }
    }

    static void pause(long ms) { if (ms > 0) try { Thread.sleep(ms); } catch (InterruptedException e) { } }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 1. Reliable deadlock: T1 transfers A->B while T2 transfers B->A ===");
        Account a = new Account(1, 1000), b = new Account(2, 1000);
        Thread t1 = new Thread(() -> transferDeadlockProne(a, b, 100, 100), "Transfer-A-to-B");
        Thread t2 = new Thread(() -> transferDeadlockProne(b, a, 50, 100), "Transfer-B-to-A");
        t1.setDaemon(true); t2.setDaemon(true);
        t1.start(); t2.start();
        t1.join(1000); t2.join(1000);
        System.out.println("  after 1 s: T1 state=" + t1.getState() + ", T2 state=" + t2.getState());
        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        long[] dead = mx.findDeadlockedThreads();
        System.out.println("  JVM reports deadlocked threads: " + (dead == null ? "none" : dead.length));
        if (dead != null) for (ThreadInfo ti : mx.getThreadInfo(dead))
            System.out.println("    " + ti.getThreadName() + " is BLOCKED waiting for lock held by " + ti.getLockOwnerName());
        System.out.println("  balances unchanged: A=" + a.balance + " B=" + b.balance);

        System.out.println("\n=== 2. Same two transfers with lock ordering ===");
        Account c = new Account(1, 1000), d = new Account(2, 1000);
        Thread u1 = new Thread(() -> transferOrdered(c, d, 100, 100));
        Thread u2 = new Thread(() -> transferOrdered(d, c, 50, 100));
        u1.start(); u2.start(); u1.join(2000); u2.join(2000);
        System.out.println("  finished: " + (!u1.isAlive() && !u2.isAlive()) + ", C=" + c.balance + " D=" + d.balance + " (expected 950 / 1050)");

        System.out.println("\n=== 3. Stress: 8 threads x 5000 random transfers among 5 accounts (ordered) ===");
        Account[] acc = new Account[5];
        for (int i = 0; i < 5; i++) acc[i] = new Account(i + 1, 1000);
        Thread[] ts = new Thread[8];
        for (int i = 0; i < ts.length; i++) {
            ts[i] = new Thread(() -> {
                Random r = new Random();
                for (int k = 0; k < 5000; k++) {
                    int x = r.nextInt(5), y = r.nextInt(5);
                    if (x != y) transferOrdered(acc[x], acc[y], 1 + r.nextInt(10), 0);
                }
            });
            ts[i].start();
        }
        for (Thread t : ts) t.join();
        int sum = 0; for (Account x : acc) sum += x.balance;
        System.out.println("  all threads finished, total money = " + sum + " (expected 5000)");
    }
}
