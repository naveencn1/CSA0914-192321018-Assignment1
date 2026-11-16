package parking.model;
public class ElectricCar extends Car implements Chargeable {
    public ElectricCar(String regNo) { super(regNo); }
    @Override public double chargingFee(int kWh) { checkKwh(kWh); return kWh * RATE_PER_KWH; }
    @Override public String category() { return "EV"; }
}
