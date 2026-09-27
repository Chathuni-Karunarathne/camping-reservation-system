package camp_res_system.service;

import static org.junit.jupiter.api.Assertions.*;

import camp_res_system.config.*;
import camp_res_system.i18n.Messages;
import camp_res_system.model.*;
import camp_res_system.repository.*;
import camp_res_system.ui.Documents;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

class ApplicationTest {
    @TempDir Path temp;
    Database db;
    AppContext app;
    final LocalDate arrival = LocalDate.now().plusDays(10), departure = arrival.plusDays(3);
    static final String PASSWORD = "Test-password-123!";

    @BeforeEach
    void initialize() throws Exception {
        db = new Database(temp.resolve("test.db"));
        db.initialize();
        app = new AppContext(db);
        app.auth.setupAdmin("Test Admin", "admin@example.test", "administrator", pw(), pw());
        app.campsites.save(
                new Campsite("C100", "Original campsite", "Central", "A quiet place", 125050, true),
                true);
    }

    char[] pw() {
        return PASSWORD.toCharArray();
    }

    void user() {
        app.session.logout();
        app.auth.register("Camper", "camper@example.test", "camper", pw(), pw());
    }

    void admin() {
        app.auth.login("administrator", pw());
    }

    Reservation book() {
        return app.reservations.book("C100", arrival, departure, 2, 125050, true);
    }

    void error(String key, org.junit.jupiter.api.function.Executable executable) {
        assertEquals(key, assertThrows(AppException.class, executable).key());
    }

    @Test
    void initializationIsRepeatableAndEnforcesForeignKeys() throws Exception {
        db.initialize();
        try (Connection c = db.open();
                Statement s = c.createStatement()) {
            try (ResultSet r = s.executeQuery("PRAGMA foreign_keys")) {
                assertTrue(r.next());
                assertEquals(1, r.getInt(1));
            }
            assertThrows(
                    SQLException.class,
                    () ->
                            s.executeUpdate(
                                    "INSERT INTO feedback(user_id,message) VALUES(999,'hello')"));
        }
    }

    @Test
    void registrationHashesPasswordsAndLoginIsCaseInsensitive() throws Exception {
        user();
        try (Connection c = db.open();
                PreparedStatement s =
                        c.prepareStatement("SELECT password_hash FROM users WHERE username=?")) {
            s.setString(1, "camper");
            try (ResultSet r = s.executeQuery()) {
                assertTrue(r.next());
                assertNotEquals(PASSWORD, r.getString(1));
                assertTrue(Passwords.verify(pw(), r.getString(1)));
            }
        }
        app.session.logout();
        char[] password = pw();
        assertEquals(User.Role.USER, app.auth.login("CAMPER", password).role());
        assertEquals('\0', password[0]);
        error("error.credentials", () -> app.auth.login("camper", "wrong-password".toCharArray()));
        assertNull(app.session.current());
    }

    @Test
    void duplicateAccountsAndValidation() {
        user();
        error(
                "error.duplicateAccount",
                () -> app.auth.register("Other", "other@example.test", "CAMPER", pw(), pw()));
        error(
                "error.duplicateAccount",
                () -> app.auth.register("Other", "CAMPER@EXAMPLE.TEST", "other", pw(), pw()));
        error("error.account", () -> app.auth.register(" ", "invalid", "a", pw(), pw()));
        error(
                "error.password",
                () ->
                        app.auth.register(
                                "Name",
                                "n@example.test",
                                "name",
                                "short".toCharArray(),
                                "short".toCharArray()));
        error(
                "error.confirmPassword",
                () ->
                        app.auth.register(
                                "Name",
                                "n@example.test",
                                "name",
                                pw(),
                                "different-password".toCharArray()));
    }

    @Test
    void setupCannotCreateASecondAdministrator() {
        error(
                "error.setupComplete",
                () -> app.auth.setupAdmin("Other", "other@example.test", "other", pw(), pw()));
        assertFalse(app.auth.needsSetup());
    }

