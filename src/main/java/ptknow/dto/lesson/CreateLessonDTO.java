package ptknow.dto.lesson;

import io.swagger.v3.oas.annotations.media.Schema;
import ptknow.model.lesson.LessonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(name = "CreateLessonDTO", description = "Payload for lesson creation or full replacement")
public record CreateLessonDTO(
        @Schema(example = "Dependency Injection")
        @NotBlank String name,
        @Schema(example = "Introduction to dependency injection in Spring")
        String description,
        @Schema(example = "# Dependency Injection\nWe will cover constructor injection and bean scopes.")
        @Size(max = 50000, message = "Lesson markdown content must be at most 50000 characters")
        String contentMd,
        @Schema(example = "2026-03-22T10:00:00Z")
        @NotNull Instant beginAt,
        @Schema(example = "2026-03-22T11:30:00Z")
        @NotNull Instant endsAt,
        @Schema(example = "LECTURE")
        @NotNull LessonType type
) {}
