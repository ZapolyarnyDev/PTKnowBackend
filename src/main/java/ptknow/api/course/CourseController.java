package ptknow.api.course;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import ptknow.dto.course.CourseDTO;
import ptknow.dto.course.CreateCourseDTO;
import ptknow.dto.enrollment.EnrollmentDTO;
import ptknow.mapper.enrollment.EnrollmentMapper;
import ptknow.model.auth.Auth;
import ptknow.model.course.Course;
import ptknow.mapper.course.CourseMapper;
import ptknow.model.enrollment.Enrollment;
import ptknow.service.course.CourseService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ptknow.service.enrollment.EnrollmentService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v0/course")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseController {

    CourseService courseService;
    EnrollmentService enrollmentService;
    CourseMapper courseMapper;
    EnrollmentMapper enrollmentMapper;

    @GetMapping
    public ResponseEntity<List<CourseDTO>> get() {
        List<CourseDTO> courseDTOS = courseService.findAllCourses().stream()
                .map(courseMapper::courseToDTO)
                .toList();
        return ResponseEntity.ok(courseDTOS);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<CourseDTO> createCourse(
            @Valid @RequestPart("course") CreateCourseDTO dto,
            @RequestPart(value = "preview", required = false) MultipartFile preview,
            @AuthenticationPrincipal Auth entity
            ) throws IOException {
        Course course = courseService.publishCourse(dto, entity, preview);

        return ResponseEntity.ok(courseMapper.courseToDTO(course));
    }

    @GetMapping("/id/{id}")
    @PreAuthorize("hasAnyRole('GUEST', 'STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<CourseDTO> getCourse(
            @PathVariable Long id,
            @AuthenticationPrincipal Auth auth
    ) {
        CourseDTO course = courseMapper.courseToDTO(courseService.seeById(id, auth));
        return ResponseEntity.ok(course);
    }

    @GetMapping("/handle/{handle}")
    @PreAuthorize("hasAnyRole('GUEST', 'STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<CourseDTO> getCourse(
            @PathVariable String handle,
            @AuthenticationPrincipal Auth auth
    ) {
        CourseDTO course = courseMapper.courseToDTO(courseService.seeByHandle(handle, auth));
        return ResponseEntity.ok(course);
    }

    @PostMapping("/{id}/preview")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<CourseDTO> updatePreview(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Auth entity
    ) throws IOException {
        var updatedProfile = courseService.updatePreview(id, entity, file);
        var dto = courseMapper.courseToDTO(updatedProfile);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable Long id,
            @AuthenticationPrincipal Auth entity
            ) throws IOException {
        courseService.deleteCourseById(id, entity);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/{id}/editors/{userId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> addCourseEditor(
            @PathVariable Long id,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Auth entity
            ) {
        courseService.addEditor(id, entity, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/editors/{userId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> removeCourseEditor(
            @PathVariable Long id,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Auth entity
    ) {
        courseService.removeEditor(id, entity, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/enroll")
    @PreAuthorize("hasAnyRole('GUEST', 'STUDENT')")
    public ResponseEntity<Void> enroll(
            @PathVariable Long id,
            @AuthenticationPrincipal Auth entity
    ) {
        enrollmentService.enroll(entity, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/enroll")
    @PreAuthorize("hasAnyRole('GUEST', 'STUDENT')")
    public ResponseEntity<Void> unEnroll(
            @PathVariable Long id,
            @AuthenticationPrincipal Auth entity
    ) {
        enrollmentService.unenroll(entity, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('GUEST', 'STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<List<EnrollmentDTO>> getMembers (
            @PathVariable Long id,
            @AuthenticationPrincipal Auth entity
    ) {
        List<Enrollment> enrollments = enrollmentService.findAllByCourse(entity, id);
        return ResponseEntity.ok(
                enrollmentMapper.mapEntityList(enrollments)
        );
    }
}

