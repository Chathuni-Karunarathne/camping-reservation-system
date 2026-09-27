package camp_res_system.repository;

import camp_res_system.config.Database;
import camp_res_system.model.Reservation;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public final class ReservationRepository {
    private final Database db;
    private static final String SELECT =
            "SELECT r.*, c.name AS campsite_name, u.username FROM reservations r JOIN campsites c"
                    + " ON c.id=r.campsite_id JOIN users u ON u.id=r.user_id ";

    public ReservationRepository(Database db) {
        this.db = db;
    }

    public boolean available(Connection c, String site, LocalDate arrival, LocalDate departure)
            throws SQLException {
        try (PreparedStatement s =
                c.prepareStatement(
                        "SELECT 1 FROM reservations WHERE campsite_id=? AND status='CONFIRMED' AND"
                                + " arrival<? AND departure>? LIMIT 1")) {
            s.setString(1, site);
            s.setString(2, departure.toString());
            s.setString(3, arrival.toString());
            try (ResultSet r = s.executeQuery()) {
                return !r.next();
            }
        }
    }

    public long insert(
            Connection c,
            String site,
            long user,
            LocalDate arrival,
            LocalDate departure,
            int people,
            long rate)
            throws SQLException {
        try (PreparedStatement s =
                c.prepareStatement(
                        "INSERT INTO"
                            + " reservations(campsite_id,user_id,arrival,departure,people,rate_cents)"
                            + " VALUES(?,?,?,?,?,?)")) {
            s.setString(1, site);
            s.setLong(2, user);
            s.setString(3, arrival.toString());
            s.setString(4, departure.toString());
            s.setInt(5, people);
            s.setLong(6, rate);
            s.executeUpdate();
        }
        try (PreparedStatement s = c.prepareStatement("SELECT last_insert_rowid()");
                ResultSet r = s.executeQuery()) {
            r.next();
            return r.getLong(1);
        }
    }

    public List<Reservation> list(Long userId) throws SQLException {
        try (Connection c = db.open();
                PreparedStatement s =
                        c.prepareStatement(
                                SELECT
                                        + (userId == null ? "" : "WHERE r.user_id=? ")
                                        + "ORDER BY r.arrival DESC, r.id DESC")) {
            if (userId != null) s.setLong(1, userId);
            try (ResultSet r = s.executeQuery()) {
                List<Reservation> out = new ArrayList<>();
                while (r.next()) out.add(map(r));
                return out;
            }
        }
    }

    public Optional<Reservation> find(long id) throws SQLException {
        try (Connection c = db.open();
                PreparedStatement s = c.prepareStatement(SELECT + "WHERE r.id=?")) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? Optional.of(map(r)) : Optional.empty();
            }
        }
    }

    public boolean cancel(long id, Long owner, LocalDate today) throws SQLException {
        try (Connection c = db.open();
                PreparedStatement s =
                        c.prepareStatement(
                                "UPDATE reservations SET status='CANCELLED' WHERE id=? AND"
                                        + " status='CONFIRMED' AND departure>?"
                                        + (owner == null ? "" : " AND user_id=? AND arrival>=?"))) {
            s.setLong(1, id);
            s.setString(2, today.toString());
            if (owner != null) {
                s.setLong(3, owner);
                s.setString(4, today.toString());
            }
            return s.executeUpdate() == 1;
        }
    }

    private static Reservation map(ResultSet r) throws SQLException {
        return new Reservation(
                r.getLong("id"),
                r.getString("campsite_id"),
                r.getString("campsite_name"),
                r.getLong("user_id"),
                r.getString("username"),
                LocalDate.parse(r.getString("arrival")),
                LocalDate.parse(r.getString("departure")),
                r.getInt("people"),
                r.getLong("rate_cents"),
                r.getString("status"));
    }
}
