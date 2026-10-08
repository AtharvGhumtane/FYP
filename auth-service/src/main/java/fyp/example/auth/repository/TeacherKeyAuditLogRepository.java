package fyp.example.auth.repository;

import fyp.example.auth.entity.TeacherKeyAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface TeacherKeyAuditLogRepository extends JpaRepository<TeacherKeyAuditLog, UUID> {
    long countByEmailIgnoreCaseAndIsSuccessFalseAndAttemptedAtAfter(String email, Instant after);
    long countByIpAddressAndIsSuccessFalseAndAttemptedAtAfter(String ipAddress, Instant after);
    void deleteByEmailIgnoreCase(String email);
}
