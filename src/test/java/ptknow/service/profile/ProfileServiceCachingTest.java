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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import ptknow.config.CacheConfig;
import ptknow.dto.profile.ProfileDetailsDTO;
import ptknow.dto.profile.ProfileUpdateDTO;
import ptknow.generator.handle.HandleGenerator;
import ptknow.mapper.ApiViewMapper;
import ptknow.mapper.profile.ProfileMapper;
import ptknow.model.auth.Auth;
import ptknow.model.auth.Role;
import ptknow.model.course.Course;
import ptknow.model.course.CourseState;
import ptknow.model.file.File;
import ptknow.model.profile.Profile;
import ptknow.repository.course.CourseRepository;
import ptknow.repository.enrollment.EnrollmentRepository;
import ptknow.repository.profile.ProfileRepository;
import ptknow.service.file.FileAttachmentService;
import ptknow.service.file.FileService;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(ProfileServiceCachingTest.TestConfig.class)
class ProfileServiceCachingTest {

    @Configuration
    @EnableCaching(proxyTargetClass = true)
    static class TestConfig {
        @Bean ProfileRepository profileRepository() { return mock(ProfileRepository.class); }
        @Bean FileService fileService() { return mock(FileService.class); }
        @Bean FileAttachmentService fileAttachmentService() { return mock(FileAttachmentService.class); }
        @Bean HandleGenerator handleGenerator() { return mock(HandleGenerator.class); }
        @Bean CourseRepository courseRepository() { return mock(CourseRepository.class); }
        @Bean EnrollmentRepository enrollmentRepository() { return mock(EnrollmentRepository.class); }
        @Bean ApiViewMapper apiViewMapper() { return new ApiViewMapper(); }
        @Bean ProfileMapper profileMapper(ApiViewMapper apiViewMapper) { return new ProfileMapper(apiViewMapper); }
        @Bean CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CacheConfig.PROFILE_BY_HANDLE_CACHE, CacheConfig.PROFILE_SEARCH_CACHE);
        }

        @Bean
        ProfileService profileService(
                FileService fileService,
                FileAttachmentService fileAttachmentService,
                ProfileRepository profileRepository,
                HandleGenerator handleGenerator,
                CacheManager cacheManager,
                CourseRepository courseRepository,
                EnrollmentRepository enrollmentRepository,
                ProfileMapper profileMapper,
                ApiViewMapper apiViewMapper
        ) {
            return new ProfileService(
                    fileService,
                    fileAttachmentService,
                    profileRepository,
                    handleGenerator,
                    cacheManager,
                    courseRepository,
                    enrollmentRepository,
                    profileMapper,
                    apiViewMapper
            );
        }
    }

    @jakarta.annotation.Resource ProfileService profileService;
    @jakarta.annotation.Resource ProfileRepository profileRepository;
    @jakarta.annotation.Resource FileService fileService;
    @jakarta.annotation.Resource FileAttachmentService fileAttachmentService;
    @jakarta.annotation.Resource CourseRepository courseRepository;
    @jakarta.annotation.Resource EnrollmentRepository enrollmentRepository;
    @jakarta.annotation.Resource CacheManager cacheManager;

    UUID userId;
    Auth user;
    Profile profile;

    @BeforeEach
    void setUp() {
        reset(profileRepository, fileService, fileAttachmentService, courseRepository, enrollmentRepository);
        cacheManager.getCache(CacheConfig.PROFILE_BY_HANDLE_CACHE).clear();
        cacheManager.getCache(CacheConfig.PROFILE_SEARCH_CACHE).clear();

        userId = UUID.randomUUID();
        user = Auth.builder().email("artem@example.com").password("password").role(Role.STUDENT).build();
        ReflectionTestUtils.setField(user, "id", userId);

        profile = Profile.builder()
                .id(UUID.randomUUID())
                .fullName("Artem Artemov")
                .handle("artemovv")
                .summary("summary")
                .user(user)
                .build();

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.findByHandle("artemovv")).thenReturn(Optional.of(profile));
        when(courseRepository.findAllTeachingViewByUserId(userId)).thenReturn(List.of());
        when(courseRepository.findAllPublishedTeachingViewByUserId(userId)).thenReturn(List.of());
        when(enrollmentRepository.findAllCourseViewsByUserId(userId)).thenReturn(List.of());
        when(enrollmentRepository.findAllPublishedCourseViewsByUserId(userId)).thenReturn(List.of());
    }

    @Test
    void getByHandleShouldUseCache() {
        profileService.getByHandle("artemovv");
        profileService.getByHandle("artemovv");
        verify(profileRepository, times(1)).findByHandle("artemovv");
    }

    @Test
    void searchShouldUseCache() {
        Pageable pageable = PageRequest.of(0, 20);
        when(profileRepository.findAll(ArgumentMatchers.<Specification<Profile>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(profile), pageable, 1));

        profileService.search(null, pageable, "artem");
        profileService.search(null, pageable, "artem");

        verify(profileRepository, times(1)).findAll(ArgumentMatchers.<Specification<Profile>>any(), eq(pageable));
    }

    @Test
    void updateShouldEvictHandleAndSearchCaches() {
        Pageable pageable = PageRequest.of(0, 20);
        when(profileRepository.findByHandle("artemovv")).thenReturn(Optional.of(profile), Optional.empty());
        when(profileRepository.findAll(ArgumentMatchers.<Specification<Profile>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(profile), pageable, 1), new PageImpl<>(List.of(profile), pageable, 1));
        when(profileRepository.save(ArgumentMatchers.any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(profileRepository.findByHandle("artem_new")).thenAnswer(invocation -> Optional.of(profile));

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
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3});
        File savedFile = File.builder()
                .id(UUID.randomUUID())
                .originalFilename("avatar.png")
                .contentType("image/png")
                .storagePath("avatars/avatar.png")
                .uploadedAt(Instant.now())
                .build();

        when(profileRepository.findByHandle("artemovv")).thenReturn(Optional.of(profile), Optional.of(profile));
        when(fileService.saveFile(file)).thenReturn(savedFile);
        when(fileService.getRequiredFile(savedFile.getId())).thenReturn(savedFile);
        when(profileRepository.save(ArgumentMatchers.any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        profileService.getByHandle("artemovv");
        profileService.updateAvatar(userId, file);
        profileService.getByHandle("artemovv");

        verify(profileRepository, times(2)).findByHandle("artemovv");
        verify(fileService, times(1)).getRequiredFile(savedFile.getId());
        verify(fileAttachmentService, times(1)).attach(eq(savedFile), any(), anyString(), any(), any(), eq(user));
    }

    @Test
    void getOwnProfileDetailsShouldIncludeAllTeachingAndEnrolledCourses() {
        user.setRole(Role.TEACHER);
        Course ownedCourse = Course.builder().id(1L).name("Draft Teaching").description("draft").handle("draft-teaching").owner(user).state(CourseState.DRAFT).build();
        Course enrolledCourse = Course.builder().id(2L).name("Archived Enrollment").description("archived").handle("archived-enrollment").owner(user).state(CourseState.ARCHIVED).build();

        when(courseRepository.findAllTeachingViewByUserId(userId)).thenReturn(List.of(ownedCourse));
        when(enrollmentRepository.findAllCourseViewsByUserId(userId)).thenReturn(List.of(enrolledCourse));

        ProfileDetailsDTO dto = profileService.getOwnProfileDetails(userId);

        assertEquals(Role.TEACHER, dto.role());
        assertEquals(1, dto.teachingCourses().size());
        assertEquals("draft-teaching", dto.teachingCourses().getFirst().handle());
        assertEquals(1, dto.enrolledCourses().size());
        assertEquals("archived-enrollment", dto.enrolledCourses().getFirst().handle());
    }

    @Test
    void getPublicProfileDetailsShouldUseOnlyPublishedCourses() {
        user.setRole(Role.TEACHER);
        Course publishedTeaching = Course.builder().id(3L).name("Published Teaching").description("published").handle("published-teaching").owner(user).state(CourseState.PUBLISHED).build();
        Course publishedEnrollment = Course.builder().id(4L).name("Published Enrollment").description("published").handle("published-enrollment").owner(user).state(CourseState.PUBLISHED).build();

        when(courseRepository.findAllPublishedTeachingViewByUserId(userId)).thenReturn(List.of(publishedTeaching));
        when(enrollmentRepository.findAllPublishedCourseViewsByUserId(userId)).thenReturn(List.of(publishedEnrollment));

        ProfileDetailsDTO dto = profileService.getPublicProfileDetails("artemovv");

        assertEquals(1, dto.teachingCourses().size());
        assertEquals("published-teaching", dto.teachingCourses().getFirst().handle());
        assertEquals(1, dto.enrolledCourses().size());
        assertEquals("published-enrollment", dto.enrolledCourses().getFirst().handle());
        verify(courseRepository, never()).findAllTeachingViewByUserId(userId);
        verify(enrollmentRepository, never()).findAllCourseViewsByUserId(userId);
    }
}
