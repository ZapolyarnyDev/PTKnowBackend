package ptknow.mapper.lesson;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ptknow.dto.lesson.LessonDTO;
import ptknow.model.auth.Auth;
import ptknow.model.auth.Role;
import ptknow.model.course.Course;
import ptknow.model.course.CourseState;
import ptknow.model.lesson.Lesson;
import ptknow.model.lesson.LessonType;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LessonMapperTest {

    private final LessonMapper lessonMapper = new LessonMapperImpl();

    @Test
    void toDtoShouldMapMarkdownContent() {
        Course course = course(11L);
        Lesson lesson = Lesson.builder()
                .name("lesson")
                .description("desc")
                .contentMd("# markdown")
                .beginAt(Instant.now())
                .endsAt(Instant.now().plusSeconds(3600))
                .course(course)
                .lessonType(LessonType.LECTURE)
                .owner(auth())
                .build();
        ReflectionTestUtils.setField(lesson, "id", 15L);

        LessonDTO dto = lessonMapper.toDTO(lesson);

        assertEquals(lesson.getId(), dto.id());
        assertEquals(lesson.getContentMd(), dto.contentMd());
        assertEquals(course.getId(), dto.courseId());
    }

    private Course course(Long id) {
        Course course = Course.builder()
                .name("course-" + id)
                .description("desc")
                .handle("course-" + id)
                .owner(auth())
                .state(CourseState.DRAFT)
                .maxUsersAmount(10)
                .build();
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }

    private Auth auth() {
        Auth auth = Auth.builder()
                .email(UUID.randomUUID() + "@test.local")
                .password("password")
                .role(Role.TEACHER)
                .build();
        ReflectionTestUtils.setField(auth, "id", UUID.randomUUID());
        return auth;
    }
}
