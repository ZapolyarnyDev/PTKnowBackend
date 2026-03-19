package ptknow.service.file;

import ptknow.model.file.File;
import ptknow.dto.file.FileMetaDTO;
import ptknow.exception.file.FileNotFoundException;
import ptknow.properties.FileStorageProperties;
import ptknow.repository.file.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.InvalidPathException;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final FileStorageProperties fileStorageProperties;

    public record OpenedFile(
            Path path,
            String contentType,
            String originalFilename,
            long size
    ) {}

    public File saveFile(MultipartFile file) throws IOException {
        Path root = Paths.get(fileStorageProperties.getUploadDir());
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }

        UUID fileId = UUID.randomUUID();
        Path filePath = root.resolve(fileId.toString());
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        File entity = File.builder()
                .id(fileId)
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .storagePath(filePath.toString())
                .uploadedAt(Instant.now())
                .build();

        return fileRepository.save(entity);
    }

    public OpenedFile openFile(UUID id) throws IOException {
        File fileEntity = fileRepository.findById(id)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        Path path = Paths.get(fileEntity.getStoragePath());
        if (!Files.exists(path)) {
            throw new FileNotFoundException("Stored file content not found");
        }

        return new OpenedFile(
                path,
                fileEntity.getContentType(),
                fileEntity.getOriginalFilename(),
                Files.size(path)
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

        Path path = Paths.get(fileEntity.getStoragePath());
        if (!Files.exists(path)) {
            throw new FileNotFoundException("Stored file content not found");
        }

        return FileMetaDTO.builder()
                .id(fileEntity.getId())
                .originalFilename(fileEntity.getOriginalFilename())
                .contentType(fileEntity.getContentType())
                .size(Files.size(path))
                .uploadedAt(fileEntity.getUploadedAt())
                .build();
    }

    public void deleteFile(UUID id) throws IOException {
        File fileEntity = fileRepository.findById(id)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        Path path = Paths.get(fileEntity.getStoragePath());
        if (Files.exists(path)) {
            Files.delete(path);
        }

        fileRepository.delete(fileEntity);
    }

}

