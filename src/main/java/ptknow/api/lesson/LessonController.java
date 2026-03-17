package ptknow.api.lesson;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import ptknow.dto.lesson.CreateLessonDTO;
import ptknow.dto.lesson.LessonDTO;
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

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v0/lessons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonController {

    LessonService lessonService;
    LessonMapper lessonMapper;

    @PostMapping("/{courseId}")
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
    @PreAuthorize("hasAnyRole('GUEST', 'STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<LessonDTO> getLesson(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal Auth auth
    ) {
        var lesson = lessonMapper.toDTO(lessonService.seeById(lessonId, auth));
        return ResponseEntity.ok(lesson);
    }

    @GetMapping("/course/{courseId}")
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
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> deleteLesson(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal Auth auth) throws IOException {
        lessonService.deleteById(lessonId, auth);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/{lessonId}/materials")
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
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> deleteMaterial(
            @PathVariable Long lessonId,
            @PathVariable UUID fileId,
            @AuthenticationPrincipal Auth auth
    ) throws IOException {
        lessonService.deleteMaterial(lessonId, fileId, auth);
        return ResponseEntity.noContent().build();
    }
}

