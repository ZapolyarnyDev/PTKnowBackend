package ptknow.mapper.profile;

import ptknow.dto.profile.ProfileResponseDTO;
import ptknow.model.profile.Profile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ProfileMapper {

    @Mapping(target = "avatarUrl", source = "avatar.id", qualifiedByName = "mapAvatarIdToUrl")
    ProfileResponseDTO toDto(Profile entity);

    @Named("mapAvatarIdToUrl")
    default String mapAvatarIdToUrl(java.util.UUID avatarId) {
        if (avatarId == null) return null;
        return "/api/v0/files/" + avatarId;
    }
}

