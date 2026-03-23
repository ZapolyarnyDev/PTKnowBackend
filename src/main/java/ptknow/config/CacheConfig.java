package ptknow.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ptknow.properties.CourseCacheProperties;
import ptknow.properties.ProfileCacheProperties;

import java.util.List;

@Configuration
@EnableCaching
@EnableConfigurationProperties({ProfileCacheProperties.class, CourseCacheProperties.class})
public class CacheConfig {

    public static final String PROFILE_BY_HANDLE_CACHE = "profileByHandle";
    public static final String PROFILE_SEARCH_CACHE = "profileSearch";
    public static final String COURSE_BY_ID_CACHE = "courseById";
    public static final String COURSE_BY_HANDLE_CACHE = "courseByHandle";
    public static final String COURSE_PUBLIC_LIST_CACHE = "coursePublicList";

    @Bean
    public CacheManager cacheManager(
            ProfileCacheProperties profileCacheProperties,
            CourseCacheProperties courseCacheProperties
    ) {
        CaffeineCache profileByHandle = new CaffeineCache(
                PROFILE_BY_HANDLE_CACHE,
                Caffeine.newBuilder()
                        .maximumSize(profileCacheProperties.getByHandleMaxSize())
                        .expireAfterWrite(profileCacheProperties.getByHandleTtl())
                        .build()
        );

        CaffeineCache profileSearch = new CaffeineCache(
                PROFILE_SEARCH_CACHE,
                Caffeine.newBuilder()
                        .maximumSize(profileCacheProperties.getSearchMaxSize())
                        .expireAfterWrite(profileCacheProperties.getSearchTtl())
                        .build()
        );

        CaffeineCache courseById = new CaffeineCache(
                COURSE_BY_ID_CACHE,
                Caffeine.newBuilder()
                        .maximumSize(courseCacheProperties.getByIdMaxSize())
                        .expireAfterWrite(courseCacheProperties.getByIdTtl())
                        .build()
        );

        CaffeineCache courseByHandle = new CaffeineCache(
                COURSE_BY_HANDLE_CACHE,
                Caffeine.newBuilder()
                        .maximumSize(courseCacheProperties.getByHandleMaxSize())
                        .expireAfterWrite(courseCacheProperties.getByHandleTtl())
                        .build()
        );

        CaffeineCache coursePublicList = new CaffeineCache(
                COURSE_PUBLIC_LIST_CACHE,
                Caffeine.newBuilder()
                        .maximumSize(courseCacheProperties.getPublicListMaxSize())
                        .expireAfterWrite(courseCacheProperties.getPublicListTtl())
                        .build()
        );

        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                profileByHandle,
                profileSearch,
                courseById,
                courseByHandle,
                coursePublicList
        ));
        return cacheManager;
    }
}
