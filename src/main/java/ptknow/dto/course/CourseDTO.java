package ptknow.dto.course;

import ptknow.dto.file.FileMetaDTO;
import ptknow.dto.shared.UserSummaryDTO;
import ptknow.model.course.CourseState;

import java.util.List;

public record CourseDTO(
        Long id,
        String name,
        String description,
        List<String> tags,
        String handle,
        CourseState state,
        String previewUrl,
        FileMetaDTO preview,
        Integer maxUsersAmount,
        Integer lessonsCount,
        Integer studentsCount,
        Integer teachersCount,
        UserSummaryDTO owner,
        List<UserSummaryDTO> editors
) { }


