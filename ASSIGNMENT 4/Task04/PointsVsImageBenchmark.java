import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Headless benchmark: cost of one repaint when (A) redrawing N stored Points vs (B) blitting an off-screen BufferedImage.
 * Run: java -Djava.awt.headless=true PointsVsImageBenchmark
 */
public class PointsVsImageBenchmark {
    public static void main(String[] args) {
        int w = 800, h = 600;
        int[] sizes = {100, 1_000, 10_000, 100_000};
        System.out.printf("%-10s %-22s %-22s %-18s%n", "N dots", "A: redraw points (ms)", "B: drawImage (ms)", "A list memory (KB)");
        for (int n : sizes) {
            List<Point> pts = new ArrayList<>();
            java.util.Random r = new java.util.Random(42);
            for (int i = 0; i < n; i++) pts.add(new Point(r.nextInt(w), r.nextInt(h)));

            BufferedImage buffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);   // off-screen image
            Graphics2D bg = buffer.createGraphics();
            bg.setColor(Color.WHITE); bg.fillRect(0, 0, w, h);
            bg.setColor(Color.RED);
            for (Point p : pts) bg.fillOval(p.x - 5, p.y - 5, 10, 10);                    // draw once, incrementally in real use
            bg.dispose();

            BufferedImage screen = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);   // stands in for the window
            int reps = 20;
            for (int i = 0; i < 3; i++) { paintPoints(screen, pts); paintImage(screen, buffer); }   // warm-up
            long t0 = System.nanoTime();
            for (int i = 0; i < reps; i++) paintPoints(screen, pts);
            double a = (System.nanoTime() - t0) / 1e6 / reps;
            t0 = System.nanoTime();
            for (int i = 0; i < reps; i++) paintImage(screen, buffer);
            double b = (System.nanoTime() - t0) / 1e6 / reps;
            long listKb = (long) n * (16 + 8 + 4 + 4) / 1024;   // rough: Point object (header+2 ints) + 8-byte reference in ArrayList
            System.out.printf("%-10d %-22.3f %-22.3f %-18d%n", n, a, b, listKb);
        }
        System.out.printf("Buffer memory is constant: %d x %d x 4 bytes = %d KB%n", w, h, (long) w * h * 4 / 1024);
    }

    static void paintPoints(BufferedImage screen, List<Point> pts) {
        Graphics2D g = screen.createGraphics();
        g.setColor(Color.WHITE); g.fillRect(0, 0, screen.getWidth(), screen.getHeight());
        g.setColor(Color.RED);
        for (Point p : pts) g.fillOval(p.x - 5, p.y - 5, 10, 10);
        g.dispose();
    }

    static void paintImage(BufferedImage screen, BufferedImage buffer) {
        Graphics2D g = screen.createGraphics();
        g.drawImage(buffer, 0, 0, null);
        g.dispose();
    }
}
