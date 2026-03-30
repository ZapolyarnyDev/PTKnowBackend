package ptknow.service.file;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptknow.exception.course.CourseNotFoundException;
import ptknow.exception.file.FileAccessDeniedException;
import ptknow.exception.file.FileAttachmentNotFoundException;
import ptknow.exception.file.FileNotFoundException;
import ptknow.exception.file.InvalidResourceIdException;
import ptknow.exception.lesson.LessonNotFoundException;
import ptknow.exception.profile.ProfileNotFoundException;
import ptknow.model.auth.Auth;
import ptknow.model.file.File;
import ptknow.model.file.attachment.FileAttachment;
import ptknow.model.file.attachment.FileVisibility;
import ptknow.model.file.attachment.resource.Purpose;
import ptknow.model.file.attachment.resource.ResourceType;
import ptknow.repository.course.CourseRepository;
import ptknow.repository.file.FileAttachmentRepository;
import ptknow.repository.file.FileRepository;
import ptknow.repository.lesson.LessonRepository;
import ptknow.repository.profile.ProfileRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileAttachmentService {

    FileAttachmentRepository attachmentRepository;
    FileRepository fileRepository;
    CourseRepository courseRepository;
    LessonRepository lessonRepository;
    ProfileRepository profileRepository;

    @Transactional
    public FileAttachment attach(
            File file,
            ResourceType resourceType,
            String resourceId,
            Purpose purpose,
            FileVisibility visibility,
            Auth owner
    ) {
        Auth resourceOwner = resolveResourceOwner(resourceType, resourceId);
        if (!resourceOwner.equals(owner))
            throw new FileAccessDeniedException("Attachment owner does not match resource owner");

        File managedFile = fileRepository.findById(file.getId())
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        FileAttachment attachment = FileAttachment.builder()
                .file(managedFile)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .purpose(purpose)
                .fileVisibility(visibility)
                .owner(owner)
                .build();

        return attachmentRepository.save(attachment);
    }

    @Transactional
    public void deleteAllByFileId(UUID fileId) {
        Set<FileAttachment> attachments = attachmentRepository.findAllByFile_Id(fileId);
        attachmentRepository.deleteAllInBatch(attachments);
    }

    @Transactional(readOnly = true)
    public Set<FileAttachment> findAllByResource(ResourceType resourceType, String resourceId) {
        return attachmentRepository.findAllByResourceTypeAndResourceId(resourceType, resourceId);
    }

    @Transactional(readOnly = true)
    public Map<String, List<FileAttachment>> findAllByResourceGrouped(ResourceType resourceType, Set<String> resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return Map.of();
        }

        return attachmentRepository.findAllByResourceTypeAndResourceIdIn(resourceType, resourceIds).stream()
                .collect(Collectors.groupingBy(FileAttachment::getResourceId));
    }

    @Transactional
    public Set<UUID> deleteAllByResource(ResourceType resourceType, String resourceId) {
        Set<FileAttachment> attachments = attachmentRepository.findAllByResourceTypeAndResourceId(resourceType, resourceId);
        if (attachments.isEmpty()) {
            return Set.of();
        }

        Set<UUID> fileIds = new LinkedHashSet<>();
        for (FileAttachment attachment : attachments) {
            fileIds.add(attachment.getFile().getId());
        }

        attachmentRepository.deleteAllInBatch(attachments);
        return fileIds;
    }

    @Transactional(readOnly = true)
    public FileAttachment findByFileAndResourceAndPurpose(
            UUID fileId,
            ResourceType resourceType,
            String resourceId,
            Purpose purpose
    ) {
        return attachmentRepository.findByFile_IdAndResourceTypeAndResourceIdAndPurpose(
                        fileId,
                        resourceType,
                        resourceId,
                        purpose
                )
                .orElseThrow(() -> new FileAttachmentNotFoundException(resourceType, resourceId));
    }

    @Transactional(readOnly = true)
    public boolean hasAttachments(UUID fileId) {
        return attachmentRepository.countByFile_Id(fileId) > 0;
    }

    @Transactional
    public void delete(FileAttachment attachment) {
        attachmentRepository.delete(attachment);
    }

    private Auth resolveResourceOwner(ResourceType resourceType, String resourceId) {
        return switch (resourceType) {
            case PROFILE -> {
                UUID profileId = parseUuidResourceId(resourceType, resourceId);
                yield profileRepository.findById(profileId)
                    .orElseThrow(() -> new ProfileNotFoundException(profileId))
                    .getUser();
            }
            case COURSE -> {
                Long courseId = parseLongResourceId(resourceType, resourceId);
                yield courseRepository.findById(courseId)
                    .orElseThrow(() -> new CourseNotFoundException(courseId))
                    .getOwner();
            }
            case LESSON -> {
                Long lessonId = parseLongResourceId(resourceType, resourceId);
                yield lessonRepository.findById(lessonId)
                    .orElseThrow(() -> new LessonNotFoundException(lessonId))
                    .getOwner();
            }
        };
    }

    private UUID parseUuidResourceId(ResourceType resourceType, String resourceId) {
        try {
            return UUID.fromString(resourceId);
        } catch (IllegalArgumentException e) {
            throw new InvalidResourceIdException(resourceType, resourceId, "UUID");
        }
    }

    private Long parseLongResourceId(ResourceType resourceType, String resourceId) {
        try {
            return Long.valueOf(resourceId);
        } catch (NumberFormatException e) {
            throw new InvalidResourceIdException(resourceType, resourceId, "Long");
        }
    }
}
