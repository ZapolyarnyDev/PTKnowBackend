package ptknow.service.course;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptknow.config.CacheConfig;
import ptknow.config.cache.CourseCacheKeys;
import ptknow.dto.common.PageResponseDTO;
import ptknow.dto.course.CourseDTO;
import ptknow.exception.course.CourseNotFoundException;
import ptknow.mapper.course.CourseMapper;
import ptknow.model.course.Course;
import ptknow.model.course.CourseState;
import ptknow.repository.course.CourseRepository;
import ptknow.repository.enrollment.EnrollmentRepository;
import ptknow.repository.lesson.LessonRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseCacheService {

    CourseRepository courseRepository;
    LessonRepository lessonRepository;
    EnrollmentRepository enrollmentRepository;
    CourseMapper courseMapper;
    CacheManager cacheManager;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.COURSE_BY_ID_CACHE, key = "#courseId")
    public CourseDTO getPublishedById(Long courseId) {
        Course course = courseRepository.findViewById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        return toDto(course);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.COURSE_BY_HANDLE_CACHE,
            key = "T(ptknow.config.cache.CourseCacheKeys).byHandle(#handle)")
    public CourseDTO getPublishedByHandle(String handle) {
        Course course = courseRepository.findViewByHandle(handle)
                .orElseThrow(() -> new CourseNotFoundException(handle));
        return toDto(course);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.COURSE_PUBLIC_LIST_CACHE,
            key = "T(ptknow.config.cache.CourseCacheKeys).publicList(#pageable, #q, #state, #tag)")
    public PageResponseDTO<CourseDTO> getAnonymousPublicList(Pageable pageable, String q, CourseState state, String tag) {
        Page<Course> page = courseRepository.findAll(
                CourseSpecifications.visibleTo(null)
                        .and(CourseSpecifications.search(q))
                        .and(CourseSpecifications.hasState(state))
                        .and(CourseSpecifications.hasTag(tag)),
                pageable
        );

        if (page.isEmpty()) {
            return new PageResponseDTO<>(List.of(), page.getNumber(), page.getSize(), 0, 0, false);
        }

        Set<Long> courseIds = page.getContent().stream()
                .map(Course::getId)
                .collect(java.util.stream.Collectors.toSet());

        Map<Long, Course> hydratedById = courseRepository.findAllListViewByIdIn(courseIds).stream()
                .collect(java.util.stream.Collectors.toMap(Course::getId, course -> course, (left, right) -> left, LinkedHashMap::new));

        List<Course> orderedContent = page.getContent().stream()
                .map(course -> hydratedById.getOrDefault(course.getId(), course))
                .toList();

        Page<Course> hydratedPage = new PageImpl<>(orderedContent, pageable, page.getTotalElements());
        return toPageDto(hydratedPage);
    }

    @Transactional(readOnly = true)
    public CourseDTO toDto(Long courseId) {
        Course course = courseRepository.findViewById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        return toDto(course);
    }

    public void evict(Course course) {
        if (course == null) {
            return;
        }
        evict(course.getId(), course.getHandle());
    }

    public void evict(Long courseId, String handle) {
        Cache byIdCache = cacheManager.getCache(CacheConfig.COURSE_BY_ID_CACHE);
        if (byIdCache != null) {
            byIdCache.evict(courseId);
        }

        Cache byHandleCache = cacheManager.getCache(CacheConfig.COURSE_BY_HANDLE_CACHE);
        if (byHandleCache != null) {
            byHandleCache.evict(CourseCacheKeys.byHandle(handle));
        }

        evictPublicList();
    }

    public void evict(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        evict(course);
    }

    private CourseDTO toDto(Course course) {
        Set<Long> courseIds = Set.of(course.getId());
        int lessonsCount = countLessonsByCourseIds(courseIds).getOrDefault(course.getId(), 0);
        int enrollmentCount = countEnrollmentsByCourseIds(courseIds).getOrDefault(course.getId(), 0);
        return courseMapper.courseToDTO(course, lessonsCount, enrollmentCount);
    }

    private PageResponseDTO<CourseDTO> toPageDto(Page<Course> page) {
        Set<Long> courseIds = page.getContent().stream()
                .map(Course::getId)
                .collect(java.util.stream.Collectors.toSet());

        Map<Long, Integer> lessonCounts = countLessonsByCourseIds(courseIds);
        Map<Long, Integer> enrollmentCounts = countEnrollmentsByCourseIds(courseIds);

        return new PageResponseDTO<>(
                courseMapper.courseToDTOList(page.getContent(), lessonCounts, enrollmentCounts),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }

    private Map<Long, Integer> countLessonsByCourseIds(Set<Long> courseIds) {
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (Object[] row : lessonRepository.countByCourseIds(courseIds)) {
            result.put((Long) row[0], ((Long) row[1]).intValue());
        }
        return result;
    }

    private Map<Long, Integer> countEnrollmentsByCourseIds(Set<Long> courseIds) {
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (Object[] row : enrollmentRepository.countByCourseIds(courseIds)) {
            result.put((Long) row[0], ((Long) row[1]).intValue());
        }
        return result;
    }

    private void evictPublicList() {
        Cache publicListCache = cacheManager.getCache(CacheConfig.COURSE_PUBLIC_LIST_CACHE);
        if (publicListCache != null) {
            publicListCache.clear();
        }
    }
}
