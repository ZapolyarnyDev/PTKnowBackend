package ptknow.service.course;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import ptknow.model.auth.Auth;
import ptknow.model.course.Course;
import ptknow.model.course.CourseState;

import java.util.Locale;

public final class CourseSpecifications {

    private CourseSpecifications() {
    }

    public static Specification<Course> visibleTo(Auth viewer) {
        return (root, query, cb) -> {
            query.distinct(true);

            if (viewer == null) {
                return cb.equal(root.get("state"), CourseState.PUBLISHED);
            }

            if ("ADMIN".equals(viewer.getRole().name())) {
                return cb.conjunction();
            }

            var ownerPredicate = cb.equal(root.get("owner").get("id"), viewer.getId());
            var publishedPredicate = cb.equal(root.get("state"), CourseState.PUBLISHED);
            var editorsJoin = root.join("editors", JoinType.LEFT);
            var editorPredicate = cb.equal(editorsJoin.get("id"), viewer.getId());
            var enrollmentsJoin = root.join("enrollments", JoinType.LEFT);
            var enrolledPredicate = cb.equal(enrollmentsJoin.get("user").get("id"), viewer.getId());

            return cb.or(publishedPredicate, ownerPredicate, editorPredicate, enrolledPredicate);
        };
    }

    public static Specification<Course> hasState(CourseState state) {
        return (root, query, cb) -> state == null
                ? cb.conjunction()
                : cb.equal(root.get("state"), state);
    }

    public static Specification<Course> hasTag(String tag) {
        return (root, query, cb) -> {
            if (tag == null || tag.isBlank()) {
                return cb.conjunction();
            }

            query.distinct(true);
            var tagJoin = root.join("courseTags", JoinType.LEFT);
            return cb.equal(cb.lower(tagJoin.get("tagName")), tag.toLowerCase(Locale.ROOT));
        };
    }

    public static Specification<Course> search(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) {
                return cb.conjunction();
            }

            String normalized = q.trim().toLowerCase(Locale.ROOT);

            if (normalized.startsWith("@")) {
                String handleQuery = normalized.substring(1).trim();
                if (handleQuery.isBlank()) {
                    return cb.conjunction();
                }

                return cb.like(cb.lower(root.get("handle")), handleQuery + "%");
            }

            String pattern = "%" + normalized + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.like(cb.lower(root.get("handle")), pattern)
            );
        };
    }
}
