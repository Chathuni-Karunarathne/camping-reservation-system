package camp_res_system.service;

/** Localizable expected error; technical causes are logged by the UI. */
public final class AppException extends RuntimeException {
    private final String key;

    public AppException(String key) {
        super(key);
        this.key = key;
    }

    public AppException(String key, Throwable cause) {
        super(key, cause);
        this.key = key;
    }

    public String key() {
        return key;
    }
}
