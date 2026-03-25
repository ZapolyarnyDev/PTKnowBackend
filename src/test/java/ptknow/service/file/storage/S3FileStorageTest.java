package ptknow.service.file.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import ptknow.properties.FileStorageProperties;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3FileStorageTest {

    @Mock
    S3Client s3Client;

    @Test
    void storeShouldUseBucketAndKeyPrefix() throws Exception {
        FileStorageProperties properties = properties();
        properties.getS3().setBucket("ptknow-files");
        properties.getS3().setKeyPrefix("lesson-materials");
        S3FileStorage storage = new S3FileStorage(s3Client, properties);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "plan.pdf",
                "application/pdf",
                "content".getBytes(StandardCharsets.UTF_8)
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        UUID fileId = UUID.randomUUID();
        FileStorage.StoredFile storedFile = storage.store(fileId, file);

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(software.amazon.awssdk.core.sync.RequestBody.class));

        assertEquals("ptknow-files", captor.getValue().bucket());
        assertEquals("lesson-materials/" + fileId, captor.getValue().key());
        assertEquals("lesson-materials/" + fileId, storedFile.storageKey());
    }

    @Test
    void openShouldReturnStreamAndContentLength() throws Exception {
        FileStorageProperties properties = properties();
        properties.getS3().setBucket("ptknow-files");
        S3FileStorage storage = new S3FileStorage(s3Client, properties);

        byte[] content = "content".getBytes(StandardCharsets.UTF_8);
        ResponseInputStream<GetObjectResponse> response = new ResponseInputStream<>(
                GetObjectResponse.builder().contentLength((long) content.length).build(),
                AbortableInputStream.create(new ByteArrayInputStream(content))
        );

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(response);

        FileStorage.OpenedFile openedFile = storage.open("test-key");

        assertSame(response, openedFile.inputStream());
        assertEquals(content.length, openedFile.size());
    }

    @Test
    void sizeShouldUseHeadObject() throws Exception {
        FileStorageProperties properties = properties();
        properties.getS3().setBucket("ptknow-files");
        S3FileStorage storage = new S3FileStorage(s3Client, properties);

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentLength(42L).build());

        assertEquals(42L, storage.size("test-key"));
    }

    @Test
    void deleteShouldTargetBucketAndKey() throws Exception {
        FileStorageProperties properties = properties();
        properties.getS3().setBucket("ptknow-files");
        S3FileStorage storage = new S3FileStorage(s3Client, properties);

        storage.delete("test-key");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());

        assertEquals("ptknow-files", captor.getValue().bucket());
        assertEquals("test-key", captor.getValue().key());
    }

    private FileStorageProperties properties() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.getS3().setRegion("us-east-1");
        properties.getS3().setAccessKey("minio");
        properties.getS3().setSecretKey("minio123");
        properties.getS3().setPathStyleAccessEnabled(true);
        return properties;
    }
}
