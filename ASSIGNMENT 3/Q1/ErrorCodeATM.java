// Name: V V Naveen Kumar | Register No: 192321018 | Course: Programming in Java (CSA09) | Assignment 3 (CO3)
import java.util.Scanner;

/** Q1 alternative: no exceptions - pre-check everything, return error codes; plus a cost benchmark. */
public class ErrorCodeATM {
    static final int OK = 0, NOT_NUMBER = -1, NEGATIVE = -2, TOO_MUCH = -3, NO_INPUT = -4;
    static double balance = 5000;

    static int withdraw(Scanner sc) {
        if (!sc.hasNext()) return NO_INPUT;
        if (!sc.hasNextInt()) return NOT_NUMBER;   // check 1 (account)
        sc.nextInt();
        if (!sc.hasNextInt()) return NOT_NUMBER;   // check 2 (amount)
        int amount = sc.nextInt();
        if (amount <= 0) return NEGATIVE;          // check 3
        if (amount > balance) return TOO_MUCH;     // check 4
        balance -= amount;
        return OK;
    }

    static int viaCode(int x)      { return x < 0 ? -1 : x; }
    static int viaException(int x) { if (x < 0) throw new IllegalArgumentException("neg"); return x; }

    public static void main(String[] args) {
        for (String in : new String[]{"1001 2000", "abc 5", "1001 xyz", "1001 -5", "1001 99999", "1001"})
            System.out.println(String.format("%-12s -> code %d", in, withdraw(new Scanner(in))));

        int N = 1_000_000; long sink = 0;
        for (int i = 0; i < 200_000; i++) { sink += viaCode(-i); try { viaException(-i); } catch (RuntimeException e) { sink++; } }
        long t0 = System.nanoTime();
        for (int i = 0; i < N; i++) sink += viaCode(-1);
        long codeNs = System.nanoTime() - t0;
        t0 = System.nanoTime();
        for (int i = 0; i < N; i++) { try { viaException(-1); } catch (RuntimeException e) { sink++; } }
        long excNs = System.nanoTime() - t0;
        System.out.printf("%nFailure path, %,d calls: error code = %.1f ms (%.1f ns/call), exception = %.1f ms (%.1f ns/call), ratio ~ %.0fx  (sink=%d)%n",
                N, codeNs / 1e6, codeNs / (double) N, excNs / 1e6, excNs / (double) N, excNs / (double) Math.max(1, codeNs), sink);
    }
}
