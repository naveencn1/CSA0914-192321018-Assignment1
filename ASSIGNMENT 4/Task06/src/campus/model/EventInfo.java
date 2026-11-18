package campus.model;

public record EventInfo(int id, String name) {
    @Override public String toString() { return name; }   // shown directly in the JComboBox
}
