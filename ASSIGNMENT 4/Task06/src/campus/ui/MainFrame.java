package campus.ui;

import campus.model.EventInfo;
import campus.model.Registration;
import campus.service.RegistrationService;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Committee screen: menu bar, event filter, paged registrations table, Prev/Next, status line.
 * Every database call runs in a SwingWorker (background thread); Swing components are touched only on the EDT.
 */
public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final EventInfo ALL_EVENTS = new EventInfo(0, "All events");

    private final transient RegistrationService service;
    private final JComboBox<EventInfo> filterBox = new JComboBox<>();
    private final JButton prevButton = new JButton("< Prev");
    private final JButton nextButton = new JButton("Next >");
    private final JLabel pageLabel = new JLabel("Page 1 of 1");
    private final JLabel statusLabel = new JLabel("Starting...");
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] {"Reg ID", "Student ID", "Name", "E-mail", "Event", "Registered at"}, 0) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JProgressBar busy = new JProgressBar();

    private transient List<EventInfo> events = new ArrayList<>();
    private int pageIndex = 0;
    private int totalRows = 0;
    private int loadSequence = 0;                    // ignore results of a load that has been superseded

    public MainFrame(RegistrationService service) {
        super("Campus Event Registration");
        this.service = service;
        setJMenuBar(buildMenuBar());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        top.add(new JLabel("Filter by event:"));
        top.add(filterBox);

        JPanel bottom = new JPanel(new BorderLayout());
        JPanel pager = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        pager.add(prevButton); pager.add(pageLabel); pager.add(nextButton);
        JPanel statusRow = new JPanel(new BorderLayout(8, 0));
        statusRow.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));
        statusRow.add(statusLabel, BorderLayout.CENTER);
        busy.setIndeterminate(true);
        busy.setVisible(false);
        busy.setPreferredSize(new Dimension(120, 14));
        statusRow.add(busy, BorderLayout.EAST);
        bottom.add(pager, BorderLayout.CENTER);
        bottom.add(statusRow, BorderLayout.SOUTH);

        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(false);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        filterBox.addActionListener(e -> { if (filterBox.isEnabled()) { pageIndex = 0; loadPage(); } });
        prevButton.addActionListener(e -> { pageIndex--; loadPage(); });
        nextButton.addActionListener(e -> { pageIndex++; loadPage(); });

        setSize(900, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        loadEventsThenPage();
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem export = new JMenuItem("Export to CSV...");
        export.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent_CTRL()));
        export.addActionListener(e -> exportCsv());
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> dispose());
        file.add(export); file.addSeparator(); file.add(exit);

        JMenu regs = new JMenu("Registrations");
        JMenuItem register = new JMenuItem("Register Student...");
        register.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent_CTRL()));
        register.addActionListener(e -> openRegisterDialog());
        JMenuItem refresh = new JMenuItem("Refresh");
        refresh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        refresh.addActionListener(e -> loadPage());
        regs.add(register); regs.add(refresh);

        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Campus Event Registration\nSwing + JDBC (UI / service / DAO layers)", "About", JOptionPane.INFORMATION_MESSAGE));
        help.add(about);

        bar.add(file); bar.add(regs); bar.add(help);
        return bar;
    }

    private static int InputEvent_CTRL() { return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(); }

    private Integer selectedEventId() {
        EventInfo e = (EventInfo) filterBox.getSelectedItem();
        return (e == null || e.id() == 0) ? null : e.id();
    }

    /** Startup: load the event list (background), fill the filter, then load page 1. */
    private void loadEventsThenPage() {
        setBusy(true, "Loading events...");
        new SwingWorker<List<EventInfo>, Void>() {
            @Override protected List<EventInfo> doInBackground() { return service.events(); }
            @Override protected void done() {
                try {
                    events = get();
                    filterBox.setEnabled(false);               // avoid firing the filter listener while filling
                    filterBox.removeAllItems();
                    filterBox.addItem(ALL_EVENTS);
                    events.forEach(filterBox::addItem);
                    filterBox.setEnabled(true);
                    loadPage();
                } catch (ExecutionException ex) {
                    fail("Could not load events", ex.getCause());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private record PageData(List<Registration> rows, int total) { }

    /** Loads ONE page (LIMIT/OFFSET) plus the total count, in the background. */
    private void loadPage() {
        final int seq = ++loadSequence;
        final Integer eventId = selectedEventId();
        final int wanted = Math.max(0, pageIndex);
        setBusy(true, "Loading...");
        new SwingWorker<PageData, Void>() {
            @Override protected PageData doInBackground() {
                int total = service.count(eventId);
                int pages = RegistrationService.pageCount(total);
                int p = Math.min(wanted, pages - 1);
                return new PageData(service.page(eventId, p), total);
            }
            @Override protected void done() {
                if (seq != loadSequence) return;               // a newer request replaced this one
                try {
                    PageData d = get();
                    totalRows = d.total();
                    int pages = RegistrationService.pageCount(totalRows);
                    pageIndex = Math.min(Math.max(0, wanted), pages - 1);
                    model.setRowCount(0);
                    for (Registration r : d.rows()) {
                        model.addRow(new Object[] {r.id(), r.studentId(), r.studentName(), r.email(), r.eventName(),
                                r.registeredAt().withNano(0).toString().replace('T', ' ')});
                    }
                    pageLabel.setText("Page " + (pageIndex + 1) + " of " + pages);
                    prevButton.setEnabled(pageIndex > 0);
                    nextButton.setEnabled(pageIndex < pages - 1);
                    setBusy(false, totalRows + " registration(s) - showing " + d.rows().size() + " on this page");
                } catch (ExecutionException ex) {
                    fail("Could not load registrations", ex.getCause());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void openRegisterDialog() {
        RegisterDialog dialog = new RegisterDialog(this, service, events);
        dialog.setVisible(true);                               // modal: returns when closed
        if (dialog.wasRegistered()) loadPage();
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("registrations.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path target = chooser.getSelectedFile().toPath();
        Integer eventId = selectedEventId();
        setBusy(true, "Exporting...");
        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() throws IOException { return service.exportCsv(target, eventId); }
            @Override protected void done() {
                try {
                    int n = get();
                    setBusy(false, "Exported " + n + " row(s) to " + target);
                } catch (ExecutionException ex) {
                    fail("Export failed", ex.getCause());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void setBusy(boolean isBusy, String message) {
        busy.setVisible(isBusy);
        statusLabel.setText(message);
        setCursor(Cursor.getPredefinedCursor(isBusy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void fail(String what, Throwable cause) {
        setBusy(false, what + ".");
        JOptionPane.showMessageDialog(this, what + ":\n" + cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
