import java.awt.event.*;
import javax.swing.SwingUtilities;

/** STYLE 1: the frame itself implements ActionListener. One actionPerformed serves the button AND the Enter key. */
public class Converter1_FrameImplements extends ConverterFrame implements ActionListener {

    public Converter1_FrameImplements() {
        super("Currency Converter - Style 1 (frame implements ActionListener)");
        convertButton.addActionListener(this);   // 'this' is the listener
        amountField.addActionListener(this);     // Enter in the text field fires ActionEvent too
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { confirmExit(); }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        convert();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Converter1_FrameImplements().setVisible(true));
    }
}
