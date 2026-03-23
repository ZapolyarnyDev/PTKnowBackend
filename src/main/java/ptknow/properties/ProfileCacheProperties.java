package ptknow.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.cache.profile")
public class ProfileCacheProperties {

    @NotNull
    Duration byHandleTtl = Duration.ofMinutes(2);

    @NotNull
    Duration searchTtl = Duration.ofSeconds(45);

    @Min(1)
    long byHandleMaxSize = 1_000;

    @Min(1)
    long searchMaxSize = 200;
}
