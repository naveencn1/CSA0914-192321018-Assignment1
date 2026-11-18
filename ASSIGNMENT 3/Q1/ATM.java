// Name: V V Naveen Kumar | Register No: 192321018 | Course: Programming in Java (CSA09) | Assignment 3 (CO3)
import java.io.*;
import java.util.*;

/** Checked: raised when the balance is too low (business rule, caller must handle). */
class InsufficientFundsException extends Exception {
    InsufficientFundsException(String msg) { super(msg); }
}

/**
 * Q1 - ATM withdrawal simulation.
 * Failure points: card file missing, account not numeric, amount not numeric,
 * amount missing, amount <= 0, amount > balance.
 */
public class ATM {
    static double balance = 5000;
    static int finallyRuns = 0;

    /** Returns the name of the path taken, so tests can record it. */
    static String withdraw(String cardFile, String input) {
        Scanner sc = null;
        String path;
        try {
            new FileReader(cardFile).close();               // FileNotFoundException / IOException (checked)
            sc = new Scanner(input);
            int account = Integer.parseInt(sc.next());      // NumberFormatException / NoSuchElementException
            int amount = sc.nextInt();                      // InputMismatchException / NoSuchElementException
            if (amount <= 0) throw new IllegalArgumentException("Amount must be positive: " + amount);
            if (amount > balance)
                throw new InsufficientFundsException("Requested " + amount + " but balance is " + balance);
            balance -= amount;
            System.out.println("  Dispensed " + amount + " to account " + account + ", balance " + balance);
            path = "no exception";
        }
        // Order matters: most specific first. Multi-catch alternatives must NOT be related by inheritance.
        catch (FileNotFoundException e) {
            System.out.println("  Card reader file missing: " + e.getMessage());
            path = "catch FileNotFoundException";
        } catch (NumberFormatException | InputMismatchException e) {   // NFE extends IllegalArgumentException,
            System.out.println("  Please type digits only: " + e.getClass().getSimpleName()); // so it must come BEFORE it
            path = "catch NumberFormatException | InputMismatchException";
        } catch (IllegalArgumentException e) {
            System.out.println("  Invalid amount: " + e.getMessage());
            path = "catch IllegalArgumentException";
        } catch (InsufficientFundsException e) {
            System.out.println("  Declined: " + e.getMessage());
            path = "catch InsufficientFundsException";
        } catch (NoSuchElementException e) {     // superclass of InputMismatchException -> must be after the multi-catch
            System.out.println("  Incomplete input: " + e.getMessage());
            path = "catch NoSuchElementException";
        } catch (IOException e) {
            System.out.println("  I/O problem: " + e.getMessage());
            path = "catch IOException";
        } finally {
            if (sc != null) sc.close();
            finallyRuns++;
            System.out.println("  [finally] scanner closed, card ejected");
        }
        return path;
    }

    public static void main(String[] args) throws IOException {
        File card = new File("card.dat");
        try (FileWriter w = new FileWriter(card)) { w.write("CARD-1001"); }

        String[][] tests = {
            {"T1 valid",             card.getPath(),     "1001 2000"},
            {"T2 letters account",   card.getPath(),     "abc 500"},
            {"T3 letters amount",    card.getPath(),     "1001 xyz"},
            {"T4 negative amount",   card.getPath(),     "1001 -50"},
            {"T5 over balance",      card.getPath(),     "1001 99999"},
            {"T6 card file missing", "no_such_card.dat", "1001 500"},
            {"T7 amount missing",    card.getPath(),     "1001"},
        };
        System.out.printf("%-22s | %-52s | finally%n", "Test", "Path taken");
        System.out.println("-".repeat(90));
        for (String[] t : tests) {
            System.out.println(t[0] + "  (input: \"" + t[2] + "\")");
            int before = finallyRuns;
            String path = withdraw(t[1], t[2]);
            System.out.printf("%-22s | %-52s | %s%n%n", t[0], path, finallyRuns == before + 1 ? "ran" : "DID NOT RUN");
        }
        card.delete();
    }
}
