package ptknow.mapper.lesson;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ptknow.dto.file.FileMetaDTO;
import ptknow.dto.lesson.LessonDTO;
import ptknow.mapper.ApiViewMapper;
import ptknow.model.file.attachment.FileAttachment;
import ptknow.model.file.attachment.resource.Purpose;
import ptknow.model.file.attachment.resource.ResourceType;
import ptknow.model.lesson.Lesson;
import ptknow.service.file.FileAttachmentService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

        return toDTO(lesson, materials);
    }

    public List<LessonDTO> toDTOList(List<Lesson> lessons) {
        Set<String> lessonIds = lessons.stream()
                .map(Lesson::getId)
                .map(String::valueOf)
                .collect(Collectors.toSet());

        Map<String, List<FileAttachment>> attachmentsByLessonId =
                fileAttachmentService.findAllByResourceGrouped(ResourceType.LESSON, lessonIds);

        return lessons.stream()
                .map(lesson -> {
                    var materials = attachmentsByLessonId.getOrDefault(String.valueOf(lesson.getId()), List.of()).stream()
                            .filter(attachment -> attachment.getPurpose() == Purpose.MATERIAL)
                            .map(apiViewMapper::toFileMeta)
                            .toList();
                    return toDTO(lesson, materials);
                })
                .toList();
    }

    private LessonDTO toDTO(Lesson lesson, List<FileMetaDTO> materials) {
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
