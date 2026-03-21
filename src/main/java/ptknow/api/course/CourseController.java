package ptknow.api.course;

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
import ptknow.dto.course.CourseDTO;
import ptknow.dto.course.CourseTeacherDTO;
import ptknow.dto.course.CreateCourseDTO;
import ptknow.dto.course.UpdateCourseTeacherDTO;
import ptknow.dto.course.UpdateCourseDTO;
import ptknow.dto.enrollment.EnrollmentDTO;
import ptknow.mapper.enrollment.EnrollmentMapper;
import ptknow.model.auth.Auth;
import ptknow.model.course.Course;
import ptknow.mapper.course.CourseMapper;
import ptknow.model.enrollment.Enrollment;
import ptknow.service.course.CourseService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ptknow.api.exception.ApiError;
import ptknow.service.enrollment.EnrollmentService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v0/course")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Курсы", description = "Каталог курсов, управление курсами, преподавателями и участниками")
public class CourseController {

    CourseService courseService;
    EnrollmentService enrollmentService;
    CourseMapper courseMapper;
    EnrollmentMapper enrollmentMapper;

    @GetMapping
    @Operation(summary = "Получить список доступных курсов", description = "Возвращает курсы, видимые текущему пользователю. Анонимный доступ не разрешён security-конфигурацией. Для non-admin это опубликованные курсы и курсы, доступные по ownership/editor/enrollment-правилам.")
    @ApiResponse(responseCode = "200", description = "Курсы получены",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = CourseDTO.class))))
    @PreAuthorize("hasAnyRole('GUEST', 'STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<List<CourseDTO>> get(@AuthenticationPrincipal Auth auth) {
        List<CourseDTO> courseDTOS = courseService.findAllCourses(auth).stream()
                .map(courseMapper::courseToDTO)
                .toList();
        return ResponseEntity.ok(courseDTOS);
    }

    @PostMapping
    @Operation(summary = "Создать курс", description = "Создаёт новый курс в состоянии DRAFT. Требуется роль TEACHER или ADMIN.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<CourseDTO> createCourse(
            @Valid @RequestPart("course") CreateCourseDTO dto,
            @RequestPart(value = "preview", required = false) MultipartFile preview,
            @AuthenticationPrincipal Auth entity
            ) throws IOException {
        Course course = courseService.publishCourse(dto, entity, preview);

        return ResponseEntity.ok(courseMapper.courseToDTO(course));
    }

    @GetMapping("/id/{id}")
    @Operation(summary = "Получить курс по id", description = "Возвращает курс по id, если у текущего пользователя есть к нему доступ. Видимость зависит от состояния курса, ownership, editor-прав и enrollment.")
    @PreAuthorize("hasAnyRole('GUEST', 'STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<CourseDTO> getCourse(
            @PathVariable Long id,
            @AuthenticationPrincipal Auth auth
    ) {
        CourseDTO course = courseMapper.courseToDTO(courseService.seeById(id, auth));
        return ResponseEntity.ok(course);
    }

    @GetMapping("/handle/{handle}")
    @Operation(summary = "Получить курс по handle", description = "Возвращает курс по handle, если у текущего пользователя есть к нему доступ. Видимость зависит от состояния курса, ownership, editor-прав и enrollment.")
    @PreAuthorize("hasAnyRole('GUEST', 'STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<CourseDTO> getCourse(
            @PathVariable String handle,
            @AuthenticationPrincipal Auth auth
    ) {
        CourseDTO course = courseMapper.courseToDTO(courseService.seeByHandle(handle, auth));
        return ResponseEntity.ok(course);
    }

    @PostMapping("/{id}/preview")
    @Operation(summary = "Загрузить или заменить preview курса", description = "Загружает preview-файл курса. Фактический бизнес-доступ: OWNER(course)|EDITOR(course)|ADMIN.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<CourseDTO> updatePreview(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Auth entity
    ) throws IOException {
        var updatedProfile = courseService.updatePreview(id, entity, file);
        var dto = courseMapper.courseToDTO(updatedProfile);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить курс", description = "Удаляет курс вместе со связанными уроками, attachment-ами и очисткой orphan-файлов. Фактический бизнес-доступ: OWNER(course)|ADMIN.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable Long id,
            @AuthenticationPrincipal Auth entity
            ) throws IOException {
        courseService.deleteCourseById(id, entity);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/{id}/editors/{userId}")
    @Operation(summary = "Добавить редактора курса", description = "Добавляет редактора курса. Требуется роль TEACHER или ADMIN; фактический бизнес-доступ: OWNER(course)|ADMIN.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> addCourseEditor(
            @PathVariable Long id,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Auth entity
            ) {
        courseService.addEditor(id, entity, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/editors/{userId}")
    @Operation(summary = "Удалить редактора курса", description = "Удаляет редактора курса. Требуется роль TEACHER или ADMIN; фактический бизнес-доступ: OWNER(course)|ADMIN.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> removeCourseEditor(
            @PathVariable Long id,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Auth entity
    ) {
        courseService.removeEditor(id, entity, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Опубликовать курс", description = "Переводит курс в состояние PUBLISHED и синхронизирует видимость preview. Фактический бизнес-доступ: OWNER(course)|ADMIN.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<CourseDTO> publishCourse(
            @PathVariable Long id,
            @AuthenticationPrincipal Auth entity
    ) {
        var course = courseService.publish(id, entity);
        return ResponseEntity.ok(courseMapper.courseToDTO(course));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Архивировать курс", description = "Переводит курс в состояние ARCHIVED и синхронизирует видимость preview. Фактический бизнес-доступ: OWNER(course)|ADMIN.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<CourseDTO> archiveCourse(
            @PathVariable Long id,
            @AuthenticationPrincipal Auth entity
    ) {
        var course = courseService.archive(id, entity);
        return ResponseEntity.ok(courseMapper.courseToDTO(course));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Частично обновить курс", description = "Частично обновляет поля курса. Фактический бизнес-доступ: OWNER(course)|ADMIN.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<CourseDTO> patchCourse(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCourseDTO dto,
            @AuthenticationPrincipal Auth entity
    ) {
        Course course = courseService.updateByPatch(id, entity, dto);
        return ResponseEntity.ok(courseMapper.courseToDTO(course));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Полностью заменить курс", description = "Полностью заменяет редактируемые поля курса. Фактический бизнес-доступ: OWNER(course)|ADMIN.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<CourseDTO> putCourse(
            @PathVariable Long id,
            @Valid @RequestBody CreateCourseDTO dto,
            @AuthenticationPrincipal Auth entity
    ) {
        Course course = courseService.updateByPut(id, entity, dto);
        return ResponseEntity.ok(courseMapper.courseToDTO(course));
    }

    @PostMapping("/{id}/enroll")
    @Operation(summary = "Записаться на курс", description = "Записывает текущего пользователя на курс. Разрешено только ролям GUEST/STUDENT, не более одной записи на пару user-course и только при наличии свободных мест.")
    @PreAuthorize("hasAnyRole('GUEST', 'STUDENT')")
    public ResponseEntity<Void> enroll(
            @PathVariable Long id,
            @AuthenticationPrincipal Auth entity
    ) {
        enrollmentService.enroll(entity, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/enroll")
    @Operation(summary = "Отписаться от курса", description = "Удаляет запись текущего пользователя на курс. Разрешено только ролям GUEST/STUDENT.")
    @PreAuthorize("hasAnyRole('GUEST', 'STUDENT')")
    public ResponseEntity<Void> unEnroll(
            @PathVariable Long id,
            @AuthenticationPrincipal Auth entity
    ) {
        enrollmentService.unenroll(entity, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "Получить участников курса", description = "Возвращает enrollment-записи курса. Требуется аутентифицированный пользователь с ролью GUEST/STUDENT/TEACHER/ADMIN, но фактический доступ есть только у OWNER(course)|EDITOR(course)|ENROLLED|ADMIN.")
    @ApiResponse(responseCode = "200", description = "Участники курса получены",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = EnrollmentDTO.class))))
    @PreAuthorize("hasAnyRole('GUEST', 'STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<List<EnrollmentDTO>> getMembers (
            @PathVariable Long id,
            @AuthenticationPrincipal Auth entity
    ) {
        List<Enrollment> enrollments = enrollmentService.findAllByCourse(entity, id);
        return ResponseEntity.ok(
                enrollmentMapper.mapEntityList(enrollments)
        );
    }

    @GetMapping("/{id}/students")
    @Operation(summary = "Получить студентов курса", description = "Возвращает enrollment-записи студентов курса. Требуется роль TEACHER или ADMIN, но фактический бизнес-доступ: OWNER(course)|ADMIN.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<List<EnrollmentDTO>> getStudents(
            @PathVariable Long id,
            @AuthenticationPrincipal Auth entity
    ) {
        List<Enrollment> enrollments = courseService.findStudents(id, entity);
        return ResponseEntity.ok(enrollmentMapper.mapEntityList(enrollments));
    }

    @GetMapping("/{id}/teachers")
    @Operation(summary = "Получить преподавателей курса", description = "Возвращает owner курса и редакторов, выступающих преподавателями. Требуется роль TEACHER или ADMIN, но фактический бизнес-доступ: OWNER(course)|ADMIN. Guest-доступ не разрешён.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<List<CourseTeacherDTO>> getTeachers(
            @PathVariable Long id,
            @AuthenticationPrincipal Auth entity
    ) {
        return ResponseEntity.ok(courseService.findTeachers(id, entity));
    }

    @PostMapping("/{id}/teachers")
    @Operation(summary = "Назначить преподавателя курса", description = "Добавляет пользователя с ролью TEACHER или ADMIN как преподавателя/редактора курса. Требуется роль TEACHER или ADMIN, но фактический бизнес-доступ: OWNER(course)|ADMIN.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> addTeacher(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCourseTeacherDTO dto,
            @AuthenticationPrincipal Auth entity
    ) {
        courseService.addTeacher(id, entity, dto.teacherId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/teachers/{teacherId}")
    @Operation(summary = "Удалить преподавателя курса", description = "Удаляет преподавателя/редактора из курса. Owner удалить нельзя. Требуется роль TEACHER или ADMIN, но фактический бизнес-доступ: OWNER(course)|ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Преподаватель удалён"),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос или попытка удалить owner",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Курс или преподаватель не найден",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> removeTeacher(
            @PathVariable Long id,
            @PathVariable UUID teacherId,
            @AuthenticationPrincipal Auth entity
    ) {
        courseService.removeTeacher(id, entity, teacherId);
        return ResponseEntity.noContent().build();
    }
}

