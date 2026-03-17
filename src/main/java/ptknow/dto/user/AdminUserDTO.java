package ptknow.dto.user;

import ptknow.model.auth.AuthProvider;
import ptknow.model.auth.Role;
import ptknow.model.auth.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminUserDTO(
        UUID id,
        String email,
        Role role,
        UserStatus status,
        AuthProvider authProvider,
        Instant registeredAt,
        String profileHandle,
        String fullName
) {
}
