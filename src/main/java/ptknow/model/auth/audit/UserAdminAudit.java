package ptknow.model.auth.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_admin_audit")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAdminAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, updatable = false)
    UUID actorId;

    @Column(nullable = false, updatable = false)
    UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    UserAdminAction action;

    @Column(nullable = false, updatable = false)
    String oldValue;

    @Column(nullable = false, updatable = false)
    String newValue;

    @Column(nullable = false, updatable = false)
    Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
