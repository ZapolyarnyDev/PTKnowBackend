package ptknow.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.Instant;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "security.jwt")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JwtProperties {

    @NotBlank
    String issuer;

    @NotBlank
    String key;

    @NotNull
    Duration accessTokenExpiration = Duration.ofMinutes(15);

    @NotNull
    Duration refreshTokenExpiration = Duration.ofDays(7);

    @NotNull
    Boolean refreshCookieSecure = false;

    @NotBlank
    String refreshCookieSameSite = "Lax";

    public Instant getAccessTokenExpiryInstant() {
        return Instant.now().plus(accessTokenExpiration);
    }

    public Instant getRefreshTokenExpiryInstant() {
        return Instant.now().plus(refreshTokenExpiration);
    }
}

