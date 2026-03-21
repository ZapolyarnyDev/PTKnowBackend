package ptknow.dto.shared;

import ptknow.model.course.CourseState;

public record CourseSummaryDTO(
        Long id,
        String name,
        String handle,
        CourseState state,
        String previewUrl
) {
}
