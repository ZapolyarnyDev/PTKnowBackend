package ptknow.dto.shared;

import io.swagger.v3.oas.annotations.media.Schema;
import ptknow.model.course.CourseState;

@Schema(name = "CourseSummaryDTO", description = "Compact course representation")
public record CourseSummaryDTO(
        @Schema(example = "42")
        Long id,
        @Schema(example = "Java Backend Basics")
        String name,
        @Schema(example = "java-backend-basics")
        String handle,
        @Schema(example = "PUBLISHED")
        CourseState state,
        @Schema(example = "/api/v0/files/550e8400-e29b-41d4-a716-446655440000")
        String previewUrl
) {
}
