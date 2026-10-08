package fyp.example.auth.service;

import fyp.example.auth.entity.TeacherKeyAuditLog;
import fyp.example.auth.entity.TeacherKeyRegistry;
import fyp.example.auth.repository.TeacherKeyAuditLogRepository;
import fyp.example.auth.repository.TeacherKeyRegistryRepository;
import fyp.example.dto.auth.CreateTeacherKeyDto;
import fyp.example.dto.auth.TeacherKeyResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherKeyService {

    private final TeacherKeyRegistryRepository registryRepository;
    private final TeacherKeyAuditLogRepository auditLogRepository;
    private final TeacherKeyAuditService teacherKeyAuditService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;
    private static final String GENERIC_TEACHER_AUTH_ERROR = "Invalid teacher credentials or secret key";

    @Transactional
    public TeacherKeyResponseDto generateTeacherKey(CreateTeacherKeyDto dto, String adminEmail) {
        String email = dto.getTeacherEmail().trim().toLowerCase();

        // 1. Generate secure random 32-byte alphanumeric/hex key
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        String rawSecretKey = "TC-" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // 2. Hash raw key using BCrypt
        String hashedKey = passwordEncoder.encode(rawSecretKey);

        // 3. Upsert or create registry row
        TeacherKeyRegistry registry = registryRepository.findByTeacherEmailIgnoreCase(email)
                .orElseGet(() -> TeacherKeyRegistry.builder()
                        .id(UUID.randomUUID())
                        .teacherEmail(email)
                        .createdAt(Instant.now())
                        .build());

        registry.setSecretKeyHash(hashedKey);
        registry.setActive(true);
        registry.setCreatedByAdmin(adminEmail);

        registryRepository.save(registry);
        log.info("Generated new teacher authorization key for email: {} by admin: {}", email, adminEmail);

        // 4. Return plaintext rawSecretKey ONCE in response
        return TeacherKeyResponseDto.builder()
                .id(registry.getId())
                .teacherEmail(registry.getTeacherEmail())
                .rawSecretKey(rawSecretKey)
                .isActive(registry.isActive())
                .createdByAdmin(registry.getCreatedByAdmin())
                .createdAt(registry.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<TeacherKeyResponseDto> getAllTeacherKeys() {
        return registryRepository.findAll().stream()
                .map(r -> TeacherKeyResponseDto.builder()
                        .id(r.getId())
                        .teacherEmail(r.getTeacherEmail())
                        .isActive(r.isActive())
                        .createdByAdmin(r.getCreatedByAdmin())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public TeacherKeyResponseDto toggleKeyStatus(UUID keyId, boolean isActive) {
        TeacherKeyRegistry registry = registryRepository.findById(keyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher key registry entry not found"));

        registry.setActive(isActive);
        registryRepository.save(registry);

        return TeacherKeyResponseDto.builder()
                .id(registry.getId())
                .teacherEmail(registry.getTeacherEmail())
                .isActive(registry.isActive())
                .createdByAdmin(registry.getCreatedByAdmin())
                .createdAt(registry.getCreatedAt())
                .build();
    }

    @Transactional
    public void verifyTeacherKey(String email, String rawSecretKey, String ipAddress) {
        String cleanEmail = email.trim().toLowerCase();
        Instant lockoutThreshold = Instant.now().minus(LOCKOUT_MINUTES, ChronoUnit.MINUTES);

        // 1. Check IP-based lockout
        long ipFailures = auditLogRepository.countByIpAddressAndIsSuccessFalseAndAttemptedAtAfter(ipAddress, lockoutThreshold);
        if (ipFailures >= MAX_FAILED_ATTEMPTS) {
            log.warn("Rate limit / Lockout triggered for IP: {} ({} failed attempts)", ipAddress, ipFailures);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts from this IP address. Please try again in 15 minutes.");
        }

        // 2. Check Email-based lockout
        long emailFailures = auditLogRepository.countByEmailIgnoreCaseAndIsSuccessFalseAndAttemptedAtAfter(cleanEmail, lockoutThreshold);
        if (emailFailures >= MAX_FAILED_ATTEMPTS) {
            log.warn("Rate limit / Lockout triggered for Email: {} ({} failed attempts)", cleanEmail, emailFailures);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts for this email. Account verification is locked for 15 minutes.");
        }

        // 3. Find registry entry
        Optional<TeacherKeyRegistry> optionalRegistry = registryRepository.findByTeacherEmailIgnoreCase(cleanEmail);

        if (optionalRegistry.isEmpty() || !optionalRegistry.get().isActive()) {
            teacherKeyAuditService.recordAudit(cleanEmail, ipAddress, false);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, GENERIC_TEACHER_AUTH_ERROR);
        }

        TeacherKeyRegistry registry = optionalRegistry.get();

        // 4. Verify BCrypt hash match
        if (rawSecretKey == null || !passwordEncoder.matches(rawSecretKey.trim(), registry.getSecretKeyHash())) {
            teacherKeyAuditService.recordAudit(cleanEmail, ipAddress, false);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, GENERIC_TEACHER_AUTH_ERROR);
        }

        // 5. On success: record audit and clear previous failed attempts
        teacherKeyAuditService.recordAudit(cleanEmail, ipAddress, true);
        auditLogRepository.deleteByEmailIgnoreCase(cleanEmail);
        log.info("Teacher key verification succeeded for email: {} from IP: {}", cleanEmail, ipAddress);
    }
}
