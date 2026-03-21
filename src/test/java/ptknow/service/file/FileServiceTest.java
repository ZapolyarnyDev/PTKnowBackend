package ptknow.service.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ptknow.model.file.File;
import ptknow.exception.file.InvalidFileUploadException;
import ptknow.properties.FileStorageProperties;
import ptknow.repository.file.FileRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    FileRepository fileRepository;

    FileStorageProperties properties = new FileStorageProperties();

    @InjectMocks
    FileService fileService;

    @TempDir
    Path tempDir;

    @Test
    void saveFileShouldDeletePhysicalCopyWhenRepositorySaveFails() throws Exception {
        properties.setUploadDir(tempDir.toString());
        fileService = new FileService(fileRepository, properties);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "lesson.md",
                "text/markdown",
                "# content".getBytes()
        );

        when(fileRepository.save(any(File.class))).thenThrow(new RuntimeException("db failure"));

        assertThrows(RuntimeException.class, () -> fileService.saveFile(multipartFile));
        assertEquals(0, Files.list(tempDir).count());
    }

    @Test
    void saveFileShouldRejectEmptyUpload() {
        properties.setUploadDir(tempDir.toString());
        fileService = new FileService(fileRepository, properties);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "lesson.md",
                "text/markdown",
                new byte[0]
        );

        assertThrows(InvalidFileUploadException.class, () -> fileService.saveFile(multipartFile));
    }

    @Test
    void saveFileShouldRejectUploadAboveConfiguredLimit() {
        properties.setUploadDir(tempDir.toString());
        properties.setMaxFileSizeBytes(4);
        fileService = new FileService(fileRepository, properties);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "lesson.md",
                "text/markdown",
                "12345".getBytes()
        );

        assertThrows(InvalidFileUploadException.class, () -> fileService.saveFile(multipartFile));
    }

    @Test
    void deleteFileShouldRemovePhysicalFileOnlyAfterCommit() throws Exception {
        properties.setUploadDir(tempDir.toString());
        fileService = new FileService(fileRepository, properties);

        UUID fileId = UUID.randomUUID();
        Path storedFile = tempDir.resolve(fileId.toString());
        Files.writeString(storedFile, "content");

        File file = File.builder()
                .id(fileId)
                .originalFilename("lesson.md")
                .contentType("text/markdown")
                .storagePath(storedFile.toString())
                .uploadedAt(Instant.now())
                .build();

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));

        TransactionSynchronizationManager.initSynchronization();
        try {
            fileService.deleteFile(fileId);

            assertTrue(Files.exists(storedFile));
            verify(fileRepository).delete(file);

            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            assertEquals(1, synchronizations.size());

            synchronizations.getFirst().afterCommit();

            assertFalse(Files.exists(storedFile));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
