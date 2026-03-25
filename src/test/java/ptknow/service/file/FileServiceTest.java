package ptknow.service.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ptknow.exception.file.InvalidFileUploadException;
import ptknow.model.file.File;
import ptknow.properties.FileStorageProperties;
import ptknow.repository.file.FileRepository;
import ptknow.service.file.storage.FileStorage;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    FileRepository fileRepository;

    @Mock
    FileStorage fileStorage;

    FileStorageProperties properties = new FileStorageProperties();

    FileService fileService;

    @Test
    void saveFileShouldDeleteStoredObjectWhenRepositorySaveFails() throws Exception {
        fileService = new FileService(fileRepository, properties, fileStorage);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "lesson.md",
                "text/markdown",
                "# content".getBytes()
        );

        when(fileStorage.store(any(UUID.class), any(org.springframework.web.multipart.MultipartFile.class)))
                .thenReturn(new FileStorage.StoredFile("stored/file-key"));
        when(fileRepository.save(any(File.class))).thenThrow(new RuntimeException("db failure"));

        assertThrows(RuntimeException.class, () -> fileService.saveFile(multipartFile));
        verify(fileStorage).delete("stored/file-key");
    }

    @Test
    void saveFileShouldRejectEmptyUpload() throws Exception {
        fileService = new FileService(fileRepository, properties, fileStorage);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "lesson.md",
                "text/markdown",
                new byte[0]
        );

        assertThrows(InvalidFileUploadException.class, () -> fileService.saveFile(multipartFile));
        verify(fileStorage, never()).store(any(UUID.class), any(org.springframework.web.multipart.MultipartFile.class));
    }

    @Test
    void saveFileShouldRejectUploadAboveConfiguredLimit() throws Exception {
        properties.setMaxFileSizeBytes(4);
        fileService = new FileService(fileRepository, properties, fileStorage);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "lesson.md",
                "text/markdown",
                "12345".getBytes()
        );

        assertThrows(InvalidFileUploadException.class, () -> fileService.saveFile(multipartFile));
        verify(fileStorage, never()).store(any(UUID.class), any(org.springframework.web.multipart.MultipartFile.class));
    }

    @Test
    void deleteFileShouldDeleteStoredObjectOnlyAfterCommit() throws Exception {
        fileService = new FileService(fileRepository, properties, fileStorage);

        UUID fileId = UUID.randomUUID();
        File file = File.builder()
                .id(fileId)
                .originalFilename("lesson.md")
                .contentType("text/markdown")
                .storagePath("stored/file-key")
                .uploadedAt(Instant.now())
                .build();

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));

        TransactionSynchronizationManager.initSynchronization();
        try {
            fileService.deleteFile(fileId);

            verify(fileRepository).delete(file);
            verify(fileStorage, never()).delete("stored/file-key");

            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            assertEquals(1, synchronizations.size());

            synchronizations.getFirst().afterCommit();

            verify(fileStorage).delete("stored/file-key");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void getMetaShouldUseStorageSize() throws Exception {
        fileService = new FileService(fileRepository, properties, fileStorage);

        UUID fileId = UUID.randomUUID();
        File file = File.builder()
                .id(fileId)
                .originalFilename("lesson.md")
                .contentType("text/markdown")
                .storagePath("stored/file-key")
                .uploadedAt(Instant.now())
                .build();

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(fileStorage.size("stored/file-key")).thenReturn(42L);

        assertEquals(42L, fileService.getMeta(fileId).size());
    }

    @Test
    void openFileShouldReturnStorageStreamAndMetadata() throws Exception {
        fileService = new FileService(fileRepository, properties, fileStorage);

        UUID fileId = UUID.randomUUID();
        File file = File.builder()
                .id(fileId)
                .originalFilename("lesson.md")
                .contentType("text/markdown")
                .storagePath("stored/file-key")
                .uploadedAt(Instant.now())
                .build();

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(fileStorage.open("stored/file-key"))
                .thenReturn(new FileStorage.OpenedFile(new ByteArrayInputStream("content".getBytes()), 7L));

        FileService.OpenedFile openedFile = fileService.openFile(fileId);

        assertEquals("text/markdown", openedFile.contentType());
        assertEquals("lesson.md", openedFile.originalFilename());
        assertEquals(7L, openedFile.size());
    }
}

