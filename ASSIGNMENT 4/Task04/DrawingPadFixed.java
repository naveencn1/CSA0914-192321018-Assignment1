import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/** Repaired drawing pad: store points (model), update label, call repaint(), draw stored points in paint code. */
public class DrawingPadFixed extends JFrame {
    private final List<Point> points = new ArrayList<>();          // TODO 1 (model): clicked points are stored here
    private final JLabel countLabel = new JLabel("Clicks: 0");
    private final DrawPanel canvas = new DrawPanel();

    /** Drawing surface. paintComponent is the Swing equivalent of AWT's paint(Graphics). */
    private class DrawPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        DrawPanel() { setBackground(Color.WHITE); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);                                // clears the background first
            g.setColor(Color.RED);
            for (Point p : points) {                                // TODO 2 (view): redraw every stored point
                g.fillOval(p.x - 5, p.y - 5, 10, 10);
            }
        }
    }

    public DrawingPadFixed() {
        super("Drawing Pad (fixed)");
        add(countLabel, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        canvas.addMouseListener(new MouseAdapter() {
            @Override                                               // compile error if the name were wrong
            public void mouseClicked(MouseEvent e) {
                points.add(e.getPoint());                           // 1. store
                countLabel.setText("Clicks: " + points.size());     // 2. update label
                canvas.repaint();                                   // 3. ask Swing to repaint -> paintComponent runs
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DrawingPadFixed().setVisible(true));
    }
}
