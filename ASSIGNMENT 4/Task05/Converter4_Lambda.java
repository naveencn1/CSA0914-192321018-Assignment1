import java.awt.event.*;
import javax.swing.SwingUtilities;

/**
 * STYLE 4: lambda expression (ActionListener is a functional interface: one abstract method).
 * WindowAdapter is a class, not an interface, so it cannot be a lambda - it stays an anonymous class.
 */
public class Converter4_Lambda extends ConverterFrame {

    public Converter4_Lambda() {
        super("Currency Converter - Style 4 (lambda)");
        ActionListener convertListener = e -> convert();
        convertButton.addActionListener(convertListener);
        amountField.addActionListener(convertListener);
        // Method reference alternative:  convertButton.addActionListener(e -> convert());
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { confirmExit(); }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Converter4_Lambda().setVisible(true));
    }
}
