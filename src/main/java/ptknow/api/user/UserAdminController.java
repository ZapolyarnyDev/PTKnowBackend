package ptknow.api.user;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ptknow.dto.user.AdminUserDTO;
import ptknow.dto.user.UpdateUserRoleDTO;
import ptknow.dto.user.UpdateUserStatusDTO;
import ptknow.model.auth.Auth;
import ptknow.service.auth.AdminUserService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v0/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserAdminController {

    AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<List<AdminUserDTO>> getAllUsers() {
        return ResponseEntity.ok(adminUserService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDTO> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(adminUserService.findById(id));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<AdminUserDTO> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleDTO dto,
            @AuthenticationPrincipal Auth initiator
    ) {
        return ResponseEntity.ok(adminUserService.updateRole(initiator, id, dto.role()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminUserDTO> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusDTO dto,
            @AuthenticationPrincipal Auth initiator
    ) {
        return ResponseEntity.ok(adminUserService.updateStatus(initiator, id, dto.status()));
    }
}
