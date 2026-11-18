import java.awt.event.*;
import javax.swing.SwingUtilities;

/** STYLE 2: a named inner class. It can be reused for several components and can see the frame's fields. */
public class Converter2_NamedInner extends ConverterFrame {

    private class ConvertListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            convert();   // outer-class method, reachable because this is an inner class
        }
    }

    private class CloseHandler extends WindowAdapter {
        @Override public void windowClosing(WindowEvent e) { confirmExit(); }
    }

    public Converter2_NamedInner() {
        super("Currency Converter - Style 2 (named inner class)");
        ConvertListener listener = new ConvertListener();  // one object, reused twice
        convertButton.addActionListener(listener);
        amountField.addActionListener(listener);
        addWindowListener(new CloseHandler());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Converter2_NamedInner().setVisible(true));
    }
}
