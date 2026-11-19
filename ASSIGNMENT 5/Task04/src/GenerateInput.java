import java.io.*;
import java.util.*;

/** Generates a pass file with a known number of deliberately damaged lines (every 'badEvery'-th). */
public class GenerateInput {
    public static void main(String[] a) throws Exception {
        int n = Integer.parseInt(a[0]); int badEvery = Integer.parseInt(a[1]); String out = a[2];
        String[] stops = {"Sriperumbudur","Guindy","Tambaram","Porur","Poonamallee","Avadi","Kundrathur","Vandalur"};
        int bad = 0;
        try (BufferedWriter w = new BufferedWriter(new FileWriter(out))) {
            for (int i = 1; i <= n; i++) {
                int id = 100000 + i;
                String line = "P" + id + "|19232" + (1000 + id % 9000) + "|Student " + id + "|" + (1 + id % 20) + "|"
                        + stops[id % stops.length] + "|2027-03-31|" + (6000 + id % 700);
                if (badEvery > 0 && i % badEvery == 0) {
                    bad++;
                    switch (bad % 6) {
                        case 0: line = "P" + id + "|x|y"; break;                                         // wrong field count
                        case 1: line = line.substring(0, line.lastIndexOf('|') + 1) + "N/A"; break;         // non-numeric amount
                        case 2: line = ""; break;                                                          // empty line
                        case 3: line = line.replace("2027-03-31", "31/03/2027"); break;                    // bad date
                        case 4: line = "P" + (100001) + line.substring(line.indexOf('|')); break;            // duplicate ID
                        default: line = line.substring(0, line.lastIndexOf('|') + 1) + "-5"; break;         // negative amount
                    }
                }
                w.write(line); w.newLine();
            }
        }
        System.out.println("generated " + n + " lines, " + bad + " deliberately damaged -> " + out);
    }
}
