package ptknow.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ptknow.properties.ProfileCacheProperties;

import java.util.List;

@Configuration
@EnableCaching
@EnableConfigurationProperties(ProfileCacheProperties.class)
public class CacheConfig {

    public static final String PROFILE_BY_HANDLE_CACHE = "profileByHandle";
    public static final String PROFILE_SEARCH_CACHE = "profileSearch";

    @Bean
    public CacheManager cacheManager(ProfileCacheProperties properties) {
        CaffeineCache profileByHandle = new CaffeineCache(
                PROFILE_BY_HANDLE_CACHE,
                Caffeine.newBuilder()
                        .maximumSize(properties.getByHandleMaxSize())
                        .expireAfterWrite(properties.getByHandleTtl())
                        .build()
        );

        CaffeineCache profileSearch = new CaffeineCache(
                PROFILE_SEARCH_CACHE,
                Caffeine.newBuilder()
                        .maximumSize(properties.getSearchMaxSize())
                        .expireAfterWrite(properties.getSearchTtl())
                        .build()
        );

        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(profileByHandle, profileSearch));
        return cacheManager;
    }
}
