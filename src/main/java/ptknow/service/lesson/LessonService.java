package ptknow.service.lesson;

import ptknow.dto.lesson.CreateLessonDTO;
import ptknow.exception.lesson.LessonCannotBeCreatedException;
import ptknow.exception.lesson.LessonNotOwnedException;
import ptknow.exception.lesson.NotAllowedToSeeLessonInfo;
import ptknow.model.auth.Auth;
import ptknow.model.auth.Role;
import ptknow.model.course.Course;
import ptknow.model.file.File;
import ptknow.model.file.attachment.FileVisibility;
import ptknow.model.file.attachment.resource.Purpose;
import ptknow.model.file.attachment.resource.ResourceType;
import ptknow.model.lesson.Lesson;
import ptknow.exception.lesson.LessonNotFoundException;
import ptknow.repository.lesson.LessonRepository;
import ptknow.service.AccessService;
import ptknow.service.OwnershipService;
import ptknow.service.course.CourseAccessService;
import ptknow.service.course.CourseService;
import ptknow.service.file.FileAttachmentService;
import ptknow.service.file.FileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonService implements OwnershipService<Long>, AccessService<Long> {

    LessonRepository lessonRepository;
    CourseService courseService;
    CourseAccessService accessService;
    FileService fileService;
    FileAttachmentService fileAttachmentService;

    @Transactional
    public Lesson createLesson(Long courseId, Auth initiator, CreateLessonDTO dto) throws LessonCannotBeCreatedException {
        Course course = courseService.findCourseById(courseId);

        if (!courseService.canEdit(course, initiator))
            throw new LessonCannotBeCreatedException(initiator.getId());

        Lesson entity = Lesson.builder()
                .name(dto.name())
                .description(dto.description())
                .beginAt(dto.beginAt())
                .endsAt(dto.endsAt())
                .course(course)
                .lessonType(dto.type())
                .owner(initiator)
                .build();

        initiator.addOwnedLesson(entity);

        return lessonRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public Lesson findById(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new LessonNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Lesson seeById(Long id, Auth initiator) throws NotAllowedToSeeLessonInfo {
        var lesson = findById(id);

        if(!accessService.canSee(lesson.getCourse(), initiator))
            throw new NotAllowedToSeeLessonInfo(initiator.getId());

        return lesson;
    }

    @Transactional(readOnly = true)
    public List<Lesson> findAllByCourse(Long courseId) {
        return lessonRepository.getAllByCourse_Id(courseId);
    }

    @Transactional(readOnly = true)
    public List<Lesson> findAllByCourse(Long courseId, Auth initiator) {
        if(!accessService.canSee(courseId, initiator))
            throw new NotAllowedToSeeLessonInfo(initiator.getId());

        return lessonRepository.getAllByCourse_Id(courseId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id, Auth initiator) throws LessonNotOwnedException, IOException {
        Lesson lesson = findById(id);

        if (!canDelete(lesson, initiator))
            throw new LessonNotOwnedException(initiator.getId());

        cleanupLessonFiles(id);
        lessonRepository.delete(lesson);
    }

    @Transactional(rollbackFor = Exception.class)
    public UUID uploadMaterial(Long lessonId, Auth initiator, MultipartFile material) throws IOException {
        Lesson lesson = findById(lessonId);
        validateCanManageMaterials(lesson, initiator);

        File savedFile = fileService.saveFile(material);
        fileAttachmentService.attach(
                savedFile,
                ResourceType.LESSON,
                lessonId.toString(),
                Purpose.MATERIAL,
                FileVisibility.ENROLLED,
                lesson.getOwner()
        );

        return savedFile.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteMaterial(Long lessonId, UUID fileId, Auth initiator) throws IOException {
        Lesson lesson = findById(lessonId);
        validateCanManageMaterials(lesson, initiator);

        var attachment = fileAttachmentService.findByFileAndResourceAndPurpose(
                fileId,
                ResourceType.LESSON,
                lessonId.toString(),
                Purpose.MATERIAL
        );

        fileAttachmentService.delete(attachment);
        if (!fileAttachmentService.hasAttachments(fileId)) {
            fileService.deleteFile(fileId);
        }
    }

    @Override
    public boolean isOwner(Long resourceId, Auth auth) {
        return lessonRepository.existsByIdAndOwner_Id(resourceId, auth.getId());
    }

    @Override
    public Auth getOwner(Long resourceId) {
        return findById(resourceId).getOwner();
    }

    public boolean canEdit(Lesson lesson, Auth auth) {
        return auth.getRole() == Role.ADMIN ||
                lesson.getOwner().equals(auth) ||
                courseService.canEdit(lesson.getCourse(), auth);
    }

    public boolean canDelete(Lesson lesson, Auth auth) {
        return auth.getRole() == Role.ADMIN ||
                lesson.getOwner().equals(auth) ||
                lesson.getCourse().getOwner().equals(auth);
    }

    private void validateCanManageMaterials(Lesson lesson, Auth initiator) {
        if (initiator.getRole() != Role.ADMIN && !lesson.getOwner().equals(initiator)) {
            throw new LessonNotOwnedException(initiator.getId());
        }
    }

    private void cleanupLessonFiles(Long lessonId) throws IOException {
        var attachments = fileAttachmentService.findAllByResource(ResourceType.LESSON, lessonId.toString());

        for (var attachment : attachments) {
            UUID fileId = attachment.getFile().getId();
            fileAttachmentService.delete(attachment);

            if (!fileAttachmentService.hasAttachments(fileId)) {
                fileService.deleteFile(fileId);
            }
        }
    }

    @Override
    public boolean canSee(Long id, Auth initiator) {
        var lesson = findById(id);
        return accessService.canSee(lesson.getCourse().getId(), initiator);
    }
}

