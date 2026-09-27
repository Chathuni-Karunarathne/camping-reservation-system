package camp_res_system.ui;

import static org.junit.jupiter.api.Assertions.*;

import camp_res_system.config.*;
import camp_res_system.model.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;
import javax.swing.*;

/** Opt-in real desktop smoke test. It never opens the user's working database. */
@EnabledIfSystemProperty(named = "tenttrack.uiSmoke", matches = "true")
class UiSmokeTest {
    @TempDir Path temp;
    AppFrame frame;
    AppContext context;
    final List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
    Thread.UncaughtExceptionHandler previous;

    @BeforeEach
    void start() throws Exception {
        previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> failures.add(error));
        Database db = new Database(temp.resolve("desktop.db"));
        db.initialize();
        context = new AppContext(db);
        edt(
                () -> {
                    Theme.install();
                    frame = new AppFrame(context, true);
                    frame.setVisible(true);
                    return null;
                });
    }

    @AfterEach
    void close() throws Exception {
        edt(
                () -> {
                    for (Window w : Window.getWindows()) w.dispose();
                    return null;
                });
        Thread.setDefaultUncaughtExceptionHandler(previous);
    }

    @Test
    void completeCustomerAndAdministratorJourney() throws Exception {
        screenshot("01-setup", frame);
        fill("Full name *", "Demo Administrator");
        fill("Email *", "admin@example.test");
        fill("Username", "admin");
        fill("Password", "Desktop-test-123!");
        fill("Confirm password *", "Desktop-test-123!");
        click("Create administrator");
        idle();
        assertEquals(User.Role.ADMIN, context.session.current().role());
        screenshot("02-admin-dashboard", frame);
        click("Campsites");
        idle();
        click("Load sample campsites");
        idle();
        assertEquals(3, context.campsites.search("", true).size());
        screenshot("03-admin-campsites", frame);
        click("Add campsite");
        JDialog editor = visibleDialog();
        screenshot("04-campsite-editor", editor);
        closeDialog(editor);
        click("Reports");
        idle();
        click("Generate");
        idle();
        screenshot("05-reports", frame);
        click("Feedback");
        idle();
        screenshot("06-admin-feedback", frame);
        click("Sign out");
        click("New here? Create an account");
        screenshot("07-registration", frame);
        fill("Full name *", "Demo Camper");
        fill("Email *", "camper@example.test");
        fill("Username", "camper");
        fill("Password", "Desktop-test-123!");
        fill("Confirm password *", "Desktop-test-123!");
        click("Create account");
        idle();
        assertEquals(User.Role.USER, context.session.current().role());
        screenshot("08-user-dashboard", frame);
        click("Campsites");
        idle();
        assertEquals(3, bookingButtonCount());
        fill("Search", "no-such-campsite");
        assertEquals(0, bookingButtonCount());
        click("Reset filters");
        assertEquals(3, bookingButtonCount());
        edt(
                () -> {
                    find(frame, JComboBox.class).stream()
                            .filter(
                                    c ->
                                            "Province"
                                                    .equals(
                                                            c.getAccessibleContext()
                                                                    .getAccessibleName()))
                            .findFirst()
                            .orElseThrow()
                            .setSelectedItem("Southern");
                    return null;
                });
        assertEquals(1, bookingButtonCount());
        click("Reset filters");
        screenshot("18-campsite-cards", frame);
        click("Details & booking");
        JDialog booking = visibleDialog();
        clickIn(booking, "Check availability");
        idle();
        edt(
                () -> {
                    find(booking, JCheckBox.class).getFirst().doClick();
                    return null;
                });
        screenshot("09-booking", booking);
        clickIn(booking, "Confirm reservation");
        idle();
        JDialog bill = visibleDialog();
        assertEquals(1, context.reservations.list().size());
        screenshot("10-bill", bill);
        closeDialog(bill);
        idle();
        screenshot("11-reservations", frame);
        click("Feedback");
        idle();
        edt(
                () -> {
                    find(frame, JTextArea.class).stream()
                            .filter(JTextArea::isEditable)
                            .findFirst()
                            .orElseThrow()
                            .setText("A peaceful stay. Thank you!");
                    return null;
                });
        click("Send feedback");
        idle();
        screenshot("12-feedback", frame);
        edt(
                () -> {
                    find(frame, JComboBox.class).getFirst().setSelectedIndex(1);
                    return null;
                });
        idle();
        click("Resumen");
        idle();
        screenshot("13-spanish-dashboard", frame);
        click("Campings");
        idle();
        screenshot("14-spanish-campsites", frame);
        edt(
                () -> {
                    frame.setSize(1000, 720);
                    return null;
                });
        Thread.sleep(200);
        screenshot("19-compact-spanish", frame);
        edt(
                () -> {
                    frame.setSize(1200, 820);
                    return null;
                });
        edt(
                () -> {
                    find(frame, JComboBox.class).getFirst().setSelectedIndex(0);
                    return null;
                });
        idle();
        click("Sign out");
        fill("Username", "admin");
        fill("Password", "Desktop-test-123!");
        click("Sign in");
        idle();
        click("Reservations");
        idle();
        assertEquals(1, edt(() -> find(frame, JTable.class).getFirst().getRowCount()));
        screenshot("15-admin-reservations", frame);
        click("Reports");
        idle();
        click("Generate");
        idle();
        assertEquals(1, edt(() -> find(frame, JTable.class).getFirst().getRowCount()));
        screenshot("16-populated-report", frame);
        click("Feedback");
        idle();
        assertEquals(1, edt(() -> find(frame, JTable.class).getFirst().getRowCount()));
        screenshot("17-populated-feedback", frame);
        assertTrue(failures.isEmpty(), failures.toString());
    }

    private void fill(String label, String text) throws Exception {
        edt(
                () -> {
                    find(frame, JTextField.class).stream()
                            .filter(c -> label.equals(c.getAccessibleContext().getAccessibleName()))
                            .findFirst()
                            .orElseThrow(() -> new AssertionError(label))
                            .setText(text);
                    return null;
                });
    }

    private long bookingButtonCount() throws Exception {
        return edt(
                () ->
                        find(frame, JButton.class).stream()
                                .filter(b -> "Details & booking".equals(b.getText()))
                                .count());
    }

    private void click(String text) throws Exception {
        clickIn(frame, text);
    }

    private void clickIn(Container root, String text) throws Exception {
        JButton button =
                edt(
                        () ->
                                find(root, JButton.class).stream()
                                        .filter(b -> text.equals(b.getText()))
                                        .findFirst()
                                        .orElseThrow(
                                                () ->
                                                        new AssertionError(
                                                                "Missing button: " + text)));
        assertTrue(edt(button::isEnabled), "Disabled: " + text);
        SwingUtilities.invokeLater(button::doClick);
        Thread.sleep(180);
    }

    private JDialog visibleDialog() throws Exception {
        for (int i = 0; i < 100; i++) {
            JDialog d =
                    edt(
                            () ->
                                    Arrays.stream(frame.getOwnedWindows())
                                            .filter(Window::isVisible)
                                            .filter(w -> w instanceof JDialog)
                                            .map(w -> (JDialog) w)
                                            .findFirst()
                                            .orElse(null));
            if (d != null) return d;
            Thread.sleep(100);
        }
        throw new AssertionError("No dialog appeared");
    }

    private void closeDialog(JDialog dialog) throws Exception {
        edt(
                () -> {
                    dialog.dispose();
                    return null;
                });
    }

    private void idle() throws Exception {
        for (int i = 0; i < 200; i++) {
            if (!edt(() -> frame.getRootPane().getGlassPane().isVisible())) {
                Thread.sleep(150);
                if (!edt(() -> frame.getRootPane().getGlassPane().isVisible())) return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Background work did not finish");
    }

    private void screenshot(String name, Window window) throws Exception {
        Files.createDirectories(Path.of("target/screenshots"));
        BufferedImage image =
                edt(
                        () -> {
                            BufferedImage out =
                                    new BufferedImage(
                                            window.getWidth(),
                                            window.getHeight(),
                                            BufferedImage.TYPE_INT_RGB);
                            Graphics2D g = out.createGraphics();
                            window.paint(g);
                            g.dispose();
                            return out;
                        });
        ImageIO.write(image, "png", Path.of("target/screenshots", name + ".png").toFile());
    }

    private static <T extends Component> List<T> find(Container root, Class<T> type) {
        List<T> out = new ArrayList<>();
        for (Component c : root.getComponents()) {
            if (type.isInstance(c)) out.add(type.cast(c));
            if (c instanceof Container nested) out.addAll(find(nested, type));
        }
        return out;
    }

    private static <T> T edt(Callable<T> task) throws Exception {
        java.util.concurrent.FutureTask<T> future = new java.util.concurrent.FutureTask<>(task);
        SwingUtilities.invokeAndWait(future);
        return future.get();
    }
}
