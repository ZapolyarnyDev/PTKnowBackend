package ptknow.dto.course;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateCourseTeacherDTO(
        @NotNull(message = "Teacher id is required")
        UUID teacherId
) {
}
