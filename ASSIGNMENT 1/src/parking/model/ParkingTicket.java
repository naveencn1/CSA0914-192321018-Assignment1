package parking.model;

import java.time.LocalDateTime;

/** Ticket: immutable record of one stay. Static counter guarantees unique ids (Tasks 4, 5). */
public class ParkingTicket {
    private static int nextId = 1;                 // class-level state (Task 5)
    private final int ticketNo;
    private final Vehicle vehicle;
    private final int floor, slot;
    private final LocalDateTime entry;
    private LocalDateTime exit;                    // null while parked

    public ParkingTicket(Vehicle vehicle, int floor, int slot, LocalDateTime entry) {
        if (vehicle == null || entry == null) throw new IllegalArgumentException("null argument");
        if (floor < 0 || slot < 0) throw new IllegalArgumentException("Invalid slot position");
        this.ticketNo = nextId++;
        this.vehicle = vehicle; this.floor = floor; this.slot = slot; this.entry = entry;
    }
    /** Copy constructor: duplicate print of the SAME ticket (same number, not a new one). */
    public ParkingTicket(ParkingTicket t) {
        this.ticketNo = t.ticketNo; this.vehicle = t.vehicle; this.floor = t.floor;
        this.slot = t.slot; this.entry = t.entry; this.exit = t.exit;
    }
    public void close(LocalDateTime exit) {
        if (exit.isBefore(entry)) throw new IllegalArgumentException("Exit before entry");
        this.exit = exit;
    }
    public int getTicketNo() { return ticketNo; }
    public Vehicle getVehicle() { return vehicle; }
    public int getFloor() { return floor; }
    public int getSlot() { return slot; }
    public LocalDateTime getEntry() { return entry; }
    public LocalDateTime getExit() { return exit; }
    public static void resetCounterForTests() { nextId = 1; }
    @Override public String toString() {
        return "Ticket#" + ticketNo + " " + vehicle + " F" + floor + "-S" + slot;
    }
}
