package camp_res_system.model;

public record User(long id, String fullName, String email, String username, Role role) {
    public enum Role {
        ADMIN,
        USER
    }
}
