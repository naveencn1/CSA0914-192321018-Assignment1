package campus.ui;

import campus.dao.InMemoryRegistrationDao;
import campus.dao.JdbcRegistrationDao;
import campus.dao.RegistrationDao;
import campus.service.RegistrationService;
import javax.swing.*;

/**
 * Entry point.
 *   java -cp "out:lib/*" campus.ui.AppMain              -> MySQL using db.properties
 *   java -cp "out:lib/*" campus.ui.AppMain --demo       -> in-memory demo data (no database needed)
 */
public class AppMain {
    public static void main(String[] args) throws Exception {
        RegistrationDao dao = (args.length > 0 && args[0].equals("--demo"))
                ? new InMemoryRegistrationDao(1000)
                : JdbcRegistrationDao.fromProperties(args.length > 0 ? args[0] : "db.properties");
        RegistrationService service = new RegistrationService(dao);
        SwingUtilities.invokeLater(() -> new MainFrame(service).setVisible(true));
    }
}
