import java.util.List;

/** Thrown by FeeService when input breaks a business rule; carries ALL messages so the UI can show them together. */
public class ValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final transient List<String> problems;

    public ValidationException(List<String> problems) {
        super(String.join("\n", problems));
        this.problems = List.copyOf(problems);
    }

    public List<String> getProblems() { return problems; }
}
