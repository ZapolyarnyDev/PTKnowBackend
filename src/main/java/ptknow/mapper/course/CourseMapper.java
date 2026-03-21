package ptknow.mapper.course;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ptknow.dto.course.CourseDTO;
import ptknow.mapper.ApiViewMapper;
import ptknow.model.course.Course;

@Component
@RequiredArgsConstructor
public class CourseMapper {

    private final ApiViewMapper apiViewMapper;

    public CourseDTO courseToDTO(Course entity) {
        return new CourseDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCourseTags().stream().map(tag -> tag.getTagName()).toList(),
                entity.getHandle(),
                entity.getState(),
                entity.getPreview() != null ? apiViewMapper.toFileUrl(entity.getPreview().getId()) : null,
                apiViewMapper.toFileMeta(entity.getPreview()),
                entity.getMaxUsersAmount(),
                entity.getLessons().size(),
                entity.getEnrollments().size(),
                entity.getEditors().size() + 1,
                apiViewMapper.toUserSummary(entity.getOwner()),
                entity.getEditors().stream()
                        .map(apiViewMapper::toUserSummary)
                        .toList()
        );
    }
}
