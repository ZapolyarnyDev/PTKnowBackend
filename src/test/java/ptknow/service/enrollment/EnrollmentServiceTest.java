package ptknow.service.enrollment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import ptknow.exception.course.CourseIsFullException;
import ptknow.exception.enrollment.AlreadyEnrolledException;
import ptknow.exception.enrollment.NotAllowedToSeeCourseMembersException;
import ptknow.exception.enrollment.UserNotEnrollableException;
import ptknow.model.auth.Auth;
import ptknow.model.auth.Role;
import ptknow.model.course.Course;
import ptknow.model.course.CourseState;
import ptknow.model.enrollment.Enrollment;
import ptknow.repository.enrollment.EnrollmentRepository;
import ptknow.service.course.CourseCacheService;
import ptknow.service.course.CourseService;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    EnrollmentRepository repository;

    @Mock
    CourseService courseService;

    @Mock
    CourseCacheService courseCacheService;

    @InjectMocks
    EnrollmentService enrollmentService;

    @Test
    void enrollShouldSaveEnrollmentForGuestUser() {
        Auth initiator = auth(Role.GUEST);
        Course course = course(1L, auth(Role.TEACHER), 10);

        when(repository.existsByUser_IdAndCourse_Id(initiator.getId(), course.getId())).thenReturn(false);
        when(repository.countByCourse_Id(course.getId())).thenReturn(0);
        when(courseService.findCourseByIdForUpdate(course.getId())).thenReturn(course);
        when(repository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Enrollment result = enrollmentService.enroll(initiator, course.getId());

        assertNotNull(result);
        assertSame(initiator, result.getUser());
        assertSame(course, result.getCourse());
        assertNotNull(result.getEnrollSince());
        assertEquals(1, initiator.getEnrollments().size());
        assertEquals(1, course.getEnrollments().size());
        verify(repository).save(any(Enrollment.class));
    }

    @Test
    void enrollShouldRejectNotEnrollableUser() {
        Auth initiator = auth(Role.TEACHER);

        assertThrows(UserNotEnrollableException.class, () -> enrollmentService.enroll(initiator, 1L));

        verify(courseService, never()).findCourseByIdForUpdate(any());
        verify(repository, never()).save(any());
    }

    @Test
    void enrollShouldRejectAlreadyEnrolledUser() {
        Auth initiator = auth(Role.STUDENT);

        when(repository.existsByUser_IdAndCourse_Id(initiator.getId(), 1L)).thenReturn(true);

        assertThrows(AlreadyEnrolledException.class, () -> enrollmentService.enroll(initiator, 1L));

        verify(courseService, never()).findCourseByIdForUpdate(any());
        verify(repository, never()).save(any());
    }

    @Test
    void enrollShouldRejectWhenCourseIsFull() {
        Auth initiator = auth(Role.STUDENT);
        Course course = course(1L, auth(Role.TEACHER), 2);

        when(repository.existsByUser_IdAndCourse_Id(initiator.getId(), course.getId())).thenReturn(false);
        when(courseService.findCourseByIdForUpdate(course.getId())).thenReturn(course);
        when(repository.countByCourse_Id(course.getId())).thenReturn(2);

        assertThrows(CourseIsFullException.class, () -> enrollmentService.enroll(initiator, course.getId()));

        verify(repository, never()).save(any());
    }

    @Test
    void enrollShouldMapDataIntegrityViolationToAlreadyEnrolledWhenDuplicateDetected() {
        Auth initiator = auth(Role.STUDENT);
        Course course = course(1L, auth(Role.TEACHER), 10);

        when(repository.existsByUser_IdAndCourse_Id(initiator.getId(), course.getId())).thenReturn(false, true);
        when(courseService.findCourseByIdForUpdate(course.getId())).thenReturn(course);
        when(repository.countByCourse_Id(course.getId())).thenReturn(0);
        when(repository.save(any(Enrollment.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(AlreadyEnrolledException.class, () -> enrollmentService.enroll(initiator, course.getId()));
    }

    @Test
    void findAllByCourseShouldReturnMembersForAdmin() {
        Auth admin = auth(Role.ADMIN);
        Auth owner = auth(Role.TEACHER);
        Course course = course(1L, owner, 10);
        List<Enrollment> enrollments = List.of(enrollment(auth(Role.STUDENT), course));

        when(courseService.findCourseById(course.getId())).thenReturn(course);
        when(repository.findAllByCourse_Id(course.getId())).thenReturn(enrollments);

        List<Enrollment> result = enrollmentService.findAllByCourse(admin, course.getId());

        assertEquals(enrollments, result);
    }

    @Test
    void findAllByCourseShouldReturnMembersForEditor() {
        Auth owner = auth(Role.TEACHER);
        Auth editor = auth(Role.TEACHER);
        Course course = course(1L, owner, 10);
        course.addEditor(editor);
        List<Enrollment> enrollments = List.of(enrollment(auth(Role.STUDENT), course));

        when(courseService.findCourseById(course.getId())).thenReturn(course);
        when(repository.findAllByCourse_Id(course.getId())).thenReturn(enrollments);

        List<Enrollment> result = enrollmentService.findAllByCourse(editor, course.getId());

        assertEquals(enrollments, result);
    }

    @Test
    void findAllByCourseShouldRejectUserWithoutAccess() {
        Auth outsider = auth(Role.GUEST);
        Auth owner = auth(Role.TEACHER);
        Course course = course(1L, owner, 10);

        when(courseService.findCourseById(course.getId())).thenReturn(course);
        when(repository.existsByUser_IdAndCourse_Id(outsider.getId(), course.getId())).thenReturn(false);

        assertThrows(NotAllowedToSeeCourseMembersException.class,
                () -> enrollmentService.findAllByCourse(outsider, course.getId()));

        verify(repository, never()).findAllByCourse_Id(any());
    }

    @Test
    void unenrollShouldDelegateToRepository() {
        Auth user = auth(Role.STUDENT);

        enrollmentService.unenroll(user, 5L);

        verify(repository).deleteByUser_IdAndCourse_Id(user.getId(), 5L);
    }

    @Test
    void findAllByUserShouldDelegateToRepository() {
        UUID userId = UUID.randomUUID();
        List<Enrollment> enrollments = List.of();

        when(repository.findAllByUser_Id(userId)).thenReturn(enrollments);

        List<Enrollment> result = enrollmentService.findAllByUser(userId);

        assertSame(enrollments, result);
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

    private Course course(Long id, Auth owner, int maxUsers) {
        Course course = Course.builder()
                .name("course-" + id)
                .description("desc")
                .handle("course-" + id)
                .owner(owner)
                .state(CourseState.DRAFT)
                .maxUsersAmount(maxUsers)
                .build();
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }

    private Enrollment enrollment(Auth user, Course course) {
        Enrollment enrollment = Enrollment.builder()
                .user(user)
                .course(course)
                .enrollSince(Instant.now())
                .build();
        ReflectionTestUtils.setField(enrollment, "id", 1L);
        return enrollment;
    }
}
