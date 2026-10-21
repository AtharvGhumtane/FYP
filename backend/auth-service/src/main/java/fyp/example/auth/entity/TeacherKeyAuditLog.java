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
@Table(name = "teacher_key_audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherKeyAuditLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "is_success", nullable = false)
    private boolean isSuccess;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;
}
