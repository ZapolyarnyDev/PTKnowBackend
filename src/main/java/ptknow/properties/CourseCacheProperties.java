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
@ConfigurationProperties(prefix = "app.cache.course")
public class CourseCacheProperties {

    @NotNull
    Duration byIdTtl = Duration.ofMinutes(2);

    @NotNull
    Duration byHandleTtl = Duration.ofMinutes(2);

    @NotNull
    Duration publicListTtl = Duration.ofSeconds(30);

    @Min(1)
    long byIdMaxSize = 1_000;

    @Min(1)
    long byHandleMaxSize = 1_000;

    @Min(1)
    long publicListMaxSize = 200;
}
