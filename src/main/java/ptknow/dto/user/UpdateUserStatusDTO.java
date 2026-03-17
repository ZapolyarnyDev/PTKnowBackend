package ptknow.dto.user;

import jakarta.validation.constraints.NotNull;
import ptknow.model.auth.UserStatus;

public record UpdateUserStatusDTO(
        @NotNull(message = "Status is required")
        UserStatus status
) {
}
