package ptknow.config.cache;

public final class CourseCacheKeys {

    private CourseCacheKeys() {
    }

    public static String byHandle(String handle) {
        return handle == null ? "" : handle.trim();
    }
}
