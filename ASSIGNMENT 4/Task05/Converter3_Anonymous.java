import java.awt.event.*;
import javax.swing.SwingUtilities;

/** STYLE 3: anonymous inner classes. Handler is written at the place of use; reuse needs a local variable. */
public class Converter3_Anonymous extends ConverterFrame {

    public Converter3_Anonymous() {
        super("Currency Converter - Style 3 (anonymous inner class)");
        ActionListener convertListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                convert();
            }
        };
        convertButton.addActionListener(convertListener);
        amountField.addActionListener(convertListener);   // reused through the local variable
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { confirmExit(); }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Converter3_Anonymous().setVisible(true));
    }
}
