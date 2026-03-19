package ptknow.api.file;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import ptknow.dto.file.FileMetaDTO;
import ptknow.exception.file.FileAccessDeniedException;
import ptknow.model.auth.Auth;
import ptknow.service.file.FileAccessService;
import ptknow.service.file.FileService;
import ptknow.service.file.FileWriteService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

@RestController
@RequestMapping("/v0/files")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileController {

    FileService fileService;
    FileAccessService accessService;
    FileWriteService fileWriteService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('GUEST', 'STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<Resource> getFile(
            @PathVariable UUID id,
            @AuthenticationPrincipal Auth user
            ) throws IOException {
        if(!accessService.canRead(id, user))
            throw new FileAccessDeniedException("You don't have permissions to view this file");

        var openedFile = fileService.openFile(id);
        var stream = new InputStreamResource(Files.newInputStream(openedFile.path()));
        String filename = fileService.sanitizeDownloadFilename(openedFile.originalFilename(), id);
        var contentType = fileService.resolveDownloadContentType(openedFile.contentType());

        String contentDisposition = ContentDisposition.inline()
                .filename(filename, StandardCharsets.UTF_8)
                .build()
                .toString();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(contentType)
                .contentLength(openedFile.size())
                .body(stream);
    }

    @GetMapping("/{id}/meta")
    @PreAuthorize("hasAnyRole('GUEST', 'STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<FileMetaDTO> getFileMeta(
            @PathVariable UUID id,
            @AuthenticationPrincipal Auth user
    ) throws IOException {
        if (!accessService.canDelete(id, user))
            throw new FileAccessDeniedException("You don't have permissions to view this file metadata");

        return ResponseEntity.ok(fileService.getMeta(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('GUEST', 'STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<Void> deleteFile(
            @PathVariable UUID id,
            @AuthenticationPrincipal Auth user
    ) throws IOException {
        fileWriteService.deleteOwnedFile(id, user);
        return ResponseEntity.noContent().build();
    }
}

