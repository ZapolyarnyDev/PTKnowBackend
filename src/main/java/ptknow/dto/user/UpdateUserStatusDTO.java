package ptknow.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import ptknow.model.auth.UserStatus;

@Schema(name = "UpdateUserStatusDTO", description = "Payload for status change")
public record UpdateUserStatusDTO(
        @Schema(example = "BLOCKED")
        @NotNull(message = "Status is required")
        UserStatus status
) {
}
