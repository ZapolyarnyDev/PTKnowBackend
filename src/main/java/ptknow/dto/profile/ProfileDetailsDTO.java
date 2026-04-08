package ptknow.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import ptknow.dto.shared.CourseSummaryDTO;
import ptknow.model.auth.Role;
import ptknow.model.auth.UserStatus;

import java.util.List;
import java.util.UUID;

@Schema(name = "ProfileDetailsDTO", description = "Detailed profile representation")
public record ProfileDetailsDTO(
        @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
        @Schema(example = "Artem Artemov")
        String fullName,
        @Schema(example = "Java backend student")
        String summary,
        @Schema(example = "artemovv")
        String handle,
        @Schema(example = "/api/v1/files/550e8400-e29b-41d4-a716-446655440000")
        String avatarUrl,
        @Schema(example = "student@ptknow.dev")
        String email,
        @Schema(example = "ACTIVE")
        UserStatus status,
        @Schema(example = "STUDENT")
        Role role,
        List<CourseSummaryDTO> enrolledCourses,
        List<CourseSummaryDTO> teachingCourses
) {
}
