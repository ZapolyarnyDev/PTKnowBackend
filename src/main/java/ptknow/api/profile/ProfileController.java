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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ptknow.api.exception.ApiError;
import ptknow.dto.common.PageResponseDTO;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/profile")
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
    @Operation(summary = "Получить мой профиль через alias", description = "Синоним метода GET /api/v0/profile.")
    public ResponseEntity<ProfileResponseDTO> getMyProfileAlias(@AuthenticationPrincipal Auth user) {
        var profile = profileService.getProfile(user.getId());
        var dto = profileMapper.toDto(profile);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/search")
    @Operation(summary = "Search profiles", description = "Returns a paginated profile list for user search by full name or handle.")
    @ApiResponse(responseCode = "200", description = "Profiles returned",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PageResponseDTO.class)))
    public ResponseEntity<PageResponseDTO<ProfileResponseDTO>> searchProfiles(
            @AuthenticationPrincipal Auth user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullName,asc") String sort,
            @RequestParam(required = false) String q
    ) {
        var pageRequest = PageRequest.of(page, Math.min(size, 100), parseSort(sort));
        var result = profileService.search(user, pageRequest, q);

        var body = new PageResponseDTO<>(
                result.getContent().stream().map(profileMapper::toDto).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );

        return ResponseEntity.ok(body);
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

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "fullName");
        }

        String[] parts = sort.split(",", 2);
        String property = parts[0].trim();
        String direction = parts.length > 1 ? parts[1].trim() : "asc";

        if (!List.of("fullName", "handle").contains(property)) {
            property = "fullName";
        }

        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return Sort.by(sortDirection, property);
    }
}

