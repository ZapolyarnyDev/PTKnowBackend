package ptknow.dto.lesson;

import jakarta.validation.constraints.NotNull;
import ptknow.model.lesson.LessonState;

public record UpdateLessonStateDTO(
        @NotNull(message = "Lesson state is required")
        LessonState state
) {}
