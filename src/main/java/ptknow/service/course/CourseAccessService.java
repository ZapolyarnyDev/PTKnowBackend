package ptknow.service.course;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptknow.exception.course.CourseNotFoundException;
import ptknow.exception.course.NotAllowedToSeeCourseInfoException;
import ptknow.model.auth.Auth;
import ptknow.model.auth.Role;
import ptknow.model.course.Course;
import ptknow.model.course.CourseState;
import ptknow.repository.course.CourseRepository;
import ptknow.repository.enrollment.EnrollmentRepository;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseAccessService {

    CourseRepository courseRepository;
    EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public Course access(Long courseId, Auth auth) {
        var course = findCourseById(courseId);
        return access(course, auth);
    }


    @Transactional(readOnly = true)
    public Course access(String courseHandle, Auth auth) {
        var course = findCourseByHandle(courseHandle);
        return access(course, auth);
    }

    @Transactional(readOnly = true)
    public Course access(Course course, Auth auth) {
        if(canSee(course, auth))
            return course;
        throw new NotAllowedToSeeCourseInfoException(auth.getId());
    }

    public boolean canSee(Long courseId, Auth auth) {
        return canSee(findCourseById(courseId), auth);
    }

    public boolean canSee(Course course, Auth auth) {
        if (auth == null)
            return course.getState() == CourseState.PUBLISHED;

        if (canManage(course, auth))
            return true;

        if (course.getState() != CourseState.PUBLISHED)
            return false;

        return enrollmentRepository.existsByUser_IdAndCourse_Id(auth.getId(), course.getId());
    }

    public boolean canManage(Course course, Auth auth) {
        if (auth == null)
            return false;

        return auth.getRole() == Role.ADMIN ||
                course.getOwner().equals(auth) ||
                course.hasEditor(auth);
    }

    private Course findCourseByHandle(String handle) {
        return courseRepository.findByHandle(handle)
                .orElseThrow(() -> new CourseNotFoundException(handle));
    }

    private Course findCourseById(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
    }
}
