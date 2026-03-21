package ptknow.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(name = "UpdateCourseTeacherDTO", description = "Payload for assigning a teacher/editor")
public record UpdateCourseTeacherDTO(
        @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull(message = "Teacher id is required")
        UUID teacherId
) {
}
