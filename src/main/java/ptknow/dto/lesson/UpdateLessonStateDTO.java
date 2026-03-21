package ptknow.dto.lesson;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import ptknow.model.lesson.LessonState;

@Schema(name = "UpdateLessonStateDTO", description = "Payload for lesson state update")
public record UpdateLessonStateDTO(
        @Schema(example = "FINISHED")
        @NotNull(message = "Lesson state is required")
        LessonState state
) {}
