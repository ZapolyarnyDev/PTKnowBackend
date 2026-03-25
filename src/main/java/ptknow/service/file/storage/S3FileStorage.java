package ptknow.service.file.storage;

import org.springframework.web.multipart.MultipartFile;
import ptknow.properties.FileStorageProperties;
import ptknow.properties.S3StorageProperties;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

public class S3FileStorage implements FileStorage {

    private final S3Client s3Client;
    private final S3StorageProperties properties;

    public S3FileStorage(S3Client s3Client, FileStorageProperties fileStorageProperties) {
        this.s3Client = s3Client;
        this.properties = fileStorageProperties.getS3();
    }

    @Override
    public StoredFile store(UUID fileId, MultipartFile file) throws IOException {
        String storageKey = buildStorageKey(fileId);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(required(properties.getBucket(), "app.file.s3.bucket"))
                .key(storageKey)
                .contentType(file.getContentType())
                .build();

        try (var inputStream = file.getInputStream()) {
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, file.getSize()));
            return new StoredFile(storageKey);
        } catch (S3Exception | SdkClientException e) {
            throw new IOException("Failed to store file in S3", e);
        }
    }

    @Override
    public OpenedFile open(String storageKey) throws IOException {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(required(properties.getBucket(), "app.file.s3.bucket"))
                .key(storageKey)
                .build();

        try {
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
            return new OpenedFile(response, response.response().contentLength());
        } catch (NoSuchKeyException e) {
            throw new IOException("Stored file content not found", e);
        } catch (S3Exception | SdkClientException e) {
            if (e instanceof S3Exception s3Exception && s3Exception.statusCode() == 404) {
                throw new IOException("Stored file content not found", e);
            }
            throw new IOException("Failed to open file from S3", e);
        }
    }

    @Override
    public long size(String storageKey) throws IOException {
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(required(properties.getBucket(), "app.file.s3.bucket"))
                .key(storageKey)
                .build();

        try {
            return s3Client.headObject(request).contentLength();
        } catch (S3Exception | SdkClientException e) {
            if (e instanceof S3Exception s3Exception && s3Exception.statusCode() == 404) {
                throw new IOException("Stored file content not found", e);
            }
            throw new IOException("Failed to read file metadata from S3", e);
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(required(properties.getBucket(), "app.file.s3.bucket"))
                .key(storageKey)
                .build();

        try {
            s3Client.deleteObject(request);
        } catch (S3Exception | SdkClientException e) {
            throw new IOException("Failed to delete file from S3", e);
        }
    }

    private String buildStorageKey(UUID fileId) {
        String prefix = properties.getKeyPrefix();
        if (prefix == null || prefix.isBlank()) {
            return fileId.toString();
        }

        String normalizedPrefix = prefix.strip();
        while (normalizedPrefix.startsWith("/")) {
            normalizedPrefix = normalizedPrefix.substring(1);
        }
        while (normalizedPrefix.endsWith("/")) {
            normalizedPrefix = normalizedPrefix.substring(0, normalizedPrefix.length() - 1);
        }

        if (normalizedPrefix.isBlank()) {
            return fileId.toString();
        }

        return normalizedPrefix + "/" + fileId;
    }

    private String required(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required S3 property is missing: " + propertyName);
        }
        return value;
    }
}
