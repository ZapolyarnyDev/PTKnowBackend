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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;
import ptknow.model.auth.Role;
import ptknow.model.file.attachment.FileVisibility;
import ptknow.model.file.attachment.resource.Purpose;
import ptknow.model.file.attachment.resource.ResourceType;
import ptknow.config.CacheConfig;
import ptknow.config.cache.ProfileCacheKeys;

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
    CacheManager cacheManager;

    @Transactional
    public Profile createProfile(String fullName, Auth user) {
        String handle = handleGenerator.generate(repository::existsByHandle);
        var entity = Profile.builder()
                .fullName(fullName)
                .handle(handle)
                .user(user)
                .build();

        Profile savedProfile = repository.save(entity);
        evictProfileSearchCache();
        return savedProfile;
    }

    @Transactional
    public Profile update(UUID userId, ProfileUpdateDTO dto) {
        Profile profile = repository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        String previousHandle = profile.getHandle();

        if (dto.fullName() != null)
            profile.setFullName(dto.fullName());
        if (dto.summary() != null)
            profile.setSummary(dto.summary());
        if (dto.handle() != null)
            profile.setHandle(dto.handle());

        Profile updatedProfile = repository.save(profile);
        evictProfileReadCaches(previousHandle, updatedProfile.getHandle());
        return updatedProfile;
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
        String handle = profile.getHandle();

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

        evictProfileReadCaches(handle, updatedProfile.getHandle());
        return updatedProfile;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAvatar(UUID userId) throws IOException {
        Profile profile = getProfile(userId);
        File oldAvatar = profile.getAvatar();
        String handle = profile.getHandle();

        if (oldAvatar == null) {
            return;
        }

        profile.setAvatar(null);
        repository.save(profile);

        fileAttachmentService.deleteAllByFileId(oldAvatar.getId());
        fileService.deleteFile(oldAvatar.getId());
        evictProfileReadCaches(handle, handle);
    }

    @Transactional(readOnly = true)
    @Override
    @Cacheable(cacheNames = CacheConfig.PROFILE_BY_HANDLE_CACHE,
            key = "T(ptknow.config.cache.ProfileCacheKeys).byHandle(#handle)")
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

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.PROFILE_SEARCH_CACHE,
            key = "T(ptknow.config.cache.ProfileCacheKeys).search(#pageable, #q)")
    public Page<Profile> search(Auth initiator, Pageable pageable, String q) {
        if (!canSeeProfile(initiator)) {
            throw new AccessDeniedException("You don't have permissions to view profiles");
        }

        return repository.findAll(ProfileSpecifications.search(q), pageable);
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
        if (initiator == null) {
            return true;
        }

        if (initiator.getRole() == null) {
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

    private void evictProfileReadCaches(String... handles) {
        Cache byHandleCache = cacheManager.getCache(CacheConfig.PROFILE_BY_HANDLE_CACHE);
        if (byHandleCache != null) {
            for (String handle : handles) {
                byHandleCache.evict(ProfileCacheKeys.byHandle(handle));
            }
        }

        evictProfileSearchCache();
    }

    private void evictProfileSearchCache() {
        Cache searchCache = cacheManager.getCache(CacheConfig.PROFILE_SEARCH_CACHE);
        if (searchCache != null) {
            searchCache.clear();
        }
    }
}

