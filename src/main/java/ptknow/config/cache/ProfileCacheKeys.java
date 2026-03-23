package ptknow.config.cache;

import org.springframework.data.domain.Pageable;

public final class ProfileCacheKeys {

    private ProfileCacheKeys() {
    }

    public static String byHandle(String handle) {
        return handle == null ? "" : handle.trim();
    }

    public static String search(Pageable pageable, String q) {
        return "page=" + pageable.getPageNumber()
                + "|size=" + pageable.getPageSize()
                + "|sort=" + pageable.getSort()
                + "|q=" + normalize(q);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
