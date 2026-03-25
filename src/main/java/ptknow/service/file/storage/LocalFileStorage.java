package ptknow.service.file.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import ptknow.properties.FileStorageProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RequiredArgsConstructor
public class LocalFileStorage implements FileStorage {

    private final FileStorageProperties properties;

    @Override
    public StoredFile store(UUID fileId, MultipartFile file) throws IOException {
        Path root = Paths.get(properties.getUploadDir());
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }

        String storageKey = fileId.toString();
        Path filePath = resolveStoragePath(storageKey);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return new StoredFile(storageKey);
    }

    @Override
    public OpenedFile open(String storageKey) throws IOException {
        Path path = resolveStoragePath(storageKey);
        if (!Files.exists(path)) {
            throw new IOException("Stored file content not found");
        }

        return new OpenedFile(Files.newInputStream(path), Files.size(path));
    }

    @Override
    public long size(String storageKey) throws IOException {
        Path path = resolveStoragePath(storageKey);
        if (!Files.exists(path)) {
            throw new IOException("Stored file content not found");
        }

        return Files.size(path);
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolveStoragePath(storageKey));
    }

    private Path resolveStoragePath(String storageKey) {
        try {
            Path root = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
            Path resolved = root.resolve(storageKey).normalize();
            if (!resolved.startsWith(root)) {
                throw new IllegalStateException("Resolved storage path escapes configured root directory");
            }
            return resolved;
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("Invalid storage key", e);
        }
    }
}
