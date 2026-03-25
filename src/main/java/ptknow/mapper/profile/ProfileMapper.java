package ptknow.mapper.profile;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ptknow.dto.profile.ProfileDetailsDTO;
import ptknow.dto.profile.ProfileResponseDTO;
import ptknow.dto.shared.CourseSummaryDTO;
import ptknow.mapper.ApiViewMapper;
import ptknow.model.profile.Profile;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProfileMapper {

    private final ApiViewMapper apiViewMapper;

    public ProfileResponseDTO toDto(Profile entity) {
        if (entity == null) {
            return null;
        }

        UUID avatarId = entity.getAvatar() != null ? entity.getAvatar().getId() : null;
        return new ProfileResponseDTO(
                entity.getFullName(),
                entity.getSummary(),
                entity.getHandle(),
                apiViewMapper.toFileUrl(avatarId)
        );
    }

    public ProfileDetailsDTO toDetailsDto(Profile entity, List<CourseSummaryDTO> enrolledCourses, List<CourseSummaryDTO> teachingCourses) {
        if (entity == null) {
            return null;
        }

        UUID avatarId = entity.getAvatar() != null ? entity.getAvatar().getId() : null;
        return new ProfileDetailsDTO(
                entity.getFullName(),
                entity.getSummary(),
                entity.getHandle(),
                apiViewMapper.toFileUrl(avatarId),
                entity.getUser() != null ? entity.getUser().getRole() : null,
                enrolledCourses,
                teachingCourses
        );
    }
}