package camp_res_system.service;

import camp_res_system.model.User;

public final class Session {
    private volatile User user;

    public User current() {
        return user;
    }

    void signIn(User user) {
        this.user = user;
    }

    public void logout() {
        user = null;
    }

    public User requireUser() {
        User current = user;
        if (current == null) throw new AppException("error.loginRequired");
        return current;
    }

    public User requireAdmin() {
        User current = requireUser();
        if (current.role() != User.Role.ADMIN) throw new AppException("error.forbidden");
        return current;
    }
}
