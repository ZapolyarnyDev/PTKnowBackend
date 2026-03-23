package ptknow.api.lesson;

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
import org.springframework.http.HttpStatus;
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
import ptknow.dto.lesson.CreateLessonDTO;
import ptknow.dto.lesson.LessonDTO;
import ptknow.dto.lesson.UpdateLessonDTO;
import ptknow.dto.lesson.UpdateLessonStateDTO;
import ptknow.mapper.lesson.LessonMapper;
import ptknow.model.auth.Auth;
import ptknow.model.lesson.LessonState;
import ptknow.model.lesson.LessonType;
import ptknow.service.lesson.LessonService;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/lessons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Lessons", description = "Lesson CRUD, markdown content and lesson materials")
public class LessonController {

    LessonService lessonService;
    LessonMapper lessonMapper;

    @PostMapping("/{courseId}")
    @Operation(summary = "Create lesson", description = "Creates a lesson in a course. Effective access: OWNER(course)|EDITOR(course)|ADMIN.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<LessonDTO> createLesson(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateLessonDTO dto,
            @AuthenticationPrincipal Auth auth
    ) {
        var lesson = lessonService.createLesson(courseId, auth, dto);
        return ResponseEntity.ok(lessonMapper.toDTO(lesson));
    }

    @GetMapping("/{lessonId}")
    @Operation(summary = "Get lesson by id", description = "Returns lesson details, markdown content and attached materials if the caller can access the parent course.")
    @PreAuthorize("permitAll()")
    public ResponseEntity<LessonDTO> getLesson(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal Auth auth
    ) {
        var lesson = lessonMapper.toDTO(lessonService.seeById(lessonId, auth));
        return ResponseEntity.ok(lesson);
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "Get course lessons", description = "Returns a paginated lesson list for a course if the caller can access that course.")
    @ApiResponse(responseCode = "200", description = "Lessons returned",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PageResponseDTO.class)))
    @PreAuthorize("permitAll()")
    public ResponseEntity<PageResponseDTO<LessonDTO>> getLessonsByCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal Auth auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "beginAt,asc") String sort,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) LessonState state,
            @RequestParam(required = false) LessonType type,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to
    ) {
        var pageRequest = PageRequest.of(page, Math.min(size, 100), parseSort(sort));
        var result = lessonService.findPageByCourse(courseId, auth, pageRequest, q, state, type, from, to);

        var body = new PageResponseDTO<>(
                lessonMapper.toDTOList(result.getContent()),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );

        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/{lessonId}")
    @Operation(summary = "Delete lesson", description = "Deletes a lesson. Effective access: OWNER(lesson)|OWNER(course)|ADMIN.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> deleteLesson(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal Auth auth) throws IOException {
        lessonService.deleteById(lessonId, auth);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/{lessonId}/materials")
    @Operation(summary = "Upload lesson material", description = "Uploads a file and attaches it as a lesson material. Effective access: OWNER(lesson)|ADMIN.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<UUID> uploadMaterial(
            @PathVariable Long lessonId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Auth auth
    ) throws IOException {
        UUID materialId = lessonService.uploadMaterial(lessonId, auth, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(materialId);
    }

    @DeleteMapping("/{lessonId}/materials/{fileId}")
    @Operation(summary = "Delete lesson material", description = "Deletes a lesson material attachment and removes the physical file if it has no remaining links. Effective access: OWNER(lesson)|ADMIN.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> deleteMaterial(
            @PathVariable Long lessonId,
            @PathVariable UUID fileId,
            @AuthenticationPrincipal Auth auth
    ) throws IOException {
        lessonService.deleteMaterial(lessonId, fileId, auth);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{lessonId}")
    @Operation(summary = "Patch lesson", description = "Partially updates lesson fields. Effective access: OWNER(lesson)|ADMIN.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<LessonDTO> patchLesson(
            @PathVariable Long lessonId,
            @Valid @RequestBody UpdateLessonDTO dto,
            @AuthenticationPrincipal Auth auth
    ) {
        var lesson = lessonService.updateByPatch(lessonId, auth, dto);
        return ResponseEntity.ok(lessonMapper.toDTO(lesson));
    }

    @PutMapping("/{lessonId}")
    @Operation(summary = "Replace lesson", description = "Fully replaces lesson fields. Effective access: OWNER(lesson)|ADMIN.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<LessonDTO> putLesson(
            @PathVariable Long lessonId,
            @Valid @RequestBody CreateLessonDTO dto,
            @AuthenticationPrincipal Auth auth
    ) {
        var lesson = lessonService.updateByPut(lessonId, auth, dto);
        return ResponseEntity.ok(lessonMapper.toDTO(lesson));
    }

    @PatchMapping("/{lessonId}/state")
    @Operation(summary = "Update lesson state", description = "Updates lesson lifecycle state. Effective access: OWNER(lesson)|ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lesson state updated",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LessonDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Lesson not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<LessonDTO> patchLessonState(
            @PathVariable Long lessonId,
            @Valid @RequestBody UpdateLessonStateDTO dto,
            @AuthenticationPrincipal Auth auth
    ) {
        var lesson = lessonService.updateState(lessonId, auth, dto);
        return ResponseEntity.ok(lessonMapper.toDTO(lesson));
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "beginAt");
        }

        String[] parts = sort.split(",", 2);
        String property = parts[0].trim();
        String direction = parts.length > 1 ? parts[1].trim() : "asc";

        if (!List.of("id", "name", "state", "type", "beginAt", "endsAt").contains(property)) {
            property = "beginAt";
        }

        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return Sort.by(sortDirection, property);
    }
}
