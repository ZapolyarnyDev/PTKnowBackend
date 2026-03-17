package ptknow.dto.course;

import ptknow.model.course.CourseState;

import java.util.List;

public record CourseDTO(
        Long id,
        String name,
        String description,
        List<String> tags,
        String handle,
        CourseState state,
        String previewUrl
) { }


