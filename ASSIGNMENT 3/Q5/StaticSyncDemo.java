// Name: V V Naveen Kumar | Register No: 192321018 | Course: Programming in Java (CSA09) | Assignment 3 (CO3)
/** Q5 - Static synchronisation: a static counter shared by ALL screens needs a class-level lock. */
public class StaticSyncDemo {
    static int totalTicketsAllScreens = 0;
    static synchronized void sellOneStatic() { totalTicketsAllScreens++; }   // lock = StaticSyncDemo.class
    int local = 0;
    synchronized void sellOneInstance() { local++; }                         // lock = this (one per screen)

    public static void main(String[] args) throws Exception {
        StaticSyncDemo[] screens = new StaticSyncDemo[20];
        for (int i = 0; i < 20; i++) screens[i] = new StaticSyncDemo();
        Thread[] ts = new Thread[20];
        for (int i = 0; i < 20; i++) {
            final StaticSyncDemo s = screens[i];
            ts[i] = new Thread(() -> { for (int k = 0; k < 5000; k++) { s.sellOneInstance(); sellOneStatic(); } });
            ts[i].start();
        }
        for (Thread t : ts) t.join();
        System.out.println("Total across 20 screens = " + totalTicketsAllScreens + " (expected 100000)");
    }
}
