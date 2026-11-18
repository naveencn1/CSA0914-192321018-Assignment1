import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * REFERENCE COPY OF THE BROKEN PROGRAM (rebuilt from the task description - replace with your supplied file).
 * Do not "fix" this file; it is kept to demonstrate the bugs.
 */
public class DrawingPadFlawed extends JFrame {
    private final JLabel countLabel = new JLabel("Clicks: 0");
    private int count = 0;

    public DrawingPadFlawed() {
        super("Drawing Pad (flawed)");
        add(countLabel, BorderLayout.NORTH);
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        addMouseListener(new MouseAdapter() {
            // BUG 1: wrong method name. MouseAdapter has no abstract methods, so this compiles,
            // but mouseClick(...) is never called by Swing - the real callback is mouseClicked(...).
            public void mouseClick(MouseEvent e) {
                count++;
                countLabel.setText("Clicks: " + count);
                // BUG 2: drawing with getGraphics() is temporary - it is erased at the next repaint
                Graphics g = getGraphics();
                g.fillOval(e.getX() - 5, e.getY() - 5, 10, 10);
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DrawingPadFlawed().setVisible(true));
    }
}
