package ptknow.service.profile;

import ptknow.dto.profile.ProfileUpdateDTO;
import ptknow.model.file.File;
import ptknow.model.profile.Profile;
import ptknow.model.auth.Auth;
import ptknow.exception.user.UserNotFoundException;
import ptknow.generator.handle.HandleGenerator;
import ptknow.repository.profile.ProfileRepository;
import ptknow.service.HandleService;
import ptknow.service.OwnershipService;
import ptknow.service.file.FileAttachmentService;
import ptknow.service.file.FileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;
import ptknow.model.auth.Role;
import ptknow.model.file.attachment.FileVisibility;
import ptknow.model.file.attachment.resource.Purpose;
import ptknow.model.file.attachment.resource.ResourceType;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ProfileService implements HandleService<Profile>, OwnershipService<UUID> {

    FileService fileService;
    FileAttachmentService fileAttachmentService;
    ProfileRepository repository;
    HandleGenerator handleGenerator;

    @Transactional
    public Profile createProfile(String fullName, Auth user) {
        String handle = handleGenerator.generate(repository::existsByHandle);
        var entity = Profile.builder()
                .fullName(fullName)
                .handle(handle)
                .user(user)
                .build();

        return repository.save(entity);
    }

    @Transactional
    public Profile update(UUID userId, ProfileUpdateDTO dto) {
        Profile profile = repository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (dto.fullName() != null)
            profile.setFullName(dto.fullName());
        if (dto.summary() != null)
            profile.setSummary(dto.summary());
        if (dto.handle() != null)
            profile.setHandle(dto.handle());

        return repository.save(profile);
    }

    @Transactional(readOnly = true)
    public Profile getProfile(UUID userId, Auth initiator) {
        Profile profile = getProfile(userId);

        if (!canManageProfile(profile, initiator))
            throw new AccessDeniedException("You don't have permissions to view this profile");

        return profile;
    }

    @Transactional(rollbackFor = Exception.class)
    public Profile updateAvatar(UUID userId, MultipartFile file) throws IOException {
        Profile profile = getProfile(userId);
        File oldAvatar = profile.getAvatar();

        File savedFile = fileService.saveFile(file);
        profile.setAvatar(savedFile);
        Profile updatedProfile = repository.save(profile);

        fileAttachmentService.attach(
                savedFile,
                ResourceType.PROFILE,
                profile.getId().toString(),
                Purpose.AVATAR,
                FileVisibility.PUBLIC,
                profile.getUser()
        );

        if (oldAvatar != null) {
            fileAttachmentService.deleteAllByFileId(oldAvatar.getId());
            fileService.deleteFile(oldAvatar.getId());
        }

        return updatedProfile;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAvatar(UUID userId) throws IOException {
        Profile profile = getProfile(userId);
        File oldAvatar = profile.getAvatar();

        if (oldAvatar == null) {
            return;
        }

        profile.setAvatar(null);
        repository.save(profile);

        fileAttachmentService.deleteAllByFileId(oldAvatar.getId());
        fileService.deleteFile(oldAvatar.getId());
    }

    @Transactional(readOnly = true)
    @Override
    public Profile getByHandle(String handle) {
        return repository.findByHandle(handle)
                .orElseThrow(() -> new UserNotFoundException(handle));
    }

    @Transactional(readOnly = true)
    @Override
    public Profile seeByHandle(String handle, Auth initiator) {
        Profile profile = getByHandle(handle);
        if (!canSeeProfile(initiator)) {
            throw new AccessDeniedException("You don't have permissions to view this profile");
        }
        return profile;
    }

    @Transactional(readOnly = true)
    public Profile getProfile(UUID userId) {
        return repository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public boolean isOwner(UUID resourceId, Auth auth) {
        return getProfile(resourceId).equals(auth.getProfile());
    }

    @Override
    public Auth getOwner(UUID resourceId) {
        return getProfile(resourceId).getUser();
    }

    private boolean canSeeProfile(Auth initiator) {
        if (initiator == null || initiator.getRole() == null) {
            return false;
        }

        Role role = initiator.getRole();
        return role == Role.GUEST
                || role == Role.STUDENT
                || role == Role.TEACHER
                || role == Role.ADMIN;
    }

    private boolean canManageProfile(Profile profile, Auth initiator) {
        if (initiator == null) {
            return false;
        }

        return initiator.getRole() == Role.ADMIN ||
                profile.getUser().getId().equals(initiator.getId());
    }
}

