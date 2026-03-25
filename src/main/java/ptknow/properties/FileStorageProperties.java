package ptknow.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import ptknow.service.file.storage.FileStorageType;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.file")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileStorageProperties {

    FileStorageType type = FileStorageType.LOCAL;

    @NotBlank
    String uploadDir;

    @Positive
    long maxFileSizeBytes = 10 * 1024 * 1024;
}
