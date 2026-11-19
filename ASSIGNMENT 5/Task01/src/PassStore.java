import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Delimited text-file store for bus passes (design from Task A1). */
public class PassStore {
    private final Path file;

    public PassStore(String path) { this.file = Paths.get(path); }

    /** ADD: append one line, flush + force to disk so a power cut loses at most this line. */
    public void add(PassRecord r) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file.toFile(), true);
             BufferedWriter w = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
            w.write(r.toLine());
            w.newLine();
            w.flush();
            fos.getFD().sync();
        }
    }

    /** FIND (TODO 1 completed): read line by line, skip damaged lines, return the matching record. */
    public PassRecord find(String passId) throws IOException {
        if (!Files.exists(file)) return null;                 // missing file -> not found
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                PassRecord r = PassRecord.parse(line);        // TODO 1: parse + skip damaged
                if (r == null) continue;                      // damaged line never stops the search
                if (r.passId.equals(passId)) return r;        // matching record
            }
        }
        return null;
    }

    /** LIST all valid passes on a route; damaged lines are counted, not fatal. */
    public List<PassRecord> listByRoute(String routeNo, int[] damagedCount) throws IOException {
        List<PassRecord> out = new ArrayList<>();
        if (!Files.exists(file)) return out;
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                PassRecord r = PassRecord.parse(line);
                if (r == null) { if (damagedCount != null) damagedCount[0]++; continue; }
                if (r.routeNo.equals(routeNo)) out.add(r);
            }
        }
        return out;
    }

    /** Alternative design: load all valid records into a HashMap once (O(n) build, O(1) find). */
    public Map<String, PassRecord> loadAll() throws IOException {
        Map<String, PassRecord> m = new HashMap<>();
        if (!Files.exists(file)) return m;
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                PassRecord r = PassRecord.parse(line);
                if (r != null) m.put(r.passId, r);
            }
        }
        return m;
    }
}
