package ptknow.model.file;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class File {

    @Id
    UUID id;

    @Column(nullable = false)
    String originalFilename;

    @Column(nullable = false)
    String contentType;

    @Column(nullable = false)
    String storagePath;

    @Column(nullable = false)
    Instant uploadedAt;
}