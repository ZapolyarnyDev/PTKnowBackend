package ptknow.mapper.lesson;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ptknow.mapper.ApiViewMapper;
import ptknow.dto.lesson.LessonDTO;
import ptknow.model.auth.Auth;
import ptknow.model.auth.Role;
import ptknow.model.course.Course;
import ptknow.model.course.CourseState;
import ptknow.model.lesson.Lesson;
import ptknow.model.lesson.LessonType;
import ptknow.service.file.FileAttachmentService;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LessonMapperTest {

    private final FileAttachmentService fileAttachmentService = mock(FileAttachmentService.class);
    private final LessonMapper lessonMapper = new LessonMapper(new ApiViewMapper(), fileAttachmentService);

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
        when(fileAttachmentService.findAllByResource(ptknow.model.file.attachment.resource.ResourceType.LESSON, "15"))
                .thenReturn(Set.of());

        LessonDTO dto = lessonMapper.toDTO(lesson);

        assertEquals(lesson.getId(), dto.id());
        assertEquals(lesson.getContentMd(), dto.contentMd());
        assertEquals(course.getId(), dto.courseId());
        assertEquals(course.getHandle(), dto.course().handle());
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
