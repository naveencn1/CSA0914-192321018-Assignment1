import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.*;

/**
 * Shared screen. Only the EVENT WIRING differs between the four versions
 * (Converter1..Converter4), so the code-change comparison is easy to see.
 */
public abstract class ConverterFrame extends JFrame {
    protected final JTextField amountField = new JTextField(10);
    protected final JComboBox<String> fromBox = new JComboBox<>(ConverterCore.currencies());
    protected final JComboBox<String> toBox = new JComboBox<>(ConverterCore.currencies());
    protected final JButton convertButton = new JButton("Convert");
    protected final JLabel resultLabel = new JLabel("Result: -", SwingConstants.CENTER);

    protected ConverterFrame(String title) {
        super(title);
        toBox.setSelectedItem("INR");
        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        form.add(new JLabel("Amount:"));       form.add(amountField);
        form.add(new JLabel("From:"));         form.add(fromBox);
        form.add(new JLabel("To:"));           form.add(toBox);

        JPanel south = new JPanel(new GridLayout(2, 1, 4, 4));
        south.setBorder(BorderFactory.createEmptyBorder(4, 12, 12, 12));
        south.add(convertButton);
        south.add(resultLabel);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); // closing is handled by a WindowAdapter in each version
        pack();
        setLocationRelativeTo(null);
    }

    /** The one piece of event-handling logic shared by all four styles. */
    protected void convert() {
        try {
            double amount = ConverterCore.parseAmount(amountField.getText());
            String from = (String) fromBox.getSelectedItem();
            String to = (String) toBox.getSelectedItem();
            double result = ConverterCore.convert(amount, from, to);
            String text = String.format("%.2f %s = %.2f %s", amount, from, result, to);
            resultLabel.setText("Result: " + text);
            JOptionPane.showMessageDialog(this, text, "Conversion result", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            resultLabel.setText("Result: -");
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid amount", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Called from the WindowAdapter of each version. */
    protected void confirmExit() {
        int choice = JOptionPane.showConfirmDialog(this, "Close the converter?", "Exit",
                JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            System.exit(0);
        }
    }
}
