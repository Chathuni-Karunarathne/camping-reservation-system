package camp_res_system.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

/** One connection per operation; foreign keys are enabled on every connection. */
public final class Database {
    private final Path path;

    public Database(Path path) {
        this.path = path.toAbsolutePath().normalize();
    }

    public static Database configured() {
        String configured = System.getProperty("tenttrack.db", System.getenv("TENTTRACK_DB"));
        return new Database(
                Path.of(
                        configured == null || configured.isBlank()
                                ? "data/tenttrack.db"
                                : configured));
    }

    public Path path() {
        return path;
    }

    public Connection open() throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys=ON");
            s.execute("PRAGMA busy_timeout=5000");
        } catch (SQLException e) {
            c.close();
            throw e;
        }
        return c;
    }

    public void initialize() throws IOException, SQLException {
        Files.createDirectories(path.getParent());
        String schema;
        try (var stream = Database.class.getResourceAsStream("/db/schema.sql")) {
            if (stream == null) throw new IOException("Missing schema resource");
            schema = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (Connection c = open();
                Statement s = c.createStatement()) {
            try (ResultSet version = s.executeQuery("PRAGMA user_version")) {
                if (version.next() && version.getInt(1) > 1)
                    throw new SQLException("Database version is newer than this application");
            }
            s.execute("PRAGMA journal_mode=WAL");
            c.setAutoCommit(false);
            try {
                String[] sections = schema.split("-- @statement");
                for (String sql : sections[0].split(";")) if (!sql.isBlank()) s.execute(sql);
                for (int i = 1; i < sections.length; i++) s.execute(sections[i]);
                s.execute("PRAGMA user_version=1");
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
