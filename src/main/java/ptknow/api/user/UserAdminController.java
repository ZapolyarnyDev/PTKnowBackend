package ptknow.api.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import ptknow.api.exception.ApiError;
import ptknow.dto.user.AdminUserDTO;
import ptknow.dto.user.UpdateUserRoleDTO;
import ptknow.dto.user.UpdateUserStatusDTO;
import ptknow.model.auth.Auth;
import ptknow.service.auth.AdminUserService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Администрирование пользователей", description = "Методы управления пользователями, доступные только ADMIN")
public class UserAdminController {

    AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "Получить всех пользователей", description = "Возвращает список всех пользователей. Доступно только ADMIN.")
    @ApiResponse(responseCode = "200", description = "Список пользователей получен",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = AdminUserDTO.class))))
    public ResponseEntity<List<AdminUserDTO>> getAllUsers() {
        return ResponseEntity.ok(adminUserService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя по id", description = "Возвращает одного пользователя по id. Доступно только ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пользователь получен",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminUserDTO.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<AdminUserDTO> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(adminUserService.findById(id));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Изменить роль пользователя", description = "Изменяет роль указанного пользователя. Доступно только ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Роль обновлена",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminUserDTO.class))),
            @ApiResponse(responseCode = "400", description = "Некорректное тело запроса",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<AdminUserDTO> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleDTO dto,
            @AuthenticationPrincipal Auth initiator
    ) {
        return ResponseEntity.ok(adminUserService.updateRole(initiator, id, dto.role()));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Изменить статус пользователя", description = "Изменяет статус указанного пользователя. Доступно только ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Статус обновлён",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminUserDTO.class))),
            @ApiResponse(responseCode = "400", description = "Некорректное тело запроса",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<AdminUserDTO> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusDTO dto,
            @AuthenticationPrincipal Auth initiator
    ) {
        return ResponseEntity.ok(adminUserService.updateStatus(initiator, id, dto.status()));
    }
}
