package camp_res_system.service;

import camp_res_system.config.Database;
import camp_res_system.model.*;
import camp_res_system.repository.*;

import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class ReservationService {
    public record Quote(
            long rateCents, long nights, int people, long totalCents, boolean available) {}

    private final Database db;
    private final CampsiteRepository sites;
    private final ReservationRepository bookings;
    private final Session session;
    private final Clock clock;

    public ReservationService(
            Database db,
            CampsiteRepository sites,
            ReservationRepository bookings,
            Session session,
            Clock clock) {
        this.db = db;
        this.sites = sites;
        this.bookings = bookings;
        this.session = session;
        this.clock = clock;
    }

    public static long price(long rate, int people, long nights) {
        if (rate < 1
                || rate > 100_000_000
                || people < 1
                || people > 100
                || nights < 1
                || nights > 365) throw new AppException("error.booking");
        return Math.multiplyExact(Math.multiplyExact(rate, people), nights);
    }

    private long validate(LocalDate arrival, LocalDate departure, int people) {
        if (arrival == null
                || departure == null
                || arrival.isBefore(LocalDate.now(clock))
                || !departure.isAfter(arrival)) throw new AppException("error.dates");
        long nights = ChronoUnit.DAYS.between(arrival, departure);
        if (people < 1 || people > 100 || nights > 365) throw new AppException("error.booking");
        return nights;
    }

    public Quote quote(String site, LocalDate arrival, LocalDate departure, int people) {
        session.requireUser();
        long nights = validate(arrival, departure, people);
        try (Connection c = db.open()) {
            Campsite campsite =
                    sites.find(c, site)
                            .filter(Campsite::active)
                            .orElseThrow(() -> new AppException("error.siteMissing"));
            return new Quote(
                    campsite.rateCents(),
                    nights,
                    people,
                    price(campsite.rateCents(), people, nights),
                    bookings.available(c, site, arrival, departure));
        } catch (SQLException e) {
            throw new AppException("error.database", e);
        }
    }

    public Reservation book(
            String site,
            LocalDate arrival,
            LocalDate departure,
            int people,
            long expectedRate,
            boolean acceptedTerms) {
        User user = session.requireUser();
        validate(arrival, departure, people);
        if (!acceptedTerms) throw new AppException("error.terms");
        long id;
        try (Connection c = db.open();
                Statement transaction = c.createStatement()) {
            // Acquire SQLite's write lock before reading price/availability to avoid
            // check-then-insert races.
            transaction.execute("BEGIN IMMEDIATE");
            try {
                Campsite campsite =
                        sites.find(c, site)
                                .filter(Campsite::active)
                                .orElseThrow(() -> new AppException("error.siteMissing"));
                if (campsite.rateCents() != expectedRate)
                    throw new AppException("error.priceChanged");
                if (!bookings.available(c, site, arrival, departure))
                    throw new AppException("error.overlap");
                id =
                        bookings.insert(
                                c,
                                site,
                                user.id(),
                                arrival,
                                departure,
                                people,
                                campsite.rateCents());
                transaction.execute("COMMIT");
            } catch (SQLException | RuntimeException e) {
                try {
                    transaction.execute("ROLLBACK");
                } catch (SQLException rollback) {
                    e.addSuppressed(rollback);
                }
                throw e;
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("reservation_overlap"))
                throw new AppException("error.overlap");
            throw new AppException("error.database", e);
        }
        return bill(id);
    }

    public List<Reservation> list() {
        User user = session.requireUser();
        try {
            return bookings.list(user.role() == User.Role.ADMIN ? null : user.id());
        } catch (SQLException e) {
            throw new AppException("error.database", e);
        }
    }

    public Reservation bill(long id) {
        User user = session.requireUser();
        try {
            Reservation r = bookings.find(id).orElseThrow(() -> new AppException("error.notFound"));
            if (user.role() != User.Role.ADMIN && r.userId() != user.id())
                throw new AppException("error.forbidden");
            return r;
        } catch (SQLException e) {
            throw new AppException("error.database", e);
        }
    }

    public void cancel(long id) {
        User user = session.requireUser();
        bill(id);
        try {
            if (!bookings.cancel(
                    id, user.role() == User.Role.ADMIN ? null : user.id(), LocalDate.now(clock)))
                throw new AppException("error.cancel");
        } catch (SQLException e) {
            throw new AppException("error.database", e);
        }
    }

    /** Report totals are booking value, not payments or recognized revenue. */
    public List<Reservation> report(LocalDate from, LocalDate to, String campsiteId) {
        session.requireAdmin();
        if (from == null || to == null || to.isBefore(from))
            throw new AppException("error.reportDates");
        return list().stream()
                .filter(r -> !r.arrival().isBefore(from) && !r.arrival().isAfter(to))
                .filter(r -> campsiteId == null || r.campsiteId().equalsIgnoreCase(campsiteId))
                .toList();
    }
}
