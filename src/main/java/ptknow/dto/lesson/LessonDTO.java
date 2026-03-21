package ptknow.dto.lesson;

import ptknow.model.lesson.LessonState;
import ptknow.model.lesson.LessonType;

import java.time.Instant;

public record LessonDTO(
        Long id,
        String name,
        String description,
        String contentMd,
        Instant beginAt,
        Instant endsAt,
        LessonState state,
        Long courseId,
        LessonType type
) {}
