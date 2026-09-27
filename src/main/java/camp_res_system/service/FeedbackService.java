package camp_res_system.service;

import camp_res_system.model.Feedback;
import camp_res_system.repository.FeedbackRepository;

import java.sql.SQLException;
import java.util.List;

public final class FeedbackService {
    private final FeedbackRepository feedback;
    private final Session session;

    public FeedbackService(FeedbackRepository feedback, Session session) {
        this.feedback = feedback;
        this.session = session;
    }

    public void submit(String message) {
        long user = session.requireUser().id();
        message = message.trim();
        if (message.isEmpty() || message.length() > 2000) throw new AppException("error.feedback");
        try {
            feedback.add(user, message);
        } catch (SQLException e) {
            throw new AppException("error.database", e);
        }
    }

    public List<Feedback> list() {
        session.requireAdmin();
        try {
            return feedback.list();
        } catch (SQLException e) {
            throw new AppException("error.database", e);
        }
    }
}
