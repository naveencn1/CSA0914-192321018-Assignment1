import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Model: in-memory catalog with search and paging. No GUI code, so it can be tested without a window. */
public class LibraryCatalog {
    public record Book(int id, String title, String author, String category, boolean available) { }

    public static final String ALL = "All";
    private final List<Book> books = new ArrayList<>();

    public LibraryCatalog() {
        String[][] data = {
            {"Java: The Complete Reference", "Herbert Schildt", "Computer Science"},
            {"Effective Java", "Joshua Bloch", "Computer Science"},
            {"Head First Java", "Kathy Sierra", "Computer Science"},
            {"Database System Concepts", "Silberschatz", "Computer Science"},
            {"Computer Networks", "Andrew Tanenbaum", "Computer Science"},
            {"Operating System Concepts", "Galvin", "Computer Science"},
            {"Engineering Mathematics", "K. A. Stroud", "Mathematics"},
            {"Discrete Mathematics", "Kenneth Rosen", "Mathematics"},
            {"Linear Algebra Done Right", "Sheldon Axler", "Mathematics"},
            {"Probability and Statistics", "Walpole", "Mathematics"},
            {"Concepts of Physics", "H. C. Verma", "Physics"},
            {"Fundamentals of Physics", "Halliday", "Physics"},
            {"Wings of Fire", "A. P. J. Abdul Kalam", "Biography"},
            {"The Story of My Experiments with Truth", "M. K. Gandhi", "Biography"},
            {"Ignited Minds", "A. P. J. Abdul Kalam", "Biography"},
            {"Discovery of India", "Jawaharlal Nehru", "History"},
            {"India After Gandhi", "Ramachandra Guha", "History"},
            {"A Brief History of Time", "Stephen Hawking", "Physics"},
            {"Clean Code", "Robert C. Martin", "Computer Science"},
            {"Introduction to Algorithms", "Cormen", "Computer Science"},
        };
        for (int i = 0; i < data.length; i++) {
            books.add(new Book(i + 1, data[i][0], data[i][1], data[i][2], i % 4 != 3));  // every 4th book is on loan
        }
    }

    public static String[] categories() {
        return new String[] {ALL, "Computer Science", "Mathematics", "Physics", "Biography", "History"};
    }

    public List<Book> search(String keyword, String category) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<Book> out = new ArrayList<>();
        for (Book b : books) {
            boolean catOk = category == null || ALL.equals(category) || b.category().equals(category);
            boolean keyOk = k.isEmpty() || b.title().toLowerCase(Locale.ROOT).contains(k)
                    || b.author().toLowerCase(Locale.ROOT).contains(k);
            if (catOk && keyOk) out.add(b);
        }
        return out;
    }

    public static int pageCount(int total, int pageSize) {
        return Math.max(1, (total + pageSize - 1) / pageSize);
    }

    public static <T> List<T> page(List<T> all, int pageIndex, int pageSize) {
        int from = Math.min(all.size(), pageIndex * pageSize);
        int to = Math.min(all.size(), from + pageSize);
        return all.subList(from, to);
    }
}
