package ptknow.service.course;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ptknow.dto.course.CourseTeacherDTO;
import ptknow.dto.course.CreateCourseDTO;
import ptknow.dto.course.UpdateCourseDTO;
import ptknow.model.auth.Auth;
import ptknow.model.auth.Role;
import ptknow.model.course.Course;
import ptknow.model.course.CourseState;
import ptknow.model.course.CourseTag;
import ptknow.model.enrollment.Enrollment;
import ptknow.model.file.File;
import ptknow.model.file.attachment.FileAttachment;
import ptknow.model.file.attachment.resource.ResourceType;
import ptknow.model.lesson.Lesson;
import ptknow.model.file.attachment.FileVisibility;
import ptknow.model.file.attachment.resource.Purpose;
import ptknow.exception.course.*;
import ptknow.exception.file.FileAttachmentNotFoundException;
import ptknow.exception.user.UserNotFoundException;
import ptknow.generator.handle.HandleGenerator;
import ptknow.repository.auth.AuthRepository;
import ptknow.repository.course.CourseRepository;
import ptknow.repository.course.CourseTagRepository;
import ptknow.repository.enrollment.EnrollmentRepository;
import ptknow.repository.lesson.LessonRepository;
import ptknow.service.AccessService;
import ptknow.service.HandleService;
import ptknow.service.OwnershipService;
import ptknow.service.file.FileAttachmentService;
import ptknow.service.file.FileService;

