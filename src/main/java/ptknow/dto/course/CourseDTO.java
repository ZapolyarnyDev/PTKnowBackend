package ptknow.dto.course;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import ptknow.dto.file.FileMetaDTO;
import ptknow.dto.shared.UserSummaryDTO;
import ptknow.model.course.CourseState;

import java.util.List;

@Schema(name = "CourseDTO", description = "Rich course response")
public record CourseDTO(
        @Schema(example = "42")
        Long id,
        @Schema(example = "Java Backend Basics")
        String name,
        @Schema(example = "Spring Boot course for beginners with lessons and materials.")
        String description,
        @ArraySchema(schema = @Schema(example = "java"))
        List<String> tags,
        @Schema(example = "java-backend-basics")
        String handle,
        @Schema(example = "PUBLISHED")
        CourseState state,
        @Schema(example = "/v0/files/550e8400-e29b-41d4-a716-446655440000")
        String previewUrl,
        FileMetaDTO preview,
        @Schema(example = "25")
        Integer maxUsersAmount,
        @Schema(example = "12")
        Integer lessonsCount,
        @Schema(example = "18")
        Integer studentsCount,
        @Schema(example = "2")
        Integer teachersCount,
        UserSummaryDTO owner,
        List<UserSummaryDTO> editors
) { }


