package ptknow.dto.user;

import jakarta.validation.constraints.NotNull;
import ptknow.model.auth.Role;

public record UpdateUserRoleDTO(
        @NotNull(message = "Role is required")
        Role role
) {
}
