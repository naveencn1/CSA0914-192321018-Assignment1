import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;

/**
 * View/controller only: collects input, shows results. Each button has its OWN Action object
 * (name + behaviour + enabled state in one place). Payment runs in a SwingWorker so the UI never freezes.
 */
public class FeePaymentFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private final FeeService service;
    private final JTextField idField = new JTextField(12);
    private final JTextField amountField = new JTextField(12);
    private final JComboBox<String> modeBox = new JComboBox<>(FeeService.MODES.toArray(new String[0]));
    private final JLabel status = new JLabel("Ready.");
    private PaymentResult lastPayment;                       // enables the Receipt action after a successful payment

    private final Action payAction = new AbstractAction("Pay") {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) { startPayment(); }
    };
    private final Action receiptAction = new AbstractAction("Receipt") {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            JTextArea area = new JTextArea(service.receiptText(lastPayment));
            area.setEditable(false);
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            JOptionPane.showMessageDialog(FeePaymentFrame.this, area, "Receipt", JOptionPane.PLAIN_MESSAGE);
        }
    };
    private final Action clearAction = new AbstractAction("Clear") {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            idField.setText(""); amountField.setText(""); modeBox.setSelectedIndex(0);
            status.setText("Cleared.");
            idField.requestFocusInWindow();
        }
    };

    public FeePaymentFrame(FeeService service) {
        super("Student Fee Payment");
        this.service = service;
        receiptAction.setEnabled(false);

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
        form.add(new JLabel("Student ID:"));  form.add(idField);
        form.add(new JLabel("Amount (INR):")); form.add(amountField);
        form.add(new JLabel("Mode:"));        form.add(modeBox);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        buttons.add(new JButton(payAction));
        buttons.add(new JButton(receiptAction));
        buttons.add(new JButton(clearAction));

        JPanel south = new JPanel(new BorderLayout());
        south.add(buttons, BorderLayout.CENTER);
        south.add(status, BorderLayout.SOUTH);
        status.setBorder(BorderFactory.createEmptyBorder(4, 12, 8, 12));

        add(form, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        amountField.addActionListener(payAction);            // Enter in amount field = Pay
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private void startPayment() {
        PaymentRequest req = new PaymentRequest(idField.getText(), amountField.getText(), (String) modeBox.getSelectedItem());
        payAction.setEnabled(false);                         // prevent double submit
        status.setText("Processing payment...");
        new SwingWorker<PaymentResult, Void>() {
            @Override protected PaymentResult doInBackground() { return service.pay(req); }   // off the EDT
            @Override protected void done() {                                                 // back on the EDT
                payAction.setEnabled(true);
                try {
                    lastPayment = get();
                    receiptAction.setEnabled(true);
                    status.setText("Payment successful. Receipt No " + lastPayment.receiptNo()
                            + ", balance " + lastPayment.balanceAfter().toPlainString());
                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof ValidationException ve) {
                        status.setText("Please correct the input.");
                        JOptionPane.showMessageDialog(FeePaymentFrame.this, ve.getMessage(), "Invalid input", JOptionPane.WARNING_MESSAGE);
                    } else {
                        status.setText("Payment failed - see message.");
                        JOptionPane.showMessageDialog(FeePaymentFrame.this, "Payment could not be completed: " + cause.getMessage(),
                                "System error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    public static void main(String[] args) {
        // Demo with the in-memory repository. For MySQL use: new JdbcFeeRepository(url, user, password)
        FeeService service = new FeeService(new InMemoryFeeRepository());
        SwingUtilities.invokeLater(() -> new FeePaymentFrame(service).setVisible(true));
    }
}
