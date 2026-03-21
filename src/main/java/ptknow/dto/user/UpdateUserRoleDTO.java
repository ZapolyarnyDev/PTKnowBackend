package ptknow.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import ptknow.model.auth.Role;

@Schema(name = "UpdateUserRoleDTO", description = "Payload for role change")
public record UpdateUserRoleDTO(
        @Schema(example = "TEACHER")
        @NotNull(message = "Role is required")
        Role role
) {
}
