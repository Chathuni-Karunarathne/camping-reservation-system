package camp_res_system.repository;

import camp_res_system.config.Database;
import camp_res_system.model.User;

import java.sql.*;
import java.util.Optional;

public final class UserRepository {
    public record Credentials(User user, String hash) {}

    private final Database db;

    public UserRepository(Database db) {
        this.db = db;
    }

    public boolean hasAdmin() throws SQLException {
        try (Connection c = db.open();
                PreparedStatement s =
                        c.prepareStatement("SELECT 1 FROM users WHERE role='ADMIN' LIMIT 1");
                ResultSet r = s.executeQuery()) {
            return r.next();
        }
    }

    public Optional<Credentials> find(String username) throws SQLException {
        try (Connection c = db.open();
                PreparedStatement s = c.prepareStatement("SELECT * FROM users WHERE username=?")) {
            s.setString(1, username);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) return Optional.empty();
                return Optional.of(
                        new Credentials(
                                new User(
                                        r.getLong("id"),
                                        r.getString("full_name"),
                                        r.getString("email"),
                                        r.getString("username"),
                                        User.Role.valueOf(r.getString("role"))),
                                r.getString("password_hash")));
            }
        }
    }

    public User create(String name, String email, String username, String hash, User.Role role)
            throws SQLException {
        // First-admin setup is conditional in the same atomic statement, including across app
        // instances.
        String sql =
                "INSERT INTO users(full_name,email,username,password_hash,role) SELECT ?,?,?,?,?"
                        + (role == User.Role.ADMIN
                                ? " WHERE NOT EXISTS(SELECT 1 FROM users WHERE role='ADMIN')"
                                : "");
        try (Connection c = db.open();
                PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, name);
            s.setString(2, email);
            s.setString(3, username);
            s.setString(4, hash);
            s.setString(5, role.name());
            if (s.executeUpdate() == 0) throw new SQLException("setup_complete");
            try (PreparedStatement id = c.prepareStatement("SELECT last_insert_rowid()");
                    ResultSet r = id.executeQuery()) {
                r.next();
                return new User(r.getLong(1), name, email, username, role);
            }
        }
    }
}
