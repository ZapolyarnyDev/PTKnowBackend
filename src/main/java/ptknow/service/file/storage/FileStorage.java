package ptknow.service.file.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public interface FileStorage {

    StoredFile store(UUID fileId, MultipartFile file) throws IOException;

    OpenedFile open(String storageKey) throws IOException;

    long size(String storageKey) throws IOException;

    void delete(String storageKey) throws IOException;

    record StoredFile(String storageKey) {}

    record OpenedFile(
            InputStream inputStream,
            long size
    ) {}
}
