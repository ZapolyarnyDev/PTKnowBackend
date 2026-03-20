package ptknow.service.course;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ptknow.exception.course.NotAllowedToSeeCourseInfoException;
import ptknow.model.auth.Auth;
import ptknow.model.auth.Role;
import ptknow.model.course.Course;
import ptknow.model.course.CourseState;
import ptknow.repository.course.CourseRepository;
import ptknow.repository.enrollment.EnrollmentRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseAccessServiceTest {

    @Mock
    CourseRepository courseRepository;

    @Mock
    EnrollmentRepository enrollmentRepository;

    @InjectMocks
    CourseAccessService courseAccessService;

    @Test
    void canSeeShouldAllowAnonymousForPublishedCourse() {
        Course course = course(1L, auth(Role.TEACHER), CourseState.PUBLISHED);

        assertTrue(courseAccessService.canSee(course, null));
    }

    @Test
    void canSeeShouldDenyAnonymousForDraftCourse() {
        Course course = course(1L, auth(Role.TEACHER), CourseState.DRAFT);

        assertFalse(courseAccessService.canSee(course, null));
    }

    @Test
    void canSeeShouldAllowManagerRegardlessOfState() {
        Auth owner = auth(Role.TEACHER);
        Auth editor = auth(Role.TEACHER);
        Course course = course(1L, owner, CourseState.ARCHIVED);
        course.addEditor(editor);

        assertTrue(courseAccessService.canSee(course, owner));
        assertTrue(courseAccessService.canSee(course, editor));
        assertTrue(courseAccessService.canSee(course, auth(Role.ADMIN)));
    }

    @Test
    void canSeeShouldAllowEnrolledUserForPublishedCourse() {
        Auth owner = auth(Role.TEACHER);
        Auth student = auth(Role.STUDENT);
        Course course = course(1L, owner, CourseState.PUBLISHED);

        when(enrollmentRepository.existsByUser_IdAndCourse_Id(student.getId(), course.getId())).thenReturn(true);

        assertTrue(courseAccessService.canSee(course, student));
    }

    @Test
    void canSeeShouldDenyNotEnrolledUserForPublishedCourse() {
        Auth owner = auth(Role.TEACHER);
        Auth guest = auth(Role.GUEST);
        Course course = course(1L, owner, CourseState.PUBLISHED);

        when(enrollmentRepository.existsByUser_IdAndCourse_Id(guest.getId(), course.getId())).thenReturn(false);

        assertFalse(courseAccessService.canSee(course, guest));
    }

    @Test
    void accessShouldThrowWhenUserCannotSeeCourse() {
        Auth owner = auth(Role.TEACHER);
        Auth guest = auth(Role.GUEST);
        Course course = course(1L, owner, CourseState.DRAFT);

        assertThrows(NotAllowedToSeeCourseInfoException.class, () -> courseAccessService.access(course, guest));
    }

    @Test
    void accessByIdShouldLoadCourseAndReturnItWhenAllowed() {
        Auth owner = auth(Role.TEACHER);
        Auth admin = auth(Role.ADMIN);
        Course course = course(1L, owner, CourseState.DRAFT);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        Course result = courseAccessService.access(1L, admin);

        assertSame(course, result);
        verify(courseRepository).findById(1L);
    }

    private Auth auth(Role role) {
        Auth auth = Auth.builder()
                .email(UUID.randomUUID() + "@test.local")
                .password("password")
                .role(role)
                .build();
        ReflectionTestUtils.setField(auth, "id", UUID.randomUUID());
        return auth;
    }

    private Course course(Long id, Auth owner, CourseState state) {
        Course course = Course.builder()
                .name("course-" + id)
                .description("desc")
                .handle("course-" + id)
                .owner(owner)
                .maxUsersAmount(10)
                .state(state)
                .build();
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }
}
