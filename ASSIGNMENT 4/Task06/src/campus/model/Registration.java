package campus.model;

import java.time.LocalDateTime;

public record Registration(long id, String studentId, String studentName, String email,
                           int eventId, String eventName, LocalDateTime registeredAt) { }
