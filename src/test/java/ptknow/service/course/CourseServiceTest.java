package ptknow.service.course;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ptknow.dto.course.CreateCourseDTO;
import ptknow.generator.handle.HandleGenerator;
import ptknow.model.auth.Auth;
import ptknow.model.auth.Role;
import ptknow.model.course.Course;
import ptknow.model.course.CourseState;
import ptknow.model.course.CourseTag;
import ptknow.repository.auth.AuthRepository;
import ptknow.repository.course.CourseRepository;
import ptknow.repository.course.CourseTagRepository;
import ptknow.repository.enrollment.EnrollmentRepository;
import ptknow.repository.lesson.LessonRepository;
import ptknow.service.file.FileAttachmentService;
import ptknow.service.file.FileService;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    AuthRepository authRepository;

    @Mock
    CourseRepository repository;

    @Mock
    CourseTagRepository courseTagRepository;

    @Mock
    LessonRepository lessonRepository;

    @Mock
    EnrollmentRepository enrollmentRepository;

    @Mock
    HandleGenerator handleGenerator;

    @Mock
    FileService fileService;

    @Mock
    FileAttachmentService fileAttachmentService;

    @Mock
    CourseAccessService accessService;

    @Mock
    CourseCacheService courseCacheService;

    @InjectMocks
    CourseService courseService;

    @Test
    void publishCourseShouldSaveCourseWithoutTouchingDetachedOwnerCollections() throws IOException {
        Auth initiator = spy(Auth.builder()
                .email("teacher@example.com")
                .password("password")
                .role(Role.TEACHER)
                .build());

        CreateCourseDTO dto = new CreateCourseDTO(
                "Java Backend Basics",
                "Spring course",
                Set.of("java")
        );

        CourseTag tag = new CourseTag("java");

        when(repository.existsByName(dto.name())).thenReturn(false);
        when(handleGenerator.generate(any())).thenReturn("java-backend-basics");
        when(courseTagRepository.findByTagName("java")).thenReturn(Optional.of(tag));

        Course result = courseService.publishCourse(dto, initiator, null);

        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        verify(repository).save(courseCaptor.capture());
        verify(initiator, never()).addOwnedCourse(any());

        Course savedCourse = courseCaptor.getValue();
        assertSame(initiator, savedCourse.getOwner());
        assertEquals("java-backend-basics", savedCourse.getHandle());
        assertEquals(CourseState.DRAFT, savedCourse.getState());
        assertTrue(savedCourse.getCourseTags().contains(tag));
        assertSame(savedCourse, result);
    }
}
