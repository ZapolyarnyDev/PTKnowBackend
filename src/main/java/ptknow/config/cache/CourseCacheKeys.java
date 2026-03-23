package ptknow.config.cache;

import org.springframework.data.domain.Pageable;
import ptknow.model.course.CourseState;

public final class CourseCacheKeys {

    private CourseCacheKeys() {
    }

    public static String byHandle(String handle) {
        return handle == null ? "" : handle.trim();
    }

    public static String publicList(Pageable pageable, String q, CourseState state, String tag) {
        return "page=" + pageable.getPageNumber()
                + "|size=" + pageable.getPageSize()
                + "|sort=" + pageable.getSort()
                + "|q=" + normalize(q)
                + "|state=" + (state == null ? "" : state.name())
                + "|tag=" + normalize(tag);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