    @Test
    void servicesEnforceRolesAndLogout() {
        user();
        error("error.forbidden", () -> app.campsites.search("", true));
        error(
                "error.forbidden",
                () -> app.campsites.save(new Campsite("C2", "Bad", "Place", "", 100, true), true));
        error("error.forbidden", () -> app.campsites.archive("C100"));
        error("error.forbidden", () -> app.campsites.loadDemo());
        error("error.forbidden", () -> app.feedback.list());
        error("error.forbidden", () -> app.reservations.report(arrival, departure, null));
        app.session.logout();
        error("error.loginRequired", () -> app.reservations.list());
        error("error.loginRequired", () -> app.campsites.search("", false));
    }

    @Test
    void bookingDerivesNightsAndPreservesRateAfterPriceChange() {
        user();
        Reservation reservation = book();
        assertEquals(3, reservation.nights());
        assertEquals(750300, reservation.totalCents());
        admin();
        app.campsites.save(
                new Campsite("C100", "Original campsite", "Central", "Edited", 900000, true),
                false);
        assertEquals(125050, app.reservations.bill(reservation.id()).rateCents());
        assertEquals(750300, app.reservations.bill(reservation.id()).totalCents());
    }

    @Test
    void overlapRejectsAllShapesButAllowsCheckoutDay() {
        user();
        book();
        for (LocalDate[] range :
                List.of(
                        new LocalDate[] {arrival, departure},
                        new LocalDate[] {arrival.minusDays(1), arrival.plusDays(1)},
                        new LocalDate[] {arrival.plusDays(1), departure.plusDays(1)},
                        new LocalDate[] {arrival.minusDays(1), departure.plusDays(1)},
                        new LocalDate[] {arrival.plusDays(1), departure.minusDays(1)})) {
            assertFalse(app.reservations.quote("C100", range[0], range[1], 1).available());
            error(
                    "error.overlap",
                    () -> app.reservations.book("C100", range[0], range[1], 1, 125050, true));
        }
        assertTrue(app.reservations.quote("C100", departure, departure.plusDays(1), 1).available());
        app.reservations.book("C100", departure, departure.plusDays(1), 1, 125050, true);
        assertTrue(app.reservations.quote("C100", arrival.minusDays(2), arrival, 1).available());
    }

    @Test
    void datesPeopleTermsAndStaleRatesAreValidated() {
        user();
        error("error.dates", () -> app.reservations.quote("C100", null, departure, 1));
        error(
                "error.dates",
                () -> app.reservations.quote("C100", LocalDate.now().minusDays(1), departure, 1));
        error("error.dates", () -> app.reservations.quote("C100", departure, arrival, 1));
        error("error.dates", () -> app.reservations.quote("C100", arrival, arrival, 1));
        error("error.booking", () -> app.reservations.quote("C100", arrival, departure, 0));
        error(
                "error.booking",
                () -> app.reservations.quote("C100", arrival, arrival.plusDays(366), 1));
        error("error.siteMissing", () -> app.reservations.quote("missing", arrival, departure, 1));
        error(
                "error.terms",
                () -> app.reservations.book("C100", arrival, departure, 1, 125050, false));
        error(
                "error.priceChanged",
                () -> app.reservations.book("C100", arrival, departure, 1, 100, true));
        assertTrue(app.reservations.list().isEmpty());
    }

    @Test
    void customersCannotReadOrCancelOtherCustomersBookings() {
        user();
        Reservation r = book();
        app.session.logout();
        app.auth.register("Other", "other@example.test", "other", pw(), pw());
        assertTrue(app.reservations.list().isEmpty());
        error("error.forbidden", () -> app.reservations.bill(r.id()));
        error("error.forbidden", () -> app.reservations.cancel(r.id()));
        admin();
        assertEquals(r.id(), app.reservations.bill(r.id()).id());
        assertEquals(1, app.reservations.list().size());
    }

    @Test
    void cancellationReleasesAvailabilityAndPreservesHistory() {
        user();
        Reservation r = book();
        app.reservations.cancel(r.id());
        assertEquals("CANCELLED", app.reservations.bill(r.id()).status());
        assertTrue(app.reservations.quote("C100", arrival, departure, 2).available());
        assertNotEquals(r.id(), book().id());
    }

