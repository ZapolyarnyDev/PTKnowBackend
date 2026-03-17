package ptknow.service.file;

import ptknow.model.file.File;
import ptknow.dto.file.FileMetaDTO;
import ptknow.exception.file.FileNotFoundException;
import ptknow.properties.FileStorageProperties;
import ptknow.repository.file.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

