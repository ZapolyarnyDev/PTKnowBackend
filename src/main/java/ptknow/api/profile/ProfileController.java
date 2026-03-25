package ptknow.api.profile;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ptknow.api.exception.ApiError;
import ptknow.dto.common.PageResponseDTO;
import ptknow.dto.profile.ProfileDetailsDTO;
import ptknow.dto.profile.ProfileResponseDTO;
import ptknow.dto.profile.ProfileUpdateDTO;
import ptknow.mapper.profile.ProfileMapper;
import ptknow.model.auth.Auth;
import ptknow.service.profile.ProfileService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/profile")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Профиль", description = "Получение, поиск и изменение пользовательских профилей")
public class ProfileController {

    ProfileService profileService;
    ProfileMapper profileMapper;

    @GetMapping
    @Operation(summary = "Получить мой профиль", description = "Возвращает детальный профиль текущего аутентифицированного пользователя.")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileDetailsDTO> getMyProfile(@AuthenticationPrincipal Auth user) {
        return ResponseEntity.ok(profileService.getOwnProfileDetails(user.getId()));
    }

    @GetMapping("/me")
    @Operation(summary = "Получить мой профиль через alias", description = "Алиас для GET /api/v0/profile.")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileDetailsDTO> getMyProfileAlias(@AuthenticationPrincipal Auth user) {
        return ResponseEntity.ok(profileService.getOwnProfileDetails(user.getId()));
    }

    @GetMapping("/search")
    @Operation(summary = "Поиск профилей", description = "Возвращает лёгкую страницу профилей с поиском по полному имени или handle.")
    @ApiResponse(responseCode = "200", description = "Профили получены",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageResponseDTO.class)))
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
    @Operation(summary = "Получить публичный профиль по handle", description = "Возвращает публичный профиль и только публично видимые курсы пользователя.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль получен",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProfileDetailsDTO.class))),
            @ApiResponse(responseCode = "404", description = "Профиль не найден",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ProfileDetailsDTO> getProfileByHandle(@PathVariable String handle) {
        return ResponseEntity.ok(profileService.getPublicProfileDetails(handle));
    }

    @GetMapping("/id/{userId}")
    @Operation(summary = "Получить профиль по user id", description = "Возвращает профиль по user id. Для non-owner отдаёт только публично видимые курсы пользователя.")
    public ResponseEntity<ProfileDetailsDTO> getProfileByUserId(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Auth user
    ) {
        return ResponseEntity.ok(profileService.getVisibleProfileDetails(userId, user));
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить или заменить аватар", description = "Загружает аватар текущего аутентифицированного пользователя.")
    public ResponseEntity<ProfileDetailsDTO> updateAvatar(
            @AuthenticationPrincipal Auth user,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        profileService.updateAvatar(user.getId(), file);
        return ResponseEntity.ok(profileService.getOwnProfileDetails(user.getId()));
    }

    @DeleteMapping("/avatar")
    @Operation(summary = "Удалить аватар", description = "Удаляет аватар текущего аутентифицированного пользователя.")
    public ResponseEntity<Void> deleteAvatar(@AuthenticationPrincipal Auth user) throws IOException {
        profileService.deleteAvatar(user.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping
    @Operation(summary = "Полностью заменить профиль", description = "Полностью заменяет редактируемые поля профиля текущего пользователя.")
    public ResponseEntity<ProfileDetailsDTO> updateMyProfile(
            @AuthenticationPrincipal Auth user,
            @Valid @RequestBody ProfileUpdateDTO dto
    ) {
        profileService.update(user.getId(), dto);
        return ResponseEntity.ok(profileService.getOwnProfileDetails(user.getId()));
    }

    @PatchMapping
    @Operation(summary = "Частично обновить профиль", description = "Частично обновляет редактируемые поля профиля текущего пользователя.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль обновлён",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProfileDetailsDTO.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Профиль не найден",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ProfileDetailsDTO> patchMyProfile(
            @AuthenticationPrincipal Auth user,
            @Valid @RequestBody ProfileUpdateDTO dto
    ) {
        profileService.update(user.getId(), dto);
        return ResponseEntity.ok(profileService.getOwnProfileDetails(user.getId()));
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