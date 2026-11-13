/** Task 2: data types, jagged array, casting risk. */
public class Task02Demo {
    public static void main(String[] args) {
        int minutes = 130;
        double rate = 30;

        double wrong1 = (minutes / 60) * rate;            // integer division -> 2 hours
        double wrong2 = ((double) minutes / 60) * rate;   // 2.1666.. hours -> not "started hours"
        double right  = ((minutes + 59) / 60) * rate;     // ceiling with int arithmetic -> 3 hours

        System.out.println("minutes = " + minutes + ", rate = Rs " + rate + "/hour");
        System.out.println("minutes / 60 (int division) -> Rs " + wrong1 + "   WRONG (under-charges)");
        System.out.println("(double) minutes / 60       -> Rs " + wrong2 + "   WRONG (fraction of an hour)");
        System.out.println("(minutes + 59) / 60         -> Rs " + right + "   CORRECT (every started hour)");

        // Jagged array: floors with different numbers of slots
        int[] slotsPerFloor = {10, 20, 30, 40, 50};
        String[][] slots = new String[slotsPerFloor.length][];
        int total = 0;
        for (int f = 0; f < slots.length; f++) { slots[f] = new String[slotsPerFloor[f]]; total += slots[f].length; }
        System.out.println("\nJagged array: " + slots.length + " floors, total slots = " + total);
        for (int f = 0; f < slots.length; f++) System.out.println("  floor " + f + " -> " + slots[f].length + " slots");

        // Input via command-line arguments / Scanner (shown with defaults)
        System.out.println("\nInput: Scanner(System.in) for interactive entry, or args[] for command-line values.");
    }
}
