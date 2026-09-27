package camp_res_system.repository;

import camp_res_system.config.Database;
import camp_res_system.model.Campsite;

import java.sql.*;
import java.util.*;

public final class CampsiteRepository {
    private final Database db;

    public CampsiteRepository(Database db) {
        this.db = db;
    }

    public List<Campsite> search(String query, boolean includeInactive) throws SQLException {
        try (Connection c = db.open();
                PreparedStatement s =
                        c.prepareStatement(
                                "SELECT * FROM campsites WHERE (?=1 OR active=1) AND"
                                    + " (instr(lower(id),lower(?))>0 OR"
                                    + " instr(lower(name),lower(?))>0 OR"
                                    + " instr(lower(province),lower(?))>0 OR CAST(rate_cents/100.0"
                                    + " AS TEXT)=?) ORDER BY name")) {
            s.setBoolean(1, includeInactive);
            for (int i = 2; i <= 5; i++) s.setString(i, query);
            try (ResultSet r = s.executeQuery()) {
                List<Campsite> out = new ArrayList<>();
                while (r.next()) out.add(map(r));
                return out;
            }
        }
    }

    public Optional<Campsite> find(String id) throws SQLException {
        try (Connection c = db.open()) {
            return find(c, id);
        }
    }

    public Optional<Campsite> find(Connection c, String id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT * FROM campsites WHERE id=?")) {
            s.setString(1, id);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? Optional.of(map(r)) : Optional.empty();
            }
        }
    }

    public void save(Campsite site, boolean create) throws SQLException {
        String sql =
                create
                        ? "INSERT INTO campsites(name,province,description,rate_cents,active,id)"
                                + " VALUES(?,?,?,?,?,?)"
                        : "UPDATE campsites SET"
                              + " name=?,province=?,description=?,rate_cents=?,active=? WHERE id=?";
        try (Connection c = db.open();
                PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, site.name());
            s.setString(2, site.province());
            s.setString(3, site.description());
            s.setLong(4, site.rateCents());
            s.setBoolean(5, site.active());
            s.setString(6, site.id());
            if (s.executeUpdate() != 1) throw new SQLException("not_found");
        }
    }

    public void remove(String id) throws SQLException {
        // Referenced sites are archived, preserving historical invoices and reservations.
        try (Connection c = db.open();
                PreparedStatement s =
                        c.prepareStatement("UPDATE campsites SET active=0 WHERE id=?")) {
            s.setString(1, id);
            if (s.executeUpdate() != 1) throw new SQLException("not_found");
        }
    }

    private static Campsite map(ResultSet r) throws SQLException {
        return new Campsite(
                r.getString("id"),
                r.getString("name"),
                r.getString("province"),
                r.getString("description"),
                r.getLong("rate_cents"),
                r.getBoolean("active"));
    }
}
