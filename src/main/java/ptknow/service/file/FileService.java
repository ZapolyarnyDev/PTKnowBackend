package ptknow.service.file;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import ptknow.dto.file.FileMetaDTO;
import ptknow.exception.file.FileNotFoundException;
import ptknow.exception.file.InvalidFileUploadException;
import ptknow.model.file.File;
import ptknow.properties.FileStorageProperties;
import ptknow.repository.file.FileRepository;
import ptknow.service.file.storage.FileStorage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final FileStorageProperties fileStorageProperties;
    private final FileStorage fileStorage;

    public record OpenedFile(
            InputStream inputStream,
            String contentType,
            String originalFilename,
            long size
    ) {}

    public File saveFile(MultipartFile file) throws IOException {
        validateUpload(file);

        UUID fileId = UUID.randomUUID();
        FileStorage.StoredFile storedFile = fileStorage.store(fileId, file);

        try {
            File entity = File.builder()
                    .id(fileId)
                    .originalFilename(sanitizeDownloadFilename(file.getOriginalFilename(), fileId))
                    .contentType(resolveDownloadContentType(file.getContentType()).toString())
                    .storagePath(storedFile.storageKey())
                    .uploadedAt(Instant.now())
                    .build();

            return fileRepository.save(entity);
        } catch (RuntimeException e) {
            fileStorage.delete(storedFile.storageKey());
            throw e;
        }
    }

    public OpenedFile openFile(UUID id) throws IOException {
        File fileEntity = fileRepository.findById(id)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        FileStorage.OpenedFile storedFile = openStoredFile(fileEntity);
        return new OpenedFile(
                storedFile.inputStream(),
                fileEntity.getContentType(),
                fileEntity.getOriginalFilename(),
                storedFile.size()
        );
    }

    public String sanitizeDownloadFilename(String originalFilename, UUID fallbackId) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return fallbackId.toString();
        }

        String normalized = originalFilename
                .replace('\\', '/')
                .replace("\r", "")
                .replace("\n", "")
                .replace("\"", "");

        try {
            String fileNameOnly = Paths.get(normalized).getFileName().toString().trim();
            if (fileNameOnly.isBlank()) {
                return fallbackId.toString();
            }
            return fileNameOnly;
        } catch (InvalidPathException e) {
            return fallbackId.toString();
        }
    }

    public MediaType resolveDownloadContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            MediaType parsed = MediaType.parseMediaType(contentType);
            if (parsed.getType().contains("\r") || parsed.getType().contains("\n")
                    || parsed.getSubtype().contains("\r") || parsed.getSubtype().contains("\n")) {
                return MediaType.APPLICATION_OCTET_STREAM;
            }
            return parsed;
        } catch (IllegalArgumentException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    public FileMetaDTO getMeta(UUID id) throws IOException {
        File fileEntity = fileRepository.findById(id)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        return FileMetaDTO.builder()
                .id(fileEntity.getId())
                .originalFilename(fileEntity.getOriginalFilename())
                .contentType(fileEntity.getContentType())
                .size(fileStorage.size(fileEntity.getStoragePath()))
                .uploadedAt(fileEntity.getUploadedAt())
                .build();
    }

    public void deleteFile(UUID id) throws IOException {
        File fileEntity = fileRepository.findById(id)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        fileRepository.delete(fileEntity);
        deleteStoredFileAfterCommit(fileEntity.getStoragePath());
    }

    private FileStorage.OpenedFile openStoredFile(File fileEntity) throws IOException {
        try {
            return fileStorage.open(fileEntity.getStoragePath());
        } catch (IOException e) {
            throw new FileNotFoundException("Stored file content not found");
        }
    }

    private void deleteStoredFileAfterCommit(String storageKey) throws IOException {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        fileStorage.delete(storageKey);
                    } catch (IOException ignored) {
                    }
                }
            });
            return;
        }

        fileStorage.delete(storageKey);
    }

    private void validateUpload(MultipartFile file) {
        if (file.isEmpty() || file.getSize() <= 0) {
            throw new InvalidFileUploadException("Uploaded file must not be empty");
        }

        if (file.getSize() > fileStorageProperties.getMaxFileSizeBytes()) {
            throw new InvalidFileUploadException("Uploaded file exceeds the allowed size");
        }
    }
}
