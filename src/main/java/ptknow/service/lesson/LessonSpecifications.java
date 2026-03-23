package ptknow.service.lesson;

import org.springframework.data.jpa.domain.Specification;
import ptknow.model.lesson.Lesson;
import ptknow.model.lesson.LessonState;
import ptknow.model.lesson.LessonType;

import java.time.Instant;
import java.util.Locale;

public final class LessonSpecifications {

    private LessonSpecifications() {
    }

    public static Specification<Lesson> byCourseId(Long courseId) {
        return (root, _, cb) -> cb.equal(root.get("course").get("id"), courseId);
    }

    public static Specification<Lesson> hasState(LessonState state) {
        return (root, _, cb) -> state == null ? cb.conjunction() : cb.equal(root.get("state"), state);
    }

    public static Specification<Lesson> hasType(LessonType type) {
        return (root, _, cb) -> type == null ? cb.conjunction() : cb.equal(root.get("type"), type);
    }

    public static Specification<Lesson> beginsFrom(Instant from) {
        return (root, _, cb) -> from == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("beginAt"), from);
    }

    public static Specification<Lesson> beginsTo(Instant to) {
        return (root, _, cb) -> to == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("beginAt"), to);
    }

    public static Specification<Lesson> search(String q) {
        return (root, _, cb) -> {
            if (q == null || q.isBlank())
                return cb.conjunction();

            String pattern = "%" + q.toLowerCase(Locale.ROOT) + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }
}
