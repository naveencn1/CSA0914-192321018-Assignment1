// Name: V V Naveen Kumar | Register No: 192321018 | Course: Programming in Java (CSA09) | Assignment 3 (CO3)
/** Q1 Student B: an uncaught exception and its stack trace. */
public class UncaughtDemo {
    static int parseAmount(String s) { return Integer.parseInt(s); }
    static void withdraw(String s)   { System.out.println("Amount = " + parseAmount(s)); }
    public static void main(String[] args) {
        withdraw("abc");   // nothing catches the NumberFormatException -> thread dies, JVM prints trace
    }
}
