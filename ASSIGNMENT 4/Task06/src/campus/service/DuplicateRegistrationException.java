package campus.service;

public class DuplicateRegistrationException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public DuplicateRegistrationException(String studentId, String eventName) {
        super("Student " + studentId + " is already registered for " + eventName + ".");
    }
}
