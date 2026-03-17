package ptknow.service.course;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ptknow.dto.course.CreateCourseDTO;
import ptknow.model.auth.Auth;
import ptknow.model.auth.Role;
import ptknow.model.course.Course;
import ptknow.model.course.CourseState;
import ptknow.model.course.CourseTag;
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
import ptknow.service.AccessService;
import ptknow.service.HandleService;
import ptknow.service.OwnershipService;
import ptknow.service.file.FileAttachmentService;
import ptknow.service.file.FileService;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
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
    HandleGenerator handleGenerator;
    FileService fileService;
    FileAttachmentService fileAttachmentService;
    CourseAccessService accessService;

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

        initiator.addOwnedCourse(entity);

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

        if(initiator.getRole() != Role.ADMIN && !course.getOwner().equals(initiator))
            throw new CourseNotOwnedByUserException(initiator.getId());

        Auth target = authRepository.findById(targetId)
                .orElseThrow(() -> new UserNotFoundException(targetId));

        course.addEditor(target);

        authRepository.save(target);
        return repository.save(course);
    }


    public Course removeEditor(Long courseId, Auth initiator, UUID targetId) {
        Course course = findCourseById(courseId);

        if(initiator.getRole() != Role.ADMIN && !course.getOwner().equals(initiator))
            throw new CourseNotOwnedByUserException(initiator.getId());

        Auth target = authRepository.findById(targetId)
                .orElseThrow(() -> new UserNotFoundException(targetId));

        course.removeEditor(target);

        authRepository.save(target);
        return repository.save(course);
    }

    @Transactional
    public Course publish(Long courseId, Auth initiator) {
        Course course = findCourseById(courseId);
        validateCanChangeCourseState(course, initiator);

        course.setState(CourseState.PUBLISHED);
        syncPreviewVisibility(course);
        return repository.save(course);
    }

    @Transactional
    public Course archive(Long courseId, Auth initiator) {
        Course course = findCourseById(courseId);
        validateCanChangeCourseState(course, initiator);

        course.setState(CourseState.ARCHIVED);
        syncPreviewVisibility(course);
        return repository.save(course);
    }

    @Override
    public boolean canSee(Long id, Auth initiator) {
        return accessService.canSee(id, initiator);
    }

    private void validateCanChangeCourseState(Course course, Auth initiator) {
        if (initiator.getRole() != Role.ADMIN && !course.getOwner().equals(initiator)) {
            throw new CourseNotOwnedByUserException(initiator.getId());
        }
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
}

