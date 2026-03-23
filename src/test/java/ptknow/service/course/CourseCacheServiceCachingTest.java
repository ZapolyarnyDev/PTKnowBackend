package ptknow.service.course;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ptknow.config.CacheConfig;
import ptknow.dto.common.PageResponseDTO;
import ptknow.dto.course.CourseDTO;
import ptknow.mapper.course.CourseMapper;
import ptknow.model.auth.Auth;
import ptknow.model.auth.Role;
import ptknow.model.course.Course;
import ptknow.model.course.CourseState;
import ptknow.repository.course.CourseRepository;
import ptknow.repository.enrollment.EnrollmentRepository;
import ptknow.repository.lesson.LessonRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(CourseCacheServiceCachingTest.TestConfig.class)
class CourseCacheServiceCachingTest {

    @Configuration
    @EnableCaching(proxyTargetClass = true)
    static class TestConfig {
        @Bean
        CourseRepository courseRepository() {
            return mock(CourseRepository.class);
        }

        @Bean
        LessonRepository lessonRepository() {
            return mock(LessonRepository.class);
        }

        @Bean
        EnrollmentRepository enrollmentRepository() {
            return mock(EnrollmentRepository.class);
        }

        @Bean
        CourseMapper courseMapper() {
            return mock(CourseMapper.class);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    CacheConfig.COURSE_BY_ID_CACHE,
                    CacheConfig.COURSE_BY_HANDLE_CACHE,
                    CacheConfig.COURSE_PUBLIC_LIST_CACHE
            );
        }

        @Bean
        CourseCacheService courseCacheService(
                CourseRepository courseRepository,
                LessonRepository lessonRepository,
                EnrollmentRepository enrollmentRepository,
                CourseMapper courseMapper,
                CacheManager cacheManager
        ) {
            return new CourseCacheService(courseRepository, lessonRepository, enrollmentRepository, courseMapper, cacheManager);
        }
    }

    @jakarta.annotation.Resource
    CourseCacheService courseCacheService;

    @jakarta.annotation.Resource
    CourseRepository courseRepository;

    @jakarta.annotation.Resource
    LessonRepository lessonRepository;

    @jakarta.annotation.Resource
    EnrollmentRepository enrollmentRepository;

    @jakarta.annotation.Resource
    CourseMapper courseMapper;

    @jakarta.annotation.Resource
    CacheManager cacheManager;

    Course course;
    CourseDTO dto;

    @BeforeEach
    void setUp() {
        reset(courseRepository, lessonRepository, enrollmentRepository, courseMapper);
        cacheManager.getCache(CacheConfig.COURSE_BY_ID_CACHE).clear();
        cacheManager.getCache(CacheConfig.COURSE_BY_HANDLE_CACHE).clear();
        cacheManager.getCache(CacheConfig.COURSE_PUBLIC_LIST_CACHE).clear();

        Auth owner = Auth.builder()
                .email("owner@example.com")
                .password("password")
                .role(Role.TEACHER)
                .build();

        course = Course.builder()
                .id(42L)
                .name("Java Backend Basics")
                .description("desc")
                .handle("java-backend-basics")
                .owner(owner)
                .state(CourseState.PUBLISHED)
                .build();

        dto = new CourseDTO(
                42L,
                "Java Backend Basics",
                "desc",
                List.of(),
                "java-backend-basics",
                CourseState.PUBLISHED,
                null,
                null,
                10,
                3,
                7,
                1,
                null,
                List.of()
        );
    }

    @Test
    void getPublishedByIdShouldUseCache() {
        when(courseRepository.findViewById(42L)).thenReturn(Optional.of(course));
        when(lessonRepository.countByCourseIds(Set.of(42L))).thenReturn(java.util.Collections.singletonList(new Object[]{42L, 3L}));
        when(enrollmentRepository.countByCourseIds(Set.of(42L))).thenReturn(java.util.Collections.singletonList(new Object[]{42L, 7L}));
        when(courseMapper.courseToDTO(course, 3, 7)).thenReturn(dto);

        CourseDTO first = courseCacheService.getPublishedById(42L);
        CourseDTO second = courseCacheService.getPublishedById(42L);

        assertEquals(dto, first);
        assertEquals(dto, second);
        verify(courseRepository, times(1)).findViewById(42L);
    }

    @Test
    void getPublishedByHandleShouldUseCache() {
        when(courseRepository.findViewByHandle("java-backend-basics")).thenReturn(Optional.of(course));
        when(lessonRepository.countByCourseIds(Set.of(42L))).thenReturn(java.util.Collections.singletonList(new Object[]{42L, 3L}));
        when(enrollmentRepository.countByCourseIds(Set.of(42L))).thenReturn(java.util.Collections.singletonList(new Object[]{42L, 7L}));
        when(courseMapper.courseToDTO(course, 3, 7)).thenReturn(dto);

        courseCacheService.getPublishedByHandle("java-backend-basics");
        courseCacheService.getPublishedByHandle("java-backend-basics");

        verify(courseRepository, times(1)).findViewByHandle("java-backend-basics");
    }

    @Test
    void evictShouldDropBothKeys() {
        when(courseRepository.findViewById(42L)).thenReturn(Optional.of(course), Optional.of(course));
        when(courseRepository.findViewByHandle("java-backend-basics")).thenReturn(Optional.of(course), Optional.of(course));
        when(lessonRepository.countByCourseIds(Set.of(42L))).thenReturn(java.util.Collections.singletonList(new Object[]{42L, 3L}));
        when(enrollmentRepository.countByCourseIds(Set.of(42L))).thenReturn(java.util.Collections.singletonList(new Object[]{42L, 7L}));
        when(courseMapper.courseToDTO(course, 3, 7)).thenReturn(dto);

        courseCacheService.getPublishedById(42L);
        courseCacheService.getPublishedByHandle("java-backend-basics");
        courseCacheService.evict(course);
        courseCacheService.getPublishedById(42L);
        courseCacheService.getPublishedByHandle("java-backend-basics");

        verify(courseRepository, times(2)).findViewById(42L);
        verify(courseRepository, times(2)).findViewByHandle("java-backend-basics");
    }

    @Test
    void getAnonymousPublicListShouldUseCache() {
        var pageable = PageRequest.of(0, 20);

        when(courseRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Course>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(course), pageable, 1));
        when(courseRepository.findAllListViewByIdIn(Set.of(42L))).thenReturn(List.of(course));
        when(lessonRepository.countByCourseIds(Set.of(42L))).thenReturn(java.util.Collections.singletonList(new Object[]{42L, 3L}));
        when(enrollmentRepository.countByCourseIds(Set.of(42L))).thenReturn(java.util.Collections.singletonList(new Object[]{42L, 7L}));
        when(courseMapper.courseToDTOList(List.of(course), java.util.Map.of(42L, 3), java.util.Map.of(42L, 7)))
                .thenReturn(List.of(dto));

        PageResponseDTO<CourseDTO> first = courseCacheService.getAnonymousPublicList(pageable, "java", CourseState.PUBLISHED, "backend");
        PageResponseDTO<CourseDTO> second = courseCacheService.getAnonymousPublicList(pageable, "java", CourseState.PUBLISHED, "backend");

        assertEquals(1, first.items().size());
        assertEquals(first, second);
        verify(courseRepository, times(1)).findAll(org.mockito.ArgumentMatchers.<Specification<Course>>any(), eq(pageable));
        verify(courseRepository, times(1)).findAllListViewByIdIn(Set.of(42L));
    }
}
