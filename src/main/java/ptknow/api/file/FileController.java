package ptknow.api.file;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import ptknow.api.exception.ApiError;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Файлы", description = "Скачивание, метаданные и удаление файлов")
public class FileController {

    FileService fileService;
    FileAccessService accessService;
    FileWriteService fileWriteService;

    @GetMapping("/{id}")
    @Operation(summary = "Скачать файл", description = "Возвращает бинарное содержимое файла, если текущий пользователь имеет право чтения по правилам видимости и владения.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Бинарный поток файла",
                    content = @Content(mediaType = "application/octet-stream")),
            @ApiResponse(responseCode = "403", description = "Доступ к файлу запрещён",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Файл не найден",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("permitAll()")
    public ResponseEntity<Resource> getFile(
            @PathVariable UUID id,
            @AuthenticationPrincipal Auth user
            ) throws IOException {
        if(!accessService.canRead(id, user))
            throw new FileAccessDeniedException("You don't have permissions to view this file");

        var openedFile = fileService.openFile(id);
        var stream = new InputStreamResource(openedFile.inputStream());
        String filename = fileService.sanitizeDownloadFilename(openedFile.originalFilename(), id);
        var contentType = fileService.resolveDownloadContentType(openedFile.contentType());

        String contentDisposition = ContentDisposition.inline()
                .filename(filename, StandardCharsets.UTF_8)
                .build()
                .toString();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .header("X-Content-Type-Options", "nosniff")
                .contentType(contentType)
                .contentLength(openedFile.size())
                .body(stream);
    }

    @GetMapping("/{id}/meta")
    @Operation(summary = "Получить метаданные файла", description = "Возвращает метаданные файла. В текущей реализации доступно только пользователям, которые могут удалить файл.")
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
    @Operation(summary = "Удалить файл", description = "Удаляет файл, если у текущего пользователя есть право записи на владеющий ресурс или он ADMIN.")
    @PreAuthorize("hasAnyRole('GUEST', 'STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<Void> deleteFile(
            @PathVariable UUID id,
            @AuthenticationPrincipal Auth user
    ) throws IOException {
        fileWriteService.deleteOwnedFile(id, user);
        return ResponseEntity.noContent().build();
    }
}
