package ptknow.mapper.course;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ptknow.dto.course.CourseDTO;
import ptknow.mapper.ApiViewMapper;
import ptknow.model.course.Course;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CourseMapper {

    private final ApiViewMapper apiViewMapper;

    public CourseDTO courseToDTO(Course entity) {
        return courseToDTO(entity, entity.getLessons().size(), entity.getEnrollments().size());
    }

    public List<CourseDTO> courseToDTOList(List<Course> courses, Map<Long, Integer> lessonCounts, Map<Long, Integer> enrollmentCounts) {
        return courses.stream()
                .map(course -> courseToDTO(
                        course,
                        lessonCounts.getOrDefault(course.getId(), 0),
                        enrollmentCounts.getOrDefault(course.getId(), 0)
                ))
                .toList();
    }

    private CourseDTO courseToDTO(Course entity, int lessonsCount, int studentsCount) {
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
                lessonsCount,
                studentsCount,
                entity.getEditors().size() + 1,
                apiViewMapper.toUserSummary(entity.getOwner()),
                entity.getEditors().stream()
                        .map(apiViewMapper::toUserSummary)
                        .toList()
        );
    }
}
