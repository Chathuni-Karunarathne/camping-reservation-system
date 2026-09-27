package camp_res_system.config;

import java.io.IOException;
import java.nio.file.*;
import java.util.logging.*;

public final class Logging {
    private Logging() {}

    public static void configure(Path database) {
        try {
            Path logs = database.getParent().resolve("logs");
            Files.createDirectories(logs);
            FileHandler handler =
                    new FileHandler(
                            logs.resolve("tenttrack-%g.log").toString(), 1_000_000, 3, true);
            handler.setFormatter(new SimpleFormatter());
            Logger.getLogger("").addHandler(handler);
        } catch (IOException | SecurityException e) {
            Logger.getLogger(Logging.class.getName())
                    .log(Level.WARNING, "File logging unavailable; using console logging", e);
        }
    }
}
