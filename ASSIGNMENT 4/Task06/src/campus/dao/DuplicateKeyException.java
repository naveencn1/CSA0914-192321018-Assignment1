package campus.dao;

/** The database rejected an insert because of the UNIQUE(student_id, event_id) constraint. */
public class DuplicateKeyException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public DuplicateKeyException(Throwable cause) { super("Duplicate registration", cause); }
}
