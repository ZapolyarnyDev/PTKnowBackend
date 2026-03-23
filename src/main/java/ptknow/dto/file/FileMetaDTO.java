package ptknow.dto.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import ptknow.model.file.attachment.FileVisibility;
import ptknow.model.file.attachment.resource.Purpose;

import java.time.Instant;
import java.util.UUID;

@Builder
@Schema(name = "FileMetaDTO", description = "File metadata response")
public record FileMetaDTO(
        @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
        @Schema(example = "lesson-plan.pdf")
        String originalFilename,
        @Schema(example = "application/pdf")
        String contentType,
        @Schema(example = "81234")
        Long size,
        @Schema(example = "2026-03-22T01:15:00Z")
        Instant uploadedAt,
        @Schema(example = "/api/v0/files/550e8400-e29b-41d4-a716-446655440000")
        String downloadUrl,
        @Schema(example = "MATERIAL")
        Purpose purpose,
        @Schema(example = "ENROLLED")
        FileVisibility visibility
) {
}
