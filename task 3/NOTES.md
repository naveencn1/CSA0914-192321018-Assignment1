# Task 3 – Design the Control Logic (+ Complete the Code)
Run: `ParkingFacility` – output matches the expected output in the brief.

## Pseudocode – AllocateSlot
```
ALGORITHM AllocateSlot(slots, vehicleNo)
INPUT: slots[F][S_f], vehicleNo
OUTPUT: {floor, slot} or NULL
FOR f <- 0 TO F-1 DO
  FOR s <- 0 TO length(slots[f])-1 DO
    IF slots[f][s] = NULL THEN
      slots[f][s] <- vehicleNo
      RETURN {f, s}
    END IF
  END FOR
END FOR
RETURN NULL
```
## Pseudocode – ComputeFee
```
ALGORITHM ComputeFee(minutes, rate)
IF minutes < 0 THEN ERROR
IF minutes = 0 THEN RETURN 0
hours <- (minutes + 59) DIV 60
fee <- hours * rate
IF fee > DAILY_MAX THEN fee <- DAILY_MAX
RETURN fee
```
When no slot is free: `null` is returned (ParkingLot throws "Parking full").
Alternative: free-slot stack makes allocate/release O(1) vs O(F*S) scan (see Benchmarks).
