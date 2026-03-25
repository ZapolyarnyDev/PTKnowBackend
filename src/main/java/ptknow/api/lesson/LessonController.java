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
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Уроки", description = "CRUD уроков, markdown-содержимое и материалы уроков")
public class LessonController {

    LessonService lessonService;
    LessonMapper lessonMapper;

    @PostMapping("/{courseId}")
    @Operation(summary = "Создать урок", description = "Создаёт урок в курсе. Фактический доступ: OWNER(course)|EDITOR(course)|ADMIN.")
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
    @Operation(summary = "Получить урок по id", description = "Возвращает детали урока, markdown-содержимое и прикреплённые материалы, если у пользователя есть доступ к родительскому курсу.")
    @PreAuthorize("permitAll()")
    public ResponseEntity<LessonDTO> getLesson(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal Auth auth
    ) {
        var lesson = lessonMapper.toDTO(lessonService.seeById(lessonId, auth));
        return ResponseEntity.ok(lesson);
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "Получить уроки курса", description = "Возвращает страницу уроков курса, если у пользователя есть доступ к этому курсу.")
    @ApiResponse(responseCode = "200", description = "Уроки получены",
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
    @Operation(summary = "Удалить урок", description = "Удаляет урок. Фактический доступ: OWNER(lesson)|OWNER(course)|ADMIN.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> deleteLesson(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal Auth auth) throws IOException {
        lessonService.deleteById(lessonId, auth);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping(value = "/{lessonId}/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить материал урока", description = "Загружает файл и прикрепляет его как материал урока. Фактический доступ: OWNER(lesson)|ADMIN.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<UUID> uploadMaterial(
            @PathVariable Long lessonId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Auth auth
    ) throws IOException {
        UUID materialId = lessonService.uploadMaterial(lessonId, auth, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(materialId);
    }

    @DeleteMapping("/{lessonId}/materials/{fileId}")
    @Operation(summary = "Удалить материал урока", description = "Удаляет вложение материала урока и физический файл, если на него больше нет ссылок. Фактический доступ: OWNER(lesson)|ADMIN.")
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
    @Operation(summary = "Частично обновить урок", description = "Частично обновляет поля урока. Фактический доступ: OWNER(lesson)|ADMIN.")
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
    @Operation(summary = "Полностью заменить урок", description = "Полностью заменяет поля урока. Фактический доступ: OWNER(lesson)|ADMIN.")
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
    @Operation(summary = "Обновить состояние урока", description = "Обновляет жизненный цикл урока. Фактический доступ: OWNER(lesson)|ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Состояние урока обновлено",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LessonDTO.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Урок не найден",
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
