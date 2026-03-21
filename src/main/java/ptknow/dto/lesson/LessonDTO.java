package ptknow.dto.lesson;

import io.swagger.v3.oas.annotations.media.Schema;
import ptknow.dto.file.FileMetaDTO;
import ptknow.dto.shared.CourseSummaryDTO;
import ptknow.dto.shared.UserSummaryDTO;
import ptknow.model.lesson.LessonState;
import ptknow.model.lesson.LessonType;

import java.time.Instant;
import java.util.List;

@Schema(name = "LessonDTO", description = "Rich lesson response")
public record LessonDTO(
        @Schema(example = "73")
        Long id,
        @Schema(example = "Dependency Injection")
        String name,
        @Schema(example = "Introduction to dependency injection in Spring")
        String description,
        @Schema(example = "# Dependency Injection\nWe will cover constructor injection and bean scopes.")
        String contentMd,
        @Schema(example = "2026-03-22T10:00:00Z")
        Instant beginAt,
        @Schema(example = "2026-03-22T11:30:00Z")
        Instant endsAt,
        @Schema(example = "PLANNED")
        LessonState state,
        @Schema(example = "42")
        Long courseId,
        CourseSummaryDTO course,
        @Schema(example = "LECTURE")
        LessonType type,
        UserSummaryDTO owner,
        List<FileMetaDTO> materials
) {}
