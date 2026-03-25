package ptknow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ptknow.properties.FileStorageProperties;
import ptknow.properties.S3StorageProperties;
import ptknow.service.file.storage.FileStorage;
import ptknow.service.file.storage.LocalFileStorage;
import ptknow.service.file.storage.S3FileStorage;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

@Configuration
public class FileStorageConfig {

    @Bean
    public FileStorage fileStorage(FileStorageProperties properties) {
        return switch (properties.getType()) {
            case LOCAL -> new LocalFileStorage(properties);
            case S3 -> new S3FileStorage(buildS3Client(properties.getS3()), properties);
        };
    }

    private S3Client buildS3Client(S3StorageProperties properties) {
        validateS3Properties(properties);

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())
                ))
                .forcePathStyle(properties.isPathStyleAccessEnabled());

        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        return builder.build();
    }

    private void validateS3Properties(S3StorageProperties properties) {
        require(properties.getRegion(), "app.file.s3.region");
        require(properties.getBucket(), "app.file.s3.bucket");
        require(properties.getAccessKey(), "app.file.s3.access-key");
        require(properties.getSecretKey(), "app.file.s3.secret-key");
    }

    private void require(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required S3 property is missing: " + propertyName);
        }
    }
}

