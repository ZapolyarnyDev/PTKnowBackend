package ptknow.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;
import ptknow.model.auth.Role;

import java.util.UUID;

@Schema(name = "CourseTeacherDTO", description = "Teacher/editor assigned to course")
public record CourseTeacherDTO(
        @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
        @Schema(example = "teacher@ptknow.dev")
        String email,
        @Schema(example = "TEACHER")
        Role role,
        @Schema(example = "ivan-petrov")
        String profileHandle,
        @Schema(example = "Ivan Petrov")
        String fullName,
        @Schema(example = "false")
        boolean owner
) {
}
