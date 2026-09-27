package camp_res_system.service;

import camp_res_system.model.User;
import camp_res_system.repository.UserRepository;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;

public final class AuthService {
    private final UserRepository users;
    private final Session session;
    private final String dummyHash = Passwords.hash("timing-only-not-an-account".toCharArray());

    public AuthService(UserRepository users, Session session) {
        this.users = users;
        this.session = session;
    }

    public boolean needsSetup() {
        try {
            return !users.hasAdmin();
        } catch (SQLException e) {
            throw new AppException("error.database", e);
        }
    }

    public User login(String username, char[] password) {
        session.logout();
        try {
            var credentials = users.find(username.trim());
            boolean valid =
                    Passwords.verify(
                            password,
                            credentials.map(UserRepository.Credentials::hash).orElse(dummyHash));
            if (!valid || credentials.isEmpty()) throw new AppException("error.credentials");
            User user = credentials.get().user();
            session.signIn(user);
            return user;
        } catch (SQLException e) {
            throw new AppException("error.database", e);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    public User register(
            String name, String email, String username, char[] password, char[] confirmation) {
        return create(name, email, username, password, confirmation, User.Role.USER);
    }

    public User setupAdmin(
            String name, String email, String username, char[] password, char[] confirmation) {
        return create(name, email, username, password, confirmation, User.Role.ADMIN);
    }

    private User create(
            String name,
            String email,
            String username,
            char[] password,
            char[] confirmation,
            User.Role role) {
        try {
            name = name.trim();
            email = email.trim().toLowerCase(Locale.ROOT);
            username = username.trim().toLowerCase(Locale.ROOT);
            if (name.isEmpty()
                    || name.length() > 100
                    || !username.matches("[a-z0-9_.-]{3,40}")
                    || email.length() > 254
                    || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
                throw new AppException("error.account");
            if (password.length < 10 || password.length > 128)
                throw new AppException("error.password");
            if (!Arrays.equals(password, confirmation))
                throw new AppException("error.confirmPassword");
            User user = users.create(name, email, username, Passwords.hash(password), role);
            session.signIn(user);
            return user;
        } catch (SQLException e) {
            if (e.getMessage().contains("setup_complete"))
                throw new AppException("error.setupComplete");
            if (e.getMessage().contains("UNIQUE")) throw new AppException("error.duplicateAccount");
            throw new AppException("error.database", e);
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
        }
    }
}
