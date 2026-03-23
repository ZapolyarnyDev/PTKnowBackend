package ptknow.dto.shared;

import io.swagger.v3.oas.annotations.media.Schema;
import ptknow.model.auth.Role;
import ptknow.model.auth.UserStatus;

import java.util.UUID;

@Schema(name = "UserSummaryDTO", description = "Compact user representation")
public record UserSummaryDTO(
        @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
        @Schema(example = "teacher@ptknow.dev")
        String email,
        @Schema(example = "TEACHER")
        Role role,
        @Schema(example = "ACTIVE")
        UserStatus status,
        @Schema(example = "ivan-petrov")
        String handle,
        @Schema(example = "Ivan Petrov")
        String fullName,
        @Schema(example = "/api/v0/files/550e8400-e29b-41d4-a716-446655440000")
        String avatarUrl
) {
}
