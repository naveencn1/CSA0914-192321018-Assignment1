# Task 5 – static Members and Object Life Cycle
`ParkingTicket.nextId` is static: the counter belongs to the class; an instance variable would restart at 1 per ticket. After exit the ticket is removed from the active map and becomes eligible for garbage collection once unreferenced. Objects are passed by reference value.
Alternative: instance counter per facility gives duplicate numbers across north/south gates; fix by keeping static or prefixing a gate code. Space O(1) static vs O(k) for k facilities.
Run: `Task05Demo`.
