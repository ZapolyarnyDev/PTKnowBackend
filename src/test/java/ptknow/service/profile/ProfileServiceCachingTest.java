package ptknow.service.profile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.mock.web.MockMultipartFile;
import ptknow.config.CacheConfig;
import ptknow.dto.profile.ProfileUpdateDTO;
import ptknow.generator.handle.HandleGenerator;
import ptknow.model.auth.Auth;
import ptknow.model.auth.Role;
import ptknow.model.file.File;
import ptknow.model.profile.Profile;
import ptknow.repository.profile.ProfileRepository;
import ptknow.service.file.FileAttachmentService;
import ptknow.service.file.FileService;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(ProfileServiceCachingTest.TestConfig.class)
class ProfileServiceCachingTest {

    @Configuration
    @EnableCaching(proxyTargetClass = true)
    static class TestConfig {
        @Bean
        ProfileRepository profileRepository() {
            return mock(ProfileRepository.class);
        }

        @Bean
        FileService fileService() {
            return mock(FileService.class);
        }

        @Bean
        FileAttachmentService fileAttachmentService() {
            return mock(FileAttachmentService.class);
        }

        @Bean
        HandleGenerator handleGenerator() {
            return mock(HandleGenerator.class);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    CacheConfig.PROFILE_BY_HANDLE_CACHE,
                    CacheConfig.PROFILE_SEARCH_CACHE
            );
        }

        @Bean
        ProfileService profileService(
                FileService fileService,
                FileAttachmentService fileAttachmentService,
                ProfileRepository profileRepository,
                HandleGenerator handleGenerator,
                CacheManager cacheManager
        ) {
            return new ProfileService(fileService, fileAttachmentService, profileRepository, handleGenerator, cacheManager);
        }
    }

    @jakarta.annotation.Resource
    ProfileService profileService;

    @jakarta.annotation.Resource
    ProfileRepository profileRepository;

    @jakarta.annotation.Resource
    FileService fileService;

    @jakarta.annotation.Resource
    FileAttachmentService fileAttachmentService;

    @jakarta.annotation.Resource
    CacheManager cacheManager;

    UUID userId;
    Auth user;
    Profile profile;

    @BeforeEach
    void setUp() {
        reset(profileRepository, fileService, fileAttachmentService);
        cacheManager.getCache(CacheConfig.PROFILE_BY_HANDLE_CACHE).clear();
        cacheManager.getCache(CacheConfig.PROFILE_SEARCH_CACHE).clear();

        userId = UUID.randomUUID();
        user = Auth.builder()
                .email("artem@example.com")
                .password("password")
                .role(Role.STUDENT)
                .build();

        profile = Profile.builder()
                .id(UUID.randomUUID())
                .fullName("Artem Artemov")
                .handle("artemovv")
                .summary("summary")
                .user(user)
                .build();
    }

    @Test
    void getByHandleShouldUseCache() {
        when(profileRepository.findByHandle("artemovv")).thenReturn(Optional.of(profile));

        profileService.getByHandle("artemovv");
        profileService.getByHandle("artemovv");

        verify(profileRepository, times(1)).findByHandle("artemovv");
    }

    @Test
    void searchShouldUseCache() {
        Pageable pageable = PageRequest.of(0, 20);
        when(profileRepository.findAll(ArgumentMatchers.<Specification<Profile>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(java.util.List.of(profile), pageable, 1));

        profileService.search(null, pageable, "artem");
        profileService.search(null, pageable, "artem");

        verify(profileRepository, times(1)).findAll(ArgumentMatchers.<Specification<Profile>>any(), eq(pageable));
    }

    @Test
    void updateShouldEvictHandleAndSearchCaches() {
        Pageable pageable = PageRequest.of(0, 20);

        when(profileRepository.findByHandle("artemovv"))
                .thenReturn(Optional.of(profile), Optional.empty());
        when(profileRepository.findAll(ArgumentMatchers.<Specification<Profile>>any(), eq(pageable)))
                .thenReturn(
                        new PageImpl<>(java.util.List.of(profile), pageable, 1),
                        new PageImpl<>(java.util.List.of(profile), pageable, 1)
                );
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(ArgumentMatchers.any(Profile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(profileRepository.findByHandle("artem_new"))
                .thenAnswer(invocation -> Optional.of(profile));

        profileService.getByHandle("artemovv");
        profileService.search(null, pageable, "artem");

        profileService.update(userId, new ProfileUpdateDTO("Artem New", "updated", "artem_new"));

        assertDoesNotThrow(() -> profileService.getByHandle("artem_new"));
        assertThrows(Exception.class, () -> profileService.getByHandle("artemovv"));
        profileService.search(null, pageable, "artem");

        verify(profileRepository, times(2)).findAll(ArgumentMatchers.<Specification<Profile>>any(), eq(pageable));
        verify(profileRepository, times(2)).findByHandle("artemovv");
        verify(profileRepository, times(1)).findByHandle("artem_new");
    }

    @Test
    void updateAvatarShouldEvictHandleCache() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        File savedFile = File.builder()
                .id(UUID.randomUUID())
                .originalFilename("avatar.png")
                .contentType("image/png")
                .storagePath("avatars/avatar.png")
                .uploadedAt(Instant.now())
                .build();

        when(profileRepository.findByHandle("artemovv"))
                .thenReturn(Optional.of(profile), Optional.of(profile));
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(fileService.saveFile(file)).thenReturn(savedFile);
        when(profileRepository.save(ArgumentMatchers.any(Profile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        profileService.getByHandle("artemovv");
        profileService.updateAvatar(userId, file);
        profileService.getByHandle("artemovv");

        verify(profileRepository, times(2)).findByHandle("artemovv");
        verify(fileAttachmentService, times(1))
                .attach(eq(savedFile), any(), anyString(), any(), any(), eq(user));
    }
}
