package camp_res_system.repository;

import camp_res_system.config.Database;
import camp_res_system.model.Feedback;

import java.sql.*;
import java.util.*;

public final class FeedbackRepository {
    private final Database db;

    public FeedbackRepository(Database db) {
        this.db = db;
    }

    public void add(long user, String message) throws SQLException {
        try (Connection c = db.open();
                PreparedStatement s =
                        c.prepareStatement("INSERT INTO feedback(user_id,message) VALUES(?,?)")) {
            s.setLong(1, user);
            s.setString(2, message);
            s.executeUpdate();
        }
    }

    public List<Feedback> list() throws SQLException {
        try (Connection c = db.open();
                PreparedStatement s =
                        c.prepareStatement(
                                "SELECT f.*,u.username FROM feedback f JOIN users u ON"
                                        + " u.id=f.user_id ORDER BY f.id DESC");
                ResultSet r = s.executeQuery()) {
            List<Feedback> out = new ArrayList<>();
            while (r.next())
                out.add(
                        new Feedback(
                                r.getLong("id"),
                                r.getString("username"),
                                r.getString("message"),
                                r.getString("created_at")));
            return out;
        }
    }
}
