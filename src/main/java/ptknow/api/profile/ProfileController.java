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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ptknow.api.exception.ApiError;
import ptknow.dto.common.PageResponseDTO;
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
@Tag(name = "Profiles", description = "Profile read and update endpoints")
public class ProfileController {

    ProfileService profileService;
    ProfileMapper profileMapper;

    @GetMapping
    @Operation(summary = "Get my profile", description = "Returns the profile of the current authenticated user.")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileResponseDTO> getMyProfile(@AuthenticationPrincipal Auth user) {
        var profile = profileService.getProfile(user.getId());
        return ResponseEntity.ok(profileMapper.toDto(profile));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my profile via alias", description = "Alias for GET /api/v0/profile.")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileResponseDTO> getMyProfileAlias(@AuthenticationPrincipal Auth user) {
        var profile = profileService.getProfile(user.getId());
        return ResponseEntity.ok(profileMapper.toDto(profile));
    }

    @GetMapping("/search")
    @Operation(summary = "Search profiles", description = "Returns a paginated profile list filtered by full name or handle.")
    @ApiResponse(responseCode = "200", description = "Profiles returned",
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
    @Operation(summary = "Get profile by handle", description = "Returns a profile by handle.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProfileResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Profile not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ProfileResponseDTO> getProfileByHandle(@PathVariable String handle) {
        var profile = profileService.getByHandle(handle);
        return ResponseEntity.ok(profileMapper.toDto(profile));
    }

    @GetMapping("/id/{userId}")
    @Operation(summary = "Get profile by user id", description = "Returns a profile by user id. Access is checked in service layer.")
    public ResponseEntity<ProfileResponseDTO> getProfileByUserId(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Auth user
    ) {
        var profile = profileService.getProfile(userId, user);
        return ResponseEntity.ok(profileMapper.toDto(profile));
    }

    @PostMapping("/avatar")
    @Operation(summary = "Upload or replace avatar", description = "Uploads an avatar file for the current authenticated user.")
    public ResponseEntity<ProfileResponseDTO> updateAvatar(
            @AuthenticationPrincipal Auth user,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        var updatedProfile = profileService.updateAvatar(user.getId(), file);
        return ResponseEntity.ok(profileMapper.toDto(updatedProfile));
    }

    @DeleteMapping("/avatar")
    @Operation(summary = "Delete avatar", description = "Deletes the avatar of the current authenticated user.")
    public ResponseEntity<Void> deleteAvatar(@AuthenticationPrincipal Auth user) throws IOException {
        profileService.deleteAvatar(user.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping
    @Operation(summary = "Replace profile", description = "Fully replaces editable fields of the current authenticated user profile.")
    public ResponseEntity<ProfileResponseDTO> updateMyProfile(
            @AuthenticationPrincipal Auth user,
            @Valid @RequestBody ProfileUpdateDTO dto
    ) {
        var updated = profileService.update(user.getId(), dto);
        return ResponseEntity.ok(profileMapper.toDto(updated));
    }

    @PatchMapping
    @Operation(summary = "Patch profile", description = "Partially updates editable fields of the current authenticated user profile.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProfileResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Profile not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ProfileResponseDTO> patchMyProfile(
            @AuthenticationPrincipal Auth user,
            @Valid @RequestBody ProfileUpdateDTO dto
    ) {
        var updated = profileService.update(user.getId(), dto);
        return ResponseEntity.ok(profileMapper.toDto(updated));
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