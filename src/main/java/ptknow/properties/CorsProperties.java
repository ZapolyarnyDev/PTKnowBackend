package ptknow.properties;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.cors")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CorsProperties {

    List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));
}
