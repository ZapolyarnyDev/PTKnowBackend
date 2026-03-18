package ptknow.dto.course;

import ptknow.model.auth.Role;

import java.util.UUID;

public record CourseTeacherDTO(
        UUID id,
        String email,
        Role role,
        String profileHandle,
        String fullName,
        boolean owner
) {
}
