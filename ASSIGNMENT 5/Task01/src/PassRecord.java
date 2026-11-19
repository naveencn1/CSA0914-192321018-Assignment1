/** One bus-pass record: passId|registerNo|name|routeNo|boardingStop|validUntil|amountPaid */
public class PassRecord {
    public final String passId, registerNo, name, routeNo, boardingStop, validUntil;
    public final int amountPaid;

    public PassRecord(String passId, String registerNo, String name, String routeNo,
                      String boardingStop, String validUntil, int amountPaid) {
        this.passId = passId; this.registerNo = registerNo; this.name = name;
        this.routeNo = routeNo; this.boardingStop = boardingStop;
        this.validUntil = validUntil; this.amountPaid = amountPaid;
    }

    public String toLine() {
        return String.join("|", passId, registerNo, name, routeNo, boardingStop, validUntil,
                String.valueOf(amountPaid));
    }

    /** Returns null for ANY damaged line: empty, wrong field count, bad amount, blank key. */
    public static PassRecord parse(String line) {
        if (line == null || line.isBlank()) return null;
        String[] f = line.split("\\|", -1);          // -1 keeps trailing empty fields
        if (f.length != 7) return null;               // wrong number of fields
        if (f[0].isBlank()) return null;
        try {
            int amt = Integer.parseInt(f[6].trim());
            if (amt < 0) return null;
            return new PassRecord(f[0], f[1], f[2], f[3], f[4], f[5], amt);
        } catch (NumberFormatException e) {
            return null;                              // non-numeric amount
        }
    }

    @Override public String toString() { return toLine(); }
}
