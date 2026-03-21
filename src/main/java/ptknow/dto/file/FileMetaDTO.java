package ptknow.dto.file;

import lombok.Builder;
import ptknow.model.file.attachment.FileVisibility;
import ptknow.model.file.attachment.resource.Purpose;

import java.time.Instant;
import java.util.UUID;

@Builder
public record FileMetaDTO(
        UUID id,
        String originalFilename,
        String contentType,
        Long size,
        Instant uploadedAt,
        String downloadUrl,
        Purpose purpose,
        FileVisibility visibility
) {
}