    @Test
    void campsiteSearchEditArchiveAndDemo() {
        assertEquals(1, app.campsites.search("central", true).size());
        assertTrue(app.campsites.search("' OR 1=1 --", true).isEmpty());
        app.campsites.save(
                new Campsite("C100", "Updated", "Western", "New details", 200000, true), false);
        assertEquals("Updated", app.campsites.search("C100", true).getFirst().name());
        app.campsites.loadDemo();
        app.campsites.loadDemo();
        assertEquals(4, app.campsites.search("", true).size());
        app.campsites.archive("C100");
        assertFalse(app.campsites.search("C100", true).getFirst().active());
        user();
        assertTrue(app.campsites.search("C100", false).isEmpty());
        error("error.siteMissing", () -> book());
    }

    @Test
    void feedbackPersistsAndReportsFilterActualRecords() {
        user();
        Reservation r = book();
        app.feedback.submit(" A lovely stay ");
        error("error.feedback", () -> app.feedback.submit(" "));
        admin();
        assertEquals("A lovely stay", app.feedback.list().getFirst().message());
        assertEquals(1, app.reservations.report(arrival, arrival, "C100").size());
        assertTrue(app.reservations.report(departure, departure, null).isEmpty());
        Messages m = new Messages();
        assertTrue(Documents.bill(r, m).contains("3"));
        assertTrue(
                Documents.report(app.reservations.report(arrival, departure, null), m)
                        .contains(m.money(r.totalCents())));
    }

    @Test
    void databaseTriggerPreventsBypassingOverlapChecks() throws Exception {
        user();
        Reservation r = book();
        try (Connection c = db.open();
                PreparedStatement s =
                        c.prepareStatement(
                                "INSERT INTO"
                                    + " reservations(campsite_id,user_id,arrival,departure,people,rate_cents)"
                                    + " VALUES(?,?,?,?,?,?)")) {
            s.setString(1, "C100");
            s.setLong(2, r.userId());
            s.setString(3, arrival.toString());
            s.setString(4, departure.toString());
            s.setInt(5, 1);
            s.setLong(6, 100);
            assertThrows(SQLException.class, s::executeUpdate);
        }
    }

    @Test
    void concurrentBookingOnlyAllowsOneWinner() throws Exception {
        user();
        AppContext other = new AppContext(db);
        other.auth.login("camper", pw());
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> results = new ArrayList<>();
            for (AppContext context : List.of(app, other))
                results.add(
                        pool.submit(
                                () -> {
                                    start.await();
                                    try {
                                        context.reservations.book(
                                                "C100", arrival, departure, 1, 125050, true);
                                        return true;
                                    } catch (AppException e) {
                                        assertEquals("error.overlap", e.key());
                                        return false;
                                    }
                                }));
            start.countDown();
            int successes = 0;
            for (Future<Boolean> result : results)
                if (result.get(15, TimeUnit.SECONDS)) successes++;
            assertEquals(1, successes);
        }
        assertEquals(1, app.reservations.list().size());
    }

    @Test
    void pricingUsesExactMinorUnitsAndRejectsInvalidCounts() {
        assertEquals(6_000_000_000L, ReservationService.price(1_000_000, 100, 60));
        assertEquals(750300, ReservationService.price(125050, 2, 3));
        error("error.booking", () -> ReservationService.price(100, -1, 3));
    }

    @Test
    void englishAndSpanishHaveIdenticalKeysAndLocalizedBills() {
        ResourceBundle en = ResourceBundle.getBundle("i18n.messages", Locale.ENGLISH);
        ResourceBundle es = ResourceBundle.getBundle("i18n.messages", Locale.forLanguageTag("es"));
        assertEquals(en.keySet(), es.keySet());
        Messages m = new Messages();
        user();
        Reservation r = book();
        String english = Documents.bill(r, m);
        m.setLocale(Locale.forLanguageTag("es"));
        assertNotEquals(english, Documents.bill(r, m));
        assertEquals("Resumen de reserva", m.text("bill.title"));
    }
}
