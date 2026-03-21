package ptknow.api.profile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import ptknow.dto.profile.ProfileResponseDTO;
import ptknow.dto.profile.ProfileUpdateDTO;
import ptknow.model.auth.Auth;
import ptknow.mapper.profile.ProfileMapper;
import ptknow.service.profile.ProfileService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ptknow.api.exception.ApiError;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/v0/profile")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Профиль", description = "Получение и изменение профиля пользователя")
public class ProfileController {

    ProfileService profileService;
    ProfileMapper profileMapper;

    @GetMapping
    @Operation(summary = "Получить мой профиль", description = "Возвращает профиль текущего аутентифицированного пользователя.")
    public ResponseEntity<ProfileResponseDTO> getMyProfile(@AuthenticationPrincipal Auth user) {
        var profile = profileService.getProfile(user.getId());
        var dto = profileMapper.toDto(profile);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/me")
    @Operation(summary = "Получить мой профиль через alias", description = "Синоним метода GET /v0/profile.")
    public ResponseEntity<ProfileResponseDTO> getMyProfileAlias(@AuthenticationPrincipal Auth user) {
        var profile = profileService.getProfile(user.getId());
        var dto = profileMapper.toDto(profile);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{handle}")
    @Operation(summary = "Получить профиль по handle", description = "Возвращает профиль по handle. Несмотря на публичный путь, по текущей security-конфигурации требуется аутентификация.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль получен",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProfileResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Профиль не найден",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ProfileResponseDTO> getProfileByHandle(@PathVariable String handle) {
        var profile = profileService.getByHandle(handle);
        var dto = profileMapper.toDto(profile);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/id/{userId}")
    @Operation(summary = "Получить профиль по id пользователя", description = "Возвращает профиль по id пользователя. Видимость дополнительно проверяется в service layer.")
    public ResponseEntity<ProfileResponseDTO> getProfileByUserId(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Auth user
    ) {
        var profile = profileService.getProfile(userId, user);
        var dto = profileMapper.toDto(profile);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/avatar")
    @Operation(summary = "Загрузить или заменить аватар", description = "Загружает файл аватара для профиля текущего пользователя.")
    public ResponseEntity<ProfileResponseDTO> updateAvatar(
            @AuthenticationPrincipal Auth user,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        var updatedProfile = profileService.updateAvatar(user.getId(), file);
        var dto = profileMapper.toDto(updatedProfile);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/avatar")
    @Operation(summary = "Удалить аватар", description = "Удаляет аватар текущего пользователя.")
    public ResponseEntity<Void> deleteAvatar(
            @AuthenticationPrincipal Auth user
    ) throws IOException {
        profileService.deleteAvatar(user.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping
    @Operation(summary = "Полностью заменить профиль", description = "Полностью заменяет редактируемые поля профиля текущего пользователя.")
    public ResponseEntity<ProfileResponseDTO> updateMyProfile(
            @AuthenticationPrincipal Auth user,
            @Valid @RequestBody ProfileUpdateDTO dto
    ) {
        var updated = profileService.update(user.getId(), dto);
        var updatedDto = profileMapper.toDto(updated);
        return ResponseEntity.ok(updatedDto);
    }

    @PatchMapping
    @Operation(summary = "Частично обновить профиль", description = "Частично обновляет редактируемые поля профиля текущего пользователя.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль обновлён",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProfileResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Профиль не найден",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ProfileResponseDTO> patchMyProfile(
            @AuthenticationPrincipal Auth user,
            @Valid @RequestBody ProfileUpdateDTO dto
    ) {
        var updated = profileService.update(user.getId(), dto);
        var updatedDto = profileMapper.toDto(updated);
        return ResponseEntity.ok(updatedDto);
    }
}

