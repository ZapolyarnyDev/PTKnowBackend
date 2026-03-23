package ptknow.api.lesson;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import ptknow.dto.lesson.CreateLessonDTO;
import ptknow.dto.lesson.LessonDTO;
import ptknow.dto.lesson.UpdateLessonDTO;
import ptknow.dto.lesson.UpdateLessonStateDTO;
import ptknow.mapper.lesson.LessonMapper;
import ptknow.model.auth.Auth;
import ptknow.service.lesson.LessonService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ptknow.api.exception.ApiError;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/lessons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Уроки", description = "CRUD уроков, markdown-контент и материалы уроков")
public class LessonController {

    LessonService lessonService;
    LessonMapper lessonMapper;

    @PostMapping("/{courseId}")
    @Operation(summary = "Создать урок", description = "Создаёт урок внутри курса. Требуется роль TEACHER или ADMIN; фактический бизнес-доступ: OWNER(course)|EDITOR(course)|ADMIN.")
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
    @Operation(summary = "Получить урок по id", description = "Возвращает данные урока, включая markdown-контент и прикреплённые материалы, если у текущего пользователя есть доступ к родительскому курсу.")
    @PreAuthorize("hasAnyRole('GUEST', 'STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<LessonDTO> getLesson(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal Auth auth
    ) {
        var lesson = lessonMapper.toDTO(lessonService.seeById(lessonId, auth));
        return ResponseEntity.ok(lesson);
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "Получить уроки курса", description = "Возвращает уроки курса, если у текущего пользователя есть доступ к этому курсу.")
    @ApiResponse(responseCode = "200", description = "Уроки получены",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = LessonDTO.class))))
    @PreAuthorize("hasAnyRole('GUEST', 'STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<List<LessonDTO>> getLessonsByCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal Auth auth
    ) {
        var lessons = lessonService.findAllByCourse(courseId, auth).stream()
                .map(lessonMapper::toDTO)
                .toList();
        return ResponseEntity.ok(lessons);
    }

    @DeleteMapping("/{lessonId}")
    @Operation(summary = "Удалить урок", description = "Удаляет урок. Фактический бизнес-доступ: OWNER(lesson)|OWNER(course)|ADMIN.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> deleteLesson(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal Auth auth) throws IOException {
        lessonService.deleteById(lessonId, auth);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/{lessonId}/materials")
    @Operation(summary = "Загрузить материал урока", description = "Загружает файл и прикрепляет его как материал урока. Фактический бизнес-доступ: OWNER(lesson)|ADMIN.")
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
    @Operation(summary = "Удалить материал урока", description = "Удаляет attachment материала урока и физический файл, если на него больше нет других ссылок. Фактический бизнес-доступ: OWNER(lesson)|ADMIN.")
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
    @Operation(summary = "Частично обновить урок", description = "Частично обновляет поля урока. Фактический бизнес-доступ: OWNER(lesson)|ADMIN.")
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
    @Operation(summary = "Полностью заменить урок", description = "Полностью заменяет поля урока. Фактический бизнес-доступ: OWNER(lesson)|ADMIN.")
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
    @Operation(summary = "Изменить состояние урока", description = "Обновляет состояние урока в его жизненном цикле. Фактический бизнес-доступ: OWNER(lesson)|ADMIN.")
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
}

