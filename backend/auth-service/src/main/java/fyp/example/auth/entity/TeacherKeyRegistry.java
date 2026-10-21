package fyp.example.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "teacher_key_registry")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherKeyRegistry {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "teacher_email", nullable = false, unique = true)
    private String teacherEmail;

    @Column(name = "secret_key_hash", nullable = false)
    private String secretKeyHash;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_by_admin", nullable = false)
    private String createdByAdmin;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