import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class CourseService implements HandleService<Course>, OwnershipService<Long>, AccessService<Long> {

    AuthRepository authRepository;
    CourseRepository repository;
    CourseTagRepository courseTagRepository;
    LessonRepository lessonRepository;
    EnrollmentRepository enrollmentRepository;
    HandleGenerator handleGenerator;
    FileService fileService;
    FileAttachmentService fileAttachmentService;
    CourseAccessService accessService;
    CourseCacheService courseCacheService;

    @Transactional(rollbackFor = Exception.class)
    public Course publishCourse(CreateCourseDTO dto, Auth initiator, MultipartFile preview) throws IOException {
        if (repository.existsByName(dto.name())) {
            throw new CourseAlreadyExists(dto.name());
        }

        String handle = handleGenerator.generate(repository::existsByHandle);

        File previewFile = null;
        if (preview != null && !preview.isEmpty()) {
            previewFile = fileService.saveFile(preview);
        }

        var entity = Course.builder()
                .name(dto.name())
                .description(dto.description())
                .courseTags(courseTagsFromNames(dto.tags()))
                .handle(handle)
                .preview(previewFile)
                .owner(initiator)
                .state(CourseState.DRAFT)
                .build();

        repository.save(entity);

        if (previewFile != null) {
            fileAttachmentService.attach(
                    previewFile,
                    ResourceType.COURSE,
                    entity.getId().toString(),
                    Purpose.PREVIEW,
                    previewVisibilityFor(entity.getState()),
                    entity.getOwner()
            );
        }

        return entity;
    }


    @Transactional
    public Set<CourseTag> courseTagsFromNames(Set<String> names) {
        return names.stream()
                .map(name -> courseTagRepository.findByTagName(name)
                        .orElseGet(() -> createCourseTag(name)))
                .collect(Collectors.toSet());
    }

    @Transactional
    public CourseTag createCourseTag(String name) {
        if (courseTagRepository.existsByTagName(name))
            throw new CourseTagAlreadyExists(name);

        var entity = new CourseTag(name);
        courseTagRepository.save(entity);

        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteCourseById(Long courseId, Auth initiator) throws IOException {
        var course = findCourseById(courseId);

        if(initiator.getRole() != Role.ADMIN && !course.getOwner().equals(initiator))
            throw new CourseNotOwnedByUserException(initiator.getId());

        Set<CourseTag> tags = new HashSet<>(course.getCourseTags());
        Set<Long> lessonIds = course.getLessons().stream()
                .map(Lesson::getId)
                .collect(Collectors.toSet());

        Set<UUID> fileIdsToDelete = new HashSet<>();

        if (course.getPreview() != null) {
            fileIdsToDelete.add(course.getPreview().getId());
            course.setPreview(null);
            repository.save(course);
        }

        fileIdsToDelete.addAll(
                fileAttachmentService.deleteAllByResource(ResourceType.COURSE, courseId.toString())
        );

        for (Long lessonId : lessonIds) {
            fileIdsToDelete.addAll(
                    fileAttachmentService.deleteAllByResource(ResourceType.LESSON, lessonId.toString())
            );
        }

        repository.delete(course);

        for (UUID fileId : fileIdsToDelete) {
            if (!fileAttachmentService.hasAttachments(fileId)) {
                fileService.deleteFile(fileId);
            }
        }

        for (CourseTag tag : tags) {
            if (repository.countByCourseTagsContains(tag) == 0) {
                courseTagRepository.delete(tag);
            }
        }

        courseCacheService.evict(courseId, course.getHandle());
    }

    @Transactional(readOnly = true)
    public Course findCourseById(Long courseId) {
        return repository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
    }

    @Transactional
    public Course findCourseByIdForUpdate(Long courseId) {
        return repository.findByIdForUpdate(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
    }

    public boolean canEdit(Course course, Auth auth) {
        return auth.getRole() == Role.ADMIN ||
                course.getOwner().equals(auth) ||
                course.hasEditor(auth);
    }

    @Transactional(rollbackFor = Exception.class)
    public Course updatePreview(Long courseId, Auth initiator, MultipartFile file) throws IOException {
        Course course = findCourseById(courseId);

        if(!canEdit(course, initiator))
            throw new CourseCannotBeEditByUserException(initiator.getId());

        File previousPreview = course.getPreview();
        File savedFile = fileService.saveFile(file);
        course.setPreview(savedFile);

        Course updatedCourse = repository.save(course);

        fileAttachmentService.attach(
                savedFile,
                ResourceType.COURSE,
                course.getId().toString(),
                Purpose.PREVIEW,
                previewVisibilityFor(course.getState()),
                course.getOwner()
        );

        if (previousPreview != null) {
            fileAttachmentService.deleteAllByFileId(previousPreview.getId());
            fileService.deleteFile(previousPreview.getId());
        }

        courseCacheService.evict(updatedCourse);
        return updatedCourse;
    }

    @Transactional(readOnly = true)
    public List<Course> findAllCourses(Auth viewer) {
        if (viewer == null) {
            return repository.findAllByState(CourseState.PUBLISHED);
        }

        if (viewer.getRole() == Role.ADMIN) {
            return repository.findAll();
        }

        return repository.findAll().stream()
                .filter(course -> course.getState() == CourseState.PUBLISHED || accessService.canSee(course, viewer))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<Course> findCoursesPage(Auth viewer, Pageable pageable, String q, CourseState state, String tag) {
        Page<Course> page = repository.findAll(
                CourseSpecifications.visibleTo(viewer)
                        .and(CourseSpecifications.search(q))
                        .and(CourseSpecifications.hasState(state))
                        .and(CourseSpecifications.hasTag(tag)),
                pageable
        );

        if (page.isEmpty()) {
            return page;
        }

        Set<Long> courseIds = page.getContent().stream()
                .map(Course::getId)
                .collect(Collectors.toSet());

        Map<Long, Course> hydratedById = repository.findAllListViewByIdIn(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, course -> course, (left, right) -> left, LinkedHashMap::new));

        List<Course> orderedContent = page.getContent().stream()
                .map(course -> hydratedById.getOrDefault(course.getId(), course))
                .toList();

        return new PageImpl<>(orderedContent, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> countLessonsByCourseIds(Set<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Integer> result = new LinkedHashMap<>();
        for (Object[] row : lessonRepository.countByCourseIds(courseIds)) {
            result.put((Long) row[0], ((Long) row[1]).intValue());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> countEnrollmentsByCourseIds(Set<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Integer> result = new LinkedHashMap<>();
        for (Object[] row : enrollmentRepository.countByCourseIds(courseIds)) {
            result.put((Long) row[0], ((Long) row[1]).intValue());
        }
        return result;
    }

    @Override
    public Course getByHandle(String handle) {
        return repository.findByHandle(handle)
                .orElseThrow(() -> new CourseNotFoundException(handle));
    }

    @Override
    public Course seeByHandle(String handle, Auth initiator) {
        return accessService.access(handle, initiator);
    }

    public Course seeById(Long courseId, Auth initiator) {
        return accessService.access(courseId, initiator);
    }

    @Transactional(readOnly = true)
    public ptknow.dto.course.CourseDTO seeDtoByHandle(String handle, Auth initiator) {
        Course course = seeByHandle(handle, initiator);
        if (course.getState() == CourseState.PUBLISHED) {
            return courseCacheService.getPublishedByHandle(course.getHandle());
        }
        return courseCacheService.toDto(course.getId());
    }

    @Transactional(readOnly = true)
    public ptknow.dto.course.CourseDTO seeDtoById(Long courseId, Auth initiator) {
        Course course = seeById(courseId, initiator);
        if (course.getState() == CourseState.PUBLISHED) {
            return courseCacheService.getPublishedById(course.getId());
        }
        return courseCacheService.toDto(course.getId());
    }

    @Override
    public boolean isOwner(Long resourceId, Auth auth) {
        return repository.existsByIdAndOwner_Id(resourceId, auth.getId());
    }

    @Override
    public Auth getOwner(Long resourceId) {
        Optional<Course> result = repository.findById(resourceId);

        if(result.isEmpty())
            throw new CourseNotFoundException(resourceId);

        return result.get().getOwner();
    }

    public Course addEditor(Long courseId, Auth initiator, UUID targetId) {
        Course course = findCourseById(courseId);

        validateOwnerOrAdmin(course, initiator);

        Auth target = authRepository.findById(targetId)
                .orElseThrow(() -> new UserNotFoundException(targetId));

        course.addEditor(target);

        authRepository.save(target);
        Course saved = repository.save(course);
        courseCacheService.evict(saved);
        return saved;
    }


    public Course removeEditor(Long courseId, Auth initiator, UUID targetId) {
        Course course = findCourseById(courseId);

        validateOwnerOrAdmin(course, initiator);

        Auth target = authRepository.findById(targetId)
                .orElseThrow(() -> new UserNotFoundException(targetId));

        course.removeEditor(target);

        authRepository.save(target);
        Course saved = repository.save(course);
        courseCacheService.evict(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Enrollment> findStudents(Long courseId, Auth initiator) {
        Course course = findCourseById(courseId);
        validateOwnerOrAdmin(course, initiator);

        return course.getEnrollments().stream()
                .sorted(Comparator.comparing(Enrollment::getEnrollSince))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseTeacherDTO> findTeachers(Long courseId, Auth initiator) {
        Course course = findCourseById(courseId);
        validateOwnerOrAdmin(course, initiator);

        Set<Auth> teachers = new HashSet<>(course.getEditors());
        teachers.add(course.getOwner());

        return teachers.stream()
                .map(teacher -> toCourseTeacherDTO(teacher, course))
                .toList();
    }

    @Transactional
    public Course addTeacher(Long courseId, Auth initiator, UUID teacherId) {
        Course course = findCourseById(courseId);
        validateOwnerOrAdmin(course, initiator);

        Auth target = authRepository.findById(teacherId)
                .orElseThrow(() -> new UserNotFoundException(teacherId));

        if (target.getRole() != Role.TEACHER && target.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only users with TEACHER or ADMIN role can be assigned as course teachers");
        }

        if (course.getOwner().equals(target)) {
            return course;
        }

        course.addEditor(target);

        authRepository.save(target);
        Course saved = repository.save(course);
        courseCacheService.evict(saved);
        return saved;
    }

    @Transactional
    public Course removeTeacher(Long courseId, Auth initiator, UUID teacherId) {
        Course course = findCourseById(courseId);
        validateOwnerOrAdmin(course, initiator);

        Auth target = authRepository.findById(teacherId)
                .orElseThrow(() -> new UserNotFoundException(teacherId));

        if (course.getOwner().equals(target)) {
            throw new AccessDeniedException("Course owner cannot be removed from teachers");
        }

        course.removeEditor(target);

        authRepository.save(target);
        Course saved = repository.save(course);
        courseCacheService.evict(saved);
        return saved;
    }

    @Transactional
    public Course publish(Long courseId, Auth initiator) {
        Course course = findCourseById(courseId);
        validateCanChangeCourseState(course, initiator);

        course.setState(CourseState.PUBLISHED);
        syncPreviewVisibility(course);
        Course saved = repository.save(course);
        courseCacheService.evict(saved);
        return saved;
    }

    @Transactional
    public Course archive(Long courseId, Auth initiator) {
        Course course = findCourseById(courseId);
        validateCanChangeCourseState(course, initiator);

        course.setState(CourseState.ARCHIVED);
        syncPreviewVisibility(course);
        Course saved = repository.save(course);
        courseCacheService.evict(saved);
        return saved;
    }

    @Transactional
    public Course updateByPatch(Long courseId, Auth initiator, UpdateCourseDTO dto) {
        Course course = findCourseById(courseId);
        validateCanChangeCourseState(course, initiator);

        Set<CourseTag> previousTags = new HashSet<>(course.getCourseTags());

        if (dto.name() != null) {
            validateCourseNameIsAvailable(course, dto.name());
            course.setName(dto.name());
        }

        if (dto.description() != null) {
            course.setDescription(dto.description());
        }

        if (dto.tags() != null) {
            course.replaceCourseTags(courseTagsFromNames(dto.tags()));
        }

        if (dto.maxUsersAmount() != null) {
            course.setMaxUsersAmount(dto.maxUsersAmount());
        }

        Course saved = repository.save(course);
        cleanupUnusedTags(previousTags);
        courseCacheService.evict(saved);
        return saved;
    }

    @Transactional
    public Course updateByPut(Long courseId, Auth initiator, CreateCourseDTO dto) {
        Course course = findCourseById(courseId);
        validateCanChangeCourseState(course, initiator);

        Set<CourseTag> previousTags = new HashSet<>(course.getCourseTags());

        validateCourseNameIsAvailable(course, dto.name());
        course.setName(dto.name());
        course.setDescription(dto.description());
        course.replaceCourseTags(courseTagsFromNames(dto.tags()));

        Course saved = repository.save(course);
        cleanupUnusedTags(previousTags);
        courseCacheService.evict(saved);
        return saved;
    }

    @Override
    public boolean canSee(Long id, Auth initiator) {
        return accessService.canSee(id, initiator);
    }

    private void validateCanChangeCourseState(Course course, Auth initiator) {
        validateOwnerOrAdmin(course, initiator);
    }

    private void validateOwnerOrAdmin(Course course, Auth initiator) {
        if (initiator.getRole() != Role.ADMIN && !course.getOwner().equals(initiator)) {
            throw new CourseNotOwnedByUserException(initiator.getId());
        }
    }

    private CourseTeacherDTO toCourseTeacherDTO(Auth teacher, Course course) {
        String profileHandle = teacher.getProfile() != null ? teacher.getProfile().getHandle() : null;
        String fullName = teacher.getProfile() != null ? teacher.getProfile().getFullName() : null;

        return new CourseTeacherDTO(
                teacher.getId(),
                teacher.getEmail(),
                teacher.getRole(),
                profileHandle,
                fullName,
                course.getOwner().equals(teacher)
        );
    }

    private void syncPreviewVisibility(Course course) {
        File preview = course.getPreview();
        if (preview == null) {
            return;
        }

        FileVisibility visibility = previewVisibilityFor(course.getState());
        try {
            FileAttachment attachment = fileAttachmentService.findByFileAndResourceAndPurpose(
                    preview.getId(),
                    ResourceType.COURSE,
                    course.getId().toString(),
                    Purpose.PREVIEW
            );
            attachment.setFileVisibility(visibility);
        } catch (FileAttachmentNotFoundException e) {
            fileAttachmentService.attach(
                    preview,
                    ResourceType.COURSE,
                    course.getId().toString(),
                    Purpose.PREVIEW,
                    visibility,
                    course.getOwner()
            );
        }
    }

    private FileVisibility previewVisibilityFor(CourseState state) {
        return state == CourseState.PUBLISHED ? FileVisibility.ENROLLED : FileVisibility.PRIVATE;
    }

    private void validateCourseNameIsAvailable(Course course, String newName) {
        if (newName.equals(course.getName())) {
            return;
        }

        if (repository.existsByName(newName)) {
            throw new CourseAlreadyExists(newName);
        }
    }

    private void cleanupUnusedTags(Set<CourseTag> previousTags) {
        for (CourseTag tag : previousTags) {
            if (repository.countByCourseTagsContains(tag) == 0) {
                courseTagRepository.delete(tag);
            }
        }
    }
}

