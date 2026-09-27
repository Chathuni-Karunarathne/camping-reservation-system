package camp_res_system;

import camp_res_system.config.*;
import camp_res_system.ui.*;

import java.util.logging.*;

import javax.swing.*;

public final class Project {
    private static final Logger LOG = Logger.getLogger(Project.class.getName());

    private Project() {}

    public static void main(String[] args) {
        // Initialization and password hashing must not block Swing's event dispatch thread.
        try {
            Database db = Database.configured();
            Logging.configure(db.path());
            db.initialize();
            AppContext context = new AppContext(db);
            boolean setup = context.auth.needsSetup();
            SwingUtilities.invokeLater(
                    () -> {
                        Theme.install();
                        new AppFrame(context, setup).setVisible(true);
                    });
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Could not start TentTrack", e);
            SwingUtilities.invokeLater(
                    () ->
                            JOptionPane.showMessageDialog(
                                    null,
                                    "TentTrack could not start. Check that the database location is"
                                            + " writable. See the application log for details.",
                                    "TentTrack",
                                    JOptionPane.ERROR_MESSAGE));
        }
    }
}
