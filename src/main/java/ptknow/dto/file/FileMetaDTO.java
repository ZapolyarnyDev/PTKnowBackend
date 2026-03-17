package ptknow.dto.file;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record FileMetaDTO(
        UUID id,
        String originalFilename,
        String contentType,
        Long size,
        Instant uploadedAt
) {
}
