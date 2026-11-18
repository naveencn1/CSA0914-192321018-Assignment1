package campus.service;

import java.util.List;

public class ValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final transient List<String> problems;
    public ValidationException(List<String> problems) { super(String.join("\n", problems)); this.problems = List.copyOf(problems); }
    public List<String> getProblems() { return problems; }
}
