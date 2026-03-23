package ptknow.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "security.rate-limit")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Validated
public class AuthRateLimitProperties {

    @NotNull
    Boolean enabled = true;

    @Valid
    @NotNull
    Rule login = new Rule(10, Duration.ofMinutes(1));

    @Valid
    @NotNull
    Rule register = new Rule(5, Duration.ofMinutes(5));

    @Valid
    @NotNull
    Rule refresh = new Rule(30, Duration.ofMinutes(1));

    @Valid
    @NotNull
    Rule publicCourseList = new Rule(60, Duration.ofMinutes(1));

    @Valid
    @NotNull
    Rule publicCourseRead = new Rule(120, Duration.ofMinutes(1));

    @Valid
    @NotNull
    Rule publicLessonRead = new Rule(180, Duration.ofMinutes(1));

    @Valid
    @NotNull
    Rule publicProfileSearch = new Rule(30, Duration.ofMinutes(1));

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Rule {
        @Min(1)
        int maxRequests;

        @NotNull
        Duration window;

        public Rule() {
        }

        public Rule(int maxRequests, Duration window) {
            this.maxRequests = maxRequests;
            this.window = window;
        }
    }
}
