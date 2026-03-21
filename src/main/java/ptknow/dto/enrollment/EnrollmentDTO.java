package ptknow.dto.enrollment;

import io.swagger.v3.oas.annotations.media.Schema;
import ptknow.dto.shared.CourseSummaryDTO;
import ptknow.dto.shared.UserSummaryDTO;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "EnrollmentDTO", description = "Enrollment response")
public record EnrollmentDTO(
        @Schema(example = "15")
        Long id,
        @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
        UUID userId,
        @Schema(example = "42")
        Long courseId,
        @Schema(example = "2026-03-22T01:15:00Z")
        Instant since,
        UserSummaryDTO user,
        CourseSummaryDTO course
) {
}
