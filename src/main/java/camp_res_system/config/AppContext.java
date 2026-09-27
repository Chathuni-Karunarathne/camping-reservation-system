package camp_res_system.config;

import camp_res_system.repository.*;
import camp_res_system.service.*;

import java.time.Clock;

public final class AppContext {
    public final Session session = new Session();
    public final AuthService auth;
    public final CampsiteService campsites;
    public final ReservationService reservations;
    public final FeedbackService feedback;

    public AppContext(Database db) {
        var sites = new CampsiteRepository(db);
        auth = new AuthService(new UserRepository(db), session);
        campsites = new CampsiteService(sites, session);
        reservations =
                new ReservationService(
                        db,
                        sites,
                        new ReservationRepository(db),
                        session,
                        Clock.systemDefaultZone());
        feedback = new FeedbackService(new FeedbackRepository(db), session);
    }
}
