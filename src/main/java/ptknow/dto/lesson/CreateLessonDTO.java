package ptknow.dto.lesson;

import ptknow.model.lesson.LessonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateLessonDTO(
        @NotBlank String name,
        String description,
        @Size(max = 50000, message = "Lesson markdown content must be at most 50000 characters")
        String contentMd,
        @NotNull Instant beginAt,
        @NotNull Instant endsAt,
        @NotNull LessonType type
) {}
