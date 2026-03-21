package ptknow.dto.lesson;

import ptknow.dto.file.FileMetaDTO;
import ptknow.dto.shared.CourseSummaryDTO;
import ptknow.dto.shared.UserSummaryDTO;
import ptknow.model.lesson.LessonState;
import ptknow.model.lesson.LessonType;

import java.time.Instant;
import java.util.List;

public record LessonDTO(
        Long id,
        String name,
        String description,
        String contentMd,
        Instant beginAt,
        Instant endsAt,
        LessonState state,
        Long courseId,
        CourseSummaryDTO course,
        LessonType type,
        UserSummaryDTO owner,
        List<FileMetaDTO> materials
) {}
