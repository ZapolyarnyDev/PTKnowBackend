package ptknow.mapper;

import org.springframework.stereotype.Component;
import ptknow.dto.file.FileMetaDTO;
import ptknow.dto.shared.CourseSummaryDTO;
import ptknow.dto.shared.UserSummaryDTO;
import ptknow.model.auth.Auth;
import ptknow.model.course.Course;
import ptknow.model.file.File;
import ptknow.model.file.attachment.FileAttachment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class ApiViewMapper {

    public UserSummaryDTO toUserSummary(Auth auth) {
        if (auth == null) {
            return null;
        }

        var profile = auth.getProfile();
        UUID avatarId = profile != null && profile.getAvatar() != null ? profile.getAvatar().getId() : null;

        return new UserSummaryDTO(
                auth.getId(),
                auth.getEmail(),
                auth.getRole(),
                auth.getStatus(),
                profile != null ? profile.getHandle() : null,
                profile != null ? profile.getFullName() : null,
                toFileUrl(avatarId)
        );
    }

    public CourseSummaryDTO toCourseSummary(Course course) {
        if (course == null) {
            return null;
        }

        return new CourseSummaryDTO(
                course.getId(),
                course.getName(),
                course.getHandle(),
                course.getState(),
                course.getPreview() != null ? toFileUrl(course.getPreview().getId()) : null
        );
    }

    public FileMetaDTO toFileMeta(File file) {
        if (file == null) {
            return null;
        }

        return FileMetaDTO.builder()
                .id(file.getId())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .size(resolveSize(file))
                .uploadedAt(file.getUploadedAt())
                .downloadUrl(toFileUrl(file.getId()))
                .build();
    }

    public FileMetaDTO toFileMeta(FileAttachment attachment) {
        if (attachment == null) {
            return null;
        }

        return FileMetaDTO.builder()
                .id(attachment.getFile().getId())
                .originalFilename(attachment.getFile().getOriginalFilename())
                .contentType(attachment.getFile().getContentType())
                .size(resolveSize(attachment.getFile()))
                .uploadedAt(attachment.getFile().getUploadedAt())
                .downloadUrl(toFileUrl(attachment.getFile().getId()))
                .purpose(attachment.getPurpose())
                .visibility(attachment.getFileVisibility())
                .build();
    }

    public String toFileUrl(UUID fileId) {
        if (fileId == null) {
            return null;
        }
        return "/api/v0/files/" + fileId;
    }

    private Long resolveSize(File file) {
        try {
            return Files.size(Path.of(file.getStoragePath()));
        } catch (Exception e) {
            return null;
        }
    }
}
