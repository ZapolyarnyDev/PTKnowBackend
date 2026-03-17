package ptknow.service.file;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptknow.exception.file.FileAttachmentNotFoundException;
import ptknow.model.auth.Auth;
import ptknow.model.auth.Role;
import ptknow.model.file.attachment.FileAttachment;
import ptknow.repository.file.FileAttachmentRepository;
import ptknow.service.AccessService;
import ptknow.service.OwnershipService;
import ptknow.service.course.CourseService;
import ptknow.service.lesson.LessonService;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileAccessService implements OwnershipService<Long> {

    FileAttachmentRepository attachmentRepository;

    LessonService lessonService;
    CourseService courseService;

    @Transactional(readOnly = true)
    public boolean canRead(UUID fileId, Auth user) {
        Set<FileAttachment> attachments = findAllByFileIdOrThrow(fileId);

        if(user.getRole() == Role.ADMIN)
            return true;

        return attachments.stream().anyMatch(attachment -> canRead(attachment, user));
    }

    @Transactional(readOnly = true)
    public boolean canDelete(UUID fileId, Auth user) {
        if (user.getRole() == Role.ADMIN)
            return true;

        Set<FileAttachment> attachments = findAllByFileIdOrThrow(fileId);

        return attachments.stream().allMatch(attachment -> attachment.getOwner().equals(user));
    }

    private Set<FileAttachment> findAllByFileIdOrThrow(UUID fileId) {
        Set<FileAttachment> attachments = attachmentRepository.findAllByFile_Id(fileId);
        if (attachments.isEmpty()) {
            throw new FileAttachmentNotFoundException(fileId);
        }
        return attachments;
    }

    private boolean canRead(FileAttachment attachment, Auth user) {
        return switch (attachment.getFileVisibility()) {
            case PUBLIC -> true;
            case PRIVATE -> canSeeOwned(attachment, user);
            case ENROLLED -> canSeeEnrolled(attachment, user);
            case null, default -> false;
        };
    }

    @Override
    public boolean isOwner(Long resourceId, Auth auth) {
        return attachmentRepository.existsByIdAndOwner_Id(resourceId, auth.getId());
    }

    @Override
    public Auth getOwner(Long resourceId) throws FileAttachmentNotFoundException {
        return findById(resourceId).getOwner();
    }

    @Transactional(readOnly = true)
    public FileAttachment findById(Long id) throws FileAttachmentNotFoundException {
        return attachmentRepository.findById(id)
                .orElseThrow(() -> new FileAttachmentNotFoundException(id));
    }

    private boolean canSeeOwned(FileAttachment attachment, Auth user) {
        return switch (attachment.getResourceType()) {
            case PROFILE -> isProfileOwner(attachment, user);
            case COURSE  -> isCourseOwner(attachment, user);
            case LESSON  -> isLessonOwner(attachment, user);
        };
    }

    private boolean isProfileOwner(FileAttachment a, Auth u) {
        return a.getOwner().getId().equals(u.getId());
    }

    private boolean isCourseOwner(FileAttachment a, Auth u) {
        Long resourceId = parseLongResourceId(a);
        if (resourceId == null)
            return false;

        return courseService.isOwner(resourceId, u);
    }

    private boolean isLessonOwner(FileAttachment a, Auth u) {
        Long resourceId = parseLongResourceId(a);
        if (resourceId == null)
            return false;

        return lessonService.isOwner(resourceId, u);
    }

    public boolean canSeeEnrolled(FileAttachment attachment, Auth initiator) {
        AccessService<Long> accessService = switch (attachment.getResourceType()) {
            case COURSE -> courseService;
            case LESSON -> lessonService;
            case null, default -> null;
        };

        if(accessService == null)
            return false;

        Long resourceId = parseLongResourceId(attachment);
        if (resourceId == null)
            return false;

        return accessService.canSee(
                resourceId,
                initiator
        );
    }

    private Long parseLongResourceId(FileAttachment a) {
        try {
            return Long.valueOf(a.getResourceId());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
