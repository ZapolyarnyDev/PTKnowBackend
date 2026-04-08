package ptknow.properties;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.recaptcha")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecaptchaProperties {

    boolean enabled = false;

    String secretKey = "";

    String verifyUrl = "https://www.google.com/recaptcha/api/siteverify";

    double scoreThreshold = 0.5d;
}
