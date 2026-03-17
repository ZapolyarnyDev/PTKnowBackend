package ptknow.dto.lesson;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ptknow.model.lesson.LessonType;

import java.time.Instant;

public record UpdateLessonDTO(
        @Pattern(regexp = ".*\\S.*", message = "Lesson name must not be blank")
        @Size(max = 255, message = "Lesson name must be at most 255 characters")
        String name,
        @Size(max = 1000, message = "Lesson description must be at most 1000 characters")
        String description,
        Instant beginAt,
        Instant endsAt,
        LessonType type
) {
    @AssertTrue(message = "Lesson end time can't be before start time")
    public boolean isTimeRangeValid() {
        return beginAt == null || endsAt == null || !endsAt.isBefore(beginAt);
    }
}
