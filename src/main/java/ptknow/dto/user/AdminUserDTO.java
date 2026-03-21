package ptknow.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import ptknow.model.auth.AuthProvider;
import ptknow.model.auth.Role;
import ptknow.model.auth.UserStatus;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "AdminUserDTO", description = "Admin-facing user response")
public record AdminUserDTO(
        @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
        @Schema(example = "teacher@ptknow.dev")
        String email,
        @Schema(example = "TEACHER")
        Role role,
        @Schema(example = "ACTIVE")
        UserStatus status,
        @Schema(example = "LOCAL")
        AuthProvider authProvider,
        @Schema(example = "2026-03-22T01:15:00Z")
        Instant registeredAt,
        @Schema(example = "ivan-petrov")
        String profileHandle,
        @Schema(example = "Ivan Petrov")
        String fullName
) {
}
