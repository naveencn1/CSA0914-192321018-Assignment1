/** Task 8: overloading (compile time) and dynamic dispatch (runtime) on the packaged model. */
public class DispatchDemo {
    public static void main(String[] args) {
        parking.model.Vehicle[] parked = {
            new parking.model.TwoWheeler("TN01A1"),
            new parking.model.Car("TN02B2"),
            new parking.model.ElectricCar("TN03C3") };

        System.out.println("Overload 1: calculateFee(int minutes)  |  Overload 2: calculateFee(LocalTime in, LocalTime out)");
        for (parking.model.Vehicle v : parked) {
            double byMinutes = v.calculateFee(130);
            double byClock = v.calculateFee(java.time.LocalTime.of(8, 0), java.time.LocalTime.of(10, 10));
            System.out.println("declared type = Vehicle, actual type = " + v.getClass().getSimpleName()
                    + " -> calculateFee(130) = " + byMinutes + ", calculateFee(8:00,10:10) = " + byClock);
        }
        System.out.println("The compiler chose the overload from the argument types; the JVM chose ratePerHour() from the actual object.");
    }
}
