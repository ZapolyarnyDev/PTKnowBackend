package ptknow.service.auth;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptknow.dto.user.AdminUserDTO;
import ptknow.model.auth.Auth;
import ptknow.model.auth.Role;
import ptknow.model.auth.UserStatus;
import ptknow.model.auth.audit.UserAdminAction;
import ptknow.model.auth.audit.UserAdminAudit;
import ptknow.exception.user.UserNotFoundException;
import ptknow.repository.auth.AuthRepository;
import ptknow.repository.auth.UserAdminAuditRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminUserService {

    AuthRepository authRepository;
    UserAdminAuditRepository auditRepository;
    JwtService jwtService;

    @Transactional(readOnly = true)
    public List<AdminUserDTO> findAll() {
        return authRepository.findAllByOrderByRegisteredAtDesc()
                .stream()
                .map(this::toAdminUserDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDTO> findPage(Pageable pageable, String q, Role role, UserStatus status) {
        return authRepository.findAll(
                        AuthSpecifications.search(q)
                                .and(AuthSpecifications.hasRole(role))
                                .and(AuthSpecifications.hasStatus(status)),
                        pageable
                )
                .map(this::toAdminUserDTO);
    }

    @Transactional(readOnly = true)
    public AdminUserDTO findById(UUID userId) {
        return toAdminUserDTO(findUser(userId));
    }

    @Transactional
    public AdminUserDTO updateRole(Auth initiator, UUID targetId, Role newRole) {
        Auth target = findUser(targetId);

        if (initiator.getId().equals(targetId)) {
            throw new AccessDeniedException("You cannot change your own role");
        }

        Role oldRole = target.getRole();
        if (oldRole == newRole) {
            return toAdminUserDTO(target);
        }

        target.setRole(newRole);
        Auth saved = authRepository.save(target);

        saveAudit(
                initiator.getId(),
                targetId,
                UserAdminAction.ROLE_CHANGED,
                oldRole.name(),
                newRole.name()
        );

        jwtService.invalidateUserTokens(saved);
        return toAdminUserDTO(saved);
    }

    @Transactional
    public AdminUserDTO updateStatus(Auth initiator, UUID targetId, UserStatus newStatus) {
        Auth target = findUser(targetId);

        if (initiator.getId().equals(targetId) && newStatus == UserStatus.BLOCKED) {
            throw new AccessDeniedException("You cannot block your own account");
        }

        UserStatus oldStatus = target.getStatus();
        if (oldStatus == newStatus) {
            return toAdminUserDTO(target);
        }

        target.setStatus(newStatus);
        Auth saved = authRepository.save(target);

        saveAudit(
                initiator.getId(),
                targetId,
                UserAdminAction.STATUS_CHANGED,
                oldStatus.name(),
                newStatus.name()
        );

        jwtService.invalidateUserTokens(saved);
        return toAdminUserDTO(saved);
    }

    private Auth findUser(UUID userId) {
        return authRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private void saveAudit(UUID actorId, UUID targetId, UserAdminAction action, String oldValue, String newValue) {
        auditRepository.save(
                UserAdminAudit.builder()
                        .actorId(actorId)
                        .targetId(targetId)
                        .action(action)
                        .oldValue(oldValue)
                        .newValue(newValue)
                        .build()
        );
    }

    private AdminUserDTO toAdminUserDTO(Auth auth) {
        String profileHandle = auth.getProfile() != null ? auth.getProfile().getHandle() : null;
        String fullName = auth.getProfile() != null ? auth.getProfile().getFullName() : null;

        return new AdminUserDTO(
                auth.getId(),
                auth.getEmail(),
                auth.getRole(),
                auth.getStatus(),
                auth.getAuthProvider(),
                auth.getRegisteredAt(),
                profileHandle,
                fullName
        );
    }
}
