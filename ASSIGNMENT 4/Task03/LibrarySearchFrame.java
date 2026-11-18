import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Campus library search screen.
 *
 * Container tree (layout manager in brackets):
 *   JFrame content pane            [BorderLayout]
 *    +- NORTH  headerPanel         [BorderLayout]
 *    |    +- NORTH  titleLabel
 *    |    +- CENTER searchRow      [FlowLayout LEFT]  label, keywordField, categoryBox, searchButton
 *    +- CENTER JScrollPane(JTable)                    <- the only area that grows
 *    +- SOUTH  footerPanel         [BorderLayout]
 *         +- CENTER buttonRow      [FlowLayout CENTER] Prev, Next, Reserve
 *         +- SOUTH  statusLabel
 */
public class LibrarySearchFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final int PAGE_SIZE = 5;

    private final LibraryCatalog catalog = new LibraryCatalog();
    private final JTextField keywordField = new JTextField(20);
    private final JComboBox<String> categoryBox = new JComboBox<>(LibraryCatalog.categories());
    private final JButton searchButton = new JButton("Search");
    private final JButton prevButton = new JButton("Prev");
    private final JButton nextButton = new JButton("Next");
    private final JButton reserveButton = new JButton("Reserve");
    private final JLabel statusLabel = new JLabel("Enter a keyword and press Search.");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[] {"ID", "Title", "Author", "Category", "Status"}, 0) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(tableModel);

    private List<LibraryCatalog.Book> results = new ArrayList<>();
    private List<LibraryCatalog.Book> pageBooks = new ArrayList<>();
    private int pageIndex = 0;

    public LibrarySearchFrame() {
        super("Campus Library Search");
        setLayout(new BorderLayout(8, 8));                         // outer: BorderLayout

        JLabel title = new JLabel("Campus Library Search", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));  // keeps preferred sizes
        searchRow.add(new JLabel("Search:"));
        searchRow.add(keywordField);
        searchRow.add(categoryBox);
        searchRow.add(searchButton);

        JPanel header = new JPanel(new BorderLayout());
        header.add(title, BorderLayout.NORTH);
        header.add(searchRow, BorderLayout.CENTER);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        buttonRow.add(prevButton);
        buttonRow.add(nextButton);
        buttonRow.add(reserveButton);

        JPanel footer = new JPanel(new BorderLayout());
        footer.add(buttonRow, BorderLayout.CENTER);
        footer.add(statusLabel, BorderLayout.SOUTH);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));

        add(header, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);          // CENTER takes all spare space
        add(footer, BorderLayout.SOUTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        searchButton.addActionListener(e -> doSearch());
        keywordField.addActionListener(e -> doSearch());           // Enter also searches
        prevButton.addActionListener(e -> showPage(pageIndex - 1));
        nextButton.addActionListener(e -> showPage(pageIndex + 1));
        reserveButton.addActionListener(e -> doReserve());

        updateButtons();
        setMinimumSize(new Dimension(560, 360));
        setSize(800, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void doSearch() {
        results = catalog.search(keywordField.getText(), (String) categoryBox.getSelectedItem());
        showPage(0);
        statusLabel.setText(results.isEmpty() ? "No books found." : results.size() + " book(s) found.");
    }

    private void showPage(int index) {
        int pages = LibraryCatalog.pageCount(results.size(), PAGE_SIZE);
        pageIndex = Math.max(0, Math.min(index, pages - 1));
        pageBooks = LibraryCatalog.page(results, pageIndex, PAGE_SIZE);
        tableModel.setRowCount(0);
        for (LibraryCatalog.Book b : pageBooks) {
            tableModel.addRow(new Object[] {b.id(), b.title(), b.author(), b.category(),
                    b.available() ? "Available" : "On loan"});
        }
        if (!results.isEmpty()) {
            statusLabel.setText("Page " + (pageIndex + 1) + " of " + pages + " (" + results.size() + " results)");
        }
        updateButtons();
    }

    private void doReserve() {
        int row = table.getSelectedRow();
        if (row < 0) {
            statusLabel.setText("Select a book in the table first.");
            return;
        }
        LibraryCatalog.Book b = pageBooks.get(row);
        statusLabel.setText((b.available() ? "Reserved: " : "Added to waiting list: ") + b.title());
    }

    private void updateButtons() {
        int pages = LibraryCatalog.pageCount(results.size(), PAGE_SIZE);
        prevButton.setEnabled(pageIndex > 0);
        nextButton.setEnabled(pageIndex < pages - 1);
        reserveButton.setEnabled(!pageBooks.isEmpty());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LibrarySearchFrame().setVisible(true));
    }
}
