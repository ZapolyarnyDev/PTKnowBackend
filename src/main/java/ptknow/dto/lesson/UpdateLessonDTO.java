package ptknow.dto.lesson;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ptknow.model.lesson.LessonType;

import java.time.Instant;

@Schema(name = "UpdateLessonDTO", description = "Partial lesson update payload")
public record UpdateLessonDTO(
        @Schema(example = "Dependency Injection Basics")
        @Pattern(regexp = ".*\\S.*", message = "Lesson name must not be blank")
        @Size(max = 255, message = "Lesson name must be at most 255 characters")
        String name,
        @Schema(example = "Updated lesson description")
        @Size(max = 1000, message = "Lesson description must be at most 1000 characters")
        String description,
        @Schema(example = "## Updated markdown content")
        @Size(max = 50000, message = "Lesson markdown content must be at most 50000 characters")
        String contentMd,
        @Schema(example = "2026-03-22T10:00:00Z")
        Instant beginAt,
        @Schema(example = "2026-03-22T11:30:00Z")
        Instant endsAt,
        @Schema(example = "PRACTICE")
        LessonType type
) {
    @AssertTrue(message = "Lesson end time can't be before start time")
    public boolean isTimeRangeValid() {
        return beginAt == null || endsAt == null || !endsAt.isBefore(beginAt);
    }
}
