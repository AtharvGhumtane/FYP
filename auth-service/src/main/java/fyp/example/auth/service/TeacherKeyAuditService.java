package fyp.example.auth.service;

import fyp.example.auth.entity.TeacherKeyAuditLog;
import fyp.example.auth.repository.TeacherKeyAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherKeyAuditService {

    private final TeacherKeyAuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAudit(String email, String ipAddress, boolean isSuccess) {
        TeacherKeyAuditLog logEntry = TeacherKeyAuditLog.builder()
                .id(UUID.randomUUID())
                .email(email)
                .ipAddress(ipAddress != null ? ipAddress : "UNKNOWN")
                .isSuccess(isSuccess)
                .attemptedAt(Instant.now())
                .build();
        auditLogRepository.save(logEntry);
    }
}
