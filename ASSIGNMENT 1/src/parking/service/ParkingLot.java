package parking.service;

import parking.model.*;
import java.time.*;
import java.util.*;

/** Facade for the garage: jagged slot array + HashMap of active tickets (O(1) exit lookup). */
public class ParkingLot {
    private final Vehicle[][] slots;                         // jagged: floors may differ in size
    private final Map<String, ParkingTicket> active = new HashMap<>();
    private final PricingRule pricing;
    private final List<Pass> passes = new ArrayList<>();
    private double collection = 0;

    public ParkingLot(int[] slotsPerFloor, PricingRule pricing) {
        if (slotsPerFloor == null || slotsPerFloor.length == 0) throw new IllegalArgumentException("no floors");
        this.pricing = pricing;
        slots = new Vehicle[slotsPerFloor.length][];
        for (int f = 0; f < slotsPerFloor.length; f++) {
            if (slotsPerFloor[f] <= 0) throw new IllegalArgumentException("floor needs >=1 slot");
            slots[f] = new Vehicle[slotsPerFloor[f]];
        }
    }
    public void addPass(Pass p) { passes.add(p); }

    public ParkingTicket enter(Vehicle v, LocalDateTime when) {
        if (active.containsKey(v.getRegNo()))
            throw new ParkingException("Duplicate registration: " + v.getRegNo());
        for (int f = 0; f < slots.length; f++)
            for (int s = 0; s < slots[f].length; s++)
                if (slots[f][s] == null) {
                    slots[f][s] = v;
                    ParkingTicket t = new ParkingTicket(v, f, s, when);
                    active.put(v.getRegNo(), t);
                    return t;
                }
        throw new ParkingException("Parking full");
    }

    /** Releases the slot and returns the fee charged for this exit. */
    public double exit(String regNo, LocalDateTime when) {
        ParkingTicket t = active.get(regNo.trim().toUpperCase());
        if (t == null) throw new ParkingException("No active ticket for " + regNo);
        t.close(when);
        slots[t.getFloor()][t.getSlot()] = null;
        active.remove(t.getVehicle().getRegNo());
        double fee = covered(t.getVehicle(), when.toLocalDate()) ? 0
                   : pricing.fee(t.getVehicle(), t.getEntry(), when);
        collection += fee;
        return fee;
    }
    private boolean covered(Vehicle v, LocalDate d) {
        for (Pass p : passes) if (p.coversVehicle(v, d)) return true;
        return false;
    }
    public boolean release(int floor, int slot) {
        if (floor < 0 || floor >= slots.length || slot < 0 || slot >= slots[floor].length) return false;
        if (slots[floor][slot] == null) return false;
        active.remove(slots[floor][slot].getRegNo());
        slots[floor][slot] = null; return true;
    }
    public int occupied() { return active.size(); }
    public double totalCollection() { return collection; }
    public void addToCollection(double amt) { collection += amt; }   // e.g. day-pass sales
}
