package ptknow.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import ptknow.dto.shared.CourseSummaryDTO;
import ptknow.model.auth.Role;

import java.util.List;

@Schema(name = "ProfileDetailsDTO", description = "Detailed profile representation")
public record ProfileDetailsDTO(
        @Schema(example = "Artem Artemov")
        String fullName,
        @Schema(example = "Java backend student")
        String summary,
        @Schema(example = "artemovv")
        String handle,
        @Schema(example = "/api/v1/files/550e8400-e29b-41d4-a716-446655440000")
        String avatarUrl,
        @Schema(example = "STUDENT")
        Role role,
        List<CourseSummaryDTO> enrolledCourses,
        List<CourseSummaryDTO> teachingCourses
) {
}