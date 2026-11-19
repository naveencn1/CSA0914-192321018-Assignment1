import java.io.*;
import java.nio.file.*;

/** Task A2 / B1: line counting, unbuffered vs buffered vs readAllLines. */
public class LineCounter {
    /** One read() call per byte: 1 system call per byte. */
    public static long unbuffered(String path, long[] readCalls) throws IOException {
        long lines = 0, calls = 0;
        try (FileInputStream in = new FileInputStream(path)) {
            int b;
            while ((b = in.read()) != -1) { calls++; if (b == '\n') lines++; }
            calls++; // final read that returned -1
        }
        readCalls[0] = calls;
        return lines;
    }

    /** 64 KB byte-array buffer: one system call per 64 KB. */
    public static long buffered(String path, long[] readCalls) throws IOException {
        long lines = 0, calls = 0;
        byte[] buf = new byte[64 * 1024];
        try (FileInputStream in = new FileInputStream(path)) {
            int n;
            while ((n = in.read(buf)) != -1) {
                calls++;
                for (int i = 0; i < n; i++) if (buf[i] == '\n') lines++;
            }
            calls++;
        }
        readCalls[0] = calls;
        return lines;
    }

    /** BufferedReader.readLine(): convenient, 8 KB default buffer, creates a String per line. */
    public static long bufferedReader(String path) throws IOException {
        long lines = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            while (br.readLine() != null) lines++;
        }
        return lines;
    }

    /** Files.readAllLines(): loads EVERY line into memory: O(n) heap. */
    public static long readAllLines(String path) throws IOException {
        return Files.readAllLines(Paths.get(path)).size();
    }
}
