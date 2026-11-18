// Name: V V Naveen Kumar | Register No: 192321018 | Course: Programming in Java (CSA09) | Assignment 3 (CO3)
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;

/* ---------- Exception hierarchy ----------
 * Throwable -> Exception -> ExamException (checked, base of portal rules)
 *                              |- ExamClosedException        (checked)
 *                              |- NotRegisteredException     (checked)
 *                              |- SubmissionStorageException (checked, wraps IOException)
 * Throwable -> Exception -> RuntimeException -> FileTooLargeException (unchecked: a pre-condition the
 *              controller can validate; forgetting it is a programming/validation bug, not a recoverable state)
 */
class ExamException extends Exception {
    ExamException(String m) { super(m); }
    ExamException(String m, Throwable c) { super(m, c); }
}
class ExamClosedException extends ExamException {
    ExamClosedException(String m) { super(m); }
}
class NotRegisteredException extends ExamException {
    NotRegisteredException(String m) { super(m); }
}
class SubmissionStorageException extends ExamException {
    SubmissionStorageException(String m, Throwable c) { super(m, c); }
}
class FileTooLargeException extends RuntimeException {
    FileTooLargeException(String m) { super(m); }
}

/* ---------- Layers ---------- */
class StorageLayer {
    void store(String student, String file, boolean diskFull, boolean keepCause) throws SubmissionStorageException {
        try {
            if (diskFull) throw new IOException("Disk quota exceeded");
            System.out.println("  [storage] saved " + file + " for " + student);
        } catch (IOException e) {
            if (keepCause) throw new SubmissionStorageException("Could not store " + file, e);   // chained
            throw new SubmissionStorageException("Could not store " + file, null);               // cause LOST
        }
    }
}

class ServiceLayer {
    private final StorageLayer storage = new StorageLayer();
    private final Set<String> registered = Set.of("S1", "S2");
    boolean keepCause = true;

    void submit(String student, String course, LocalDateTime now, LocalDateTime deadline,
                String file, long sizeKb, boolean diskFull) throws ExamException {
        if (now.isAfter(deadline)) throw new ExamClosedException("Exam window closed at " + deadline);
        if (!registered.contains(student)) throw new NotRegisteredException(student + " is not registered for " + course);
        if (sizeKb > 500) throw new FileTooLargeException("File is " + sizeKb + " KB, limit is 500 KB");
        storage.store(student, file, diskFull, keepCause);        // may throw SubmissionStorageException (propagates up)
    }
}

class ControllerLayer {
    final ServiceLayer service = new ServiceLayer();

    /** Final handling point: the student only ever sees a friendly message. */
    String handle(String student, long sizeKb, boolean closed, boolean diskFull) {
        LocalDateTime deadline = LocalDateTime.of(2026, 11, 20, 12, 0);
        LocalDateTime now = closed ? deadline.plusHours(1) : deadline.minusHours(1);
        try {
            service.submit(student, "CSA09", now, deadline, "answer.pdf", sizeKb, diskFull);
            return "Submission accepted";
        } catch (ExamClosedException | NotRegisteredException e) {
            return "Rejected: " + e.getMessage();
        } catch (SubmissionStorageException e) {
            e.printStackTrace(System.out);   // full chain goes to the log, not the student (shown here as evidence)
            return "Sorry, your file could not be saved. Please try again later.";
        } catch (ExamException e) {
            return "Unexpected exam error: " + e.getMessage();
        } catch (FileTooLargeException e) {
            return "Rejected: " + e.getMessage();
        }
    }
}

public class ExamPortal {
    public static void main(String[] args) {
        ControllerLayer c = new ControllerLayer();
        System.out.println("=== Test table (one scenario per exception) ===");
        System.out.println("1 normal               -> " + c.handle("S1", 120, false, false));
        System.out.println("2 window closed        -> " + c.handle("S1", 120, true,  false));
        System.out.println("3 not registered       -> " + c.handle("S9", 120, false, false));
        System.out.println("4 file too large       -> " + c.handle("S1", 800, false, false));

        System.out.println("\n=== 5 storage failure WITH cause (chained stack trace) ===");
        System.out.println("-> " + c.handle("S1", 120, false, true));

        System.out.println("\n=== 6 storage failure WITHOUT cause (information lost) ===");
        c.service.keepCause = false;
        System.out.println("-> " + c.handle("S1", 120, false, true));
        try {
            c.service.submit("S1", "CSA09", LocalDateTime.of(2026,11,20,10,0), LocalDateTime.of(2026,11,20,12,0), "a.pdf", 1, true);
        } catch (ExamException e) {
            System.out.println("getCause() = " + e.getCause() + "   <- the 'Disk quota exceeded' reason is gone");
        }
    }
}
