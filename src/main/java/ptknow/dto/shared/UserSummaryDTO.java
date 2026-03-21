package ptknow.dto.shared;

import ptknow.model.auth.Role;
import ptknow.model.auth.UserStatus;

import java.util.UUID;

public record UserSummaryDTO(
        UUID id,
        String email,
        Role role,
        UserStatus status,
        String handle,
        String fullName,
        String avatarUrl
) {
}
