package ptknow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ptknow.properties.FileStorageProperties;
import ptknow.service.file.storage.FileStorage;
import ptknow.service.file.storage.FileStorageType;
import ptknow.service.file.storage.LocalFileStorage;

@Configuration
public class FileStorageConfig {

    @Bean
    public FileStorage fileStorage(FileStorageProperties properties) {
        return switch (properties.getType()) {
            case LOCAL -> new LocalFileStorage(properties);
            case S3 -> throw new IllegalStateException("S3 storage is not configured yet");
        };
    }
}
