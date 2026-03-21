package ptknow.mapper.lesson;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ptknow.dto.lesson.LessonDTO;
import ptknow.mapper.ApiViewMapper;
import ptknow.model.file.attachment.resource.Purpose;
import ptknow.model.file.attachment.resource.ResourceType;
import ptknow.model.lesson.Lesson;
import ptknow.service.file.FileAttachmentService;

@Component
@RequiredArgsConstructor
public class LessonMapper {

    private final ApiViewMapper apiViewMapper;
    private final FileAttachmentService fileAttachmentService;

    public LessonDTO toDTO(Lesson lesson) {
        var materials = fileAttachmentService.findAllByResource(ResourceType.LESSON, lesson.getId().toString()).stream()
                .filter(attachment -> attachment.getPurpose() == Purpose.MATERIAL)
                .map(apiViewMapper::toFileMeta)
                .toList();

        return new LessonDTO(
                lesson.getId(),
                lesson.getName(),
                lesson.getDescription(),
                lesson.getContentMd(),
                lesson.getBeginAt(),
                lesson.getEndsAt(),
                lesson.getState(),
                lesson.getCourse().getId(),
                apiViewMapper.toCourseSummary(lesson.getCourse()),
                lesson.getType(),
                apiViewMapper.toUserSummary(lesson.getOwner()),
                materials
        );
    }
}
