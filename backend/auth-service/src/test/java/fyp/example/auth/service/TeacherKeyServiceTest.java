package fyp.example.auth.service;

import fyp.example.auth.entity.TeacherKeyAuditLog;
import fyp.example.auth.entity.TeacherKeyRegistry;
import fyp.example.auth.repository.TeacherKeyAuditLogRepository;
import fyp.example.auth.repository.TeacherKeyRegistryRepository;
import fyp.example.dto.auth.CreateTeacherKeyDto;
import fyp.example.dto.auth.TeacherKeyResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherKeyServiceTest {

    @Mock
    private TeacherKeyRegistryRepository registryRepository;

    @Mock
    private TeacherKeyAuditLogRepository auditLogRepository;

    @Mock
    private TeacherKeyAuditService teacherKeyAuditService;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private TeacherKeyService teacherKeyService;

    private static final String TEACHER_EMAIL = "teacher@university.edu";
    private static final String ADMIN_EMAIL = "admin@system.local";
    private static final String IP_ADDRESS = "192.168.1.100";

    @Test
    @DisplayName("Admin generates teacher key: key is returned raw once, stored hashed in DB")
    void shouldGenerateTeacherKeySuccessfully() {
        CreateTeacherKeyDto dto = CreateTeacherKeyDto.builder()
                .teacherEmail(TEACHER_EMAIL)
                .build();

        when(registryRepository.findByTeacherEmailIgnoreCase(TEACHER_EMAIL)).thenReturn(Optional.empty());

        TeacherKeyResponseDto response = teacherKeyService.generateTeacherKey(dto, ADMIN_EMAIL);

        assertThat(response).isNotNull();
        assertThat(response.getTeacherEmail()).isEqualTo(TEACHER_EMAIL);
        assertThat(response.getRawSecretKey()).startsWith("TC-");
        assertThat(response.isActive()).isTrue();
        assertThat(response.getCreatedByAdmin()).isEqualTo(ADMIN_EMAIL);

        ArgumentCaptor<TeacherKeyRegistry> registryCaptor = ArgumentCaptor.forClass(TeacherKeyRegistry.class);
        verify(registryRepository).save(registryCaptor.capture());

        TeacherKeyRegistry saved = registryCaptor.getValue();
        assertThat(saved.getTeacherEmail()).isEqualTo(TEACHER_EMAIL);
        assertThat(saved.getSecretKeyHash()).isNotEqualTo(response.getRawSecretKey());
        assertThat(passwordEncoder.matches(response.getRawSecretKey(), saved.getSecretKeyHash())).isTrue();
    }

    @Test
    @DisplayName("Verify valid teacher key succeeds and resets audit logs")
    void shouldVerifyValidTeacherKey() {
        String rawKey = "TC-ValidSecretKey123456";
        String hashedKey = passwordEncoder.encode(rawKey);

        TeacherKeyRegistry registry = TeacherKeyRegistry.builder()
                .id(UUID.randomUUID())
                .teacherEmail(TEACHER_EMAIL)
                .secretKeyHash(hashedKey)
                .isActive(true)
                .build();

        when(auditLogRepository.countByIpAddressAndIsSuccessFalseAndAttemptedAtAfter(eq(IP_ADDRESS), any(Instant.class))).thenReturn(0L);
        when(auditLogRepository.countByEmailIgnoreCaseAndIsSuccessFalseAndAttemptedAtAfter(eq(TEACHER_EMAIL), any(Instant.class))).thenReturn(0L);
        when(registryRepository.findByTeacherEmailIgnoreCase(TEACHER_EMAIL)).thenReturn(Optional.of(registry));

        teacherKeyService.verifyTeacherKey(TEACHER_EMAIL, rawKey, IP_ADDRESS);

        verify(auditLogRepository).deleteByEmailIgnoreCase(TEACHER_EMAIL);
        verify(teacherKeyAuditService).recordAudit(TEACHER_EMAIL, IP_ADDRESS, true);
    }

    @Test
    @DisplayName("Verify wrong teacher key fails with 401 and logs failure")
    void shouldFailWhenSecretKeyIsWrong() {
        String rawKey = "TC-ValidSecretKey123456";
        String wrongKey = "TC-WrongSecretKey999999";
        String hashedKey = passwordEncoder.encode(rawKey);

        TeacherKeyRegistry registry = TeacherKeyRegistry.builder()
                .id(UUID.randomUUID())
                .teacherEmail(TEACHER_EMAIL)
                .secretKeyHash(hashedKey)
                .isActive(true)
                .build();

        when(auditLogRepository.countByIpAddressAndIsSuccessFalseAndAttemptedAtAfter(eq(IP_ADDRESS), any(Instant.class))).thenReturn(0L);
        when(auditLogRepository.countByEmailIgnoreCaseAndIsSuccessFalseAndAttemptedAtAfter(eq(TEACHER_EMAIL), any(Instant.class))).thenReturn(0L);
        when(registryRepository.findByTeacherEmailIgnoreCase(TEACHER_EMAIL)).thenReturn(Optional.of(registry));

        assertThatThrownBy(() -> teacherKeyService.verifyTeacherKey(TEACHER_EMAIL, wrongKey, IP_ADDRESS))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(teacherKeyAuditService).recordAudit(TEACHER_EMAIL, IP_ADDRESS, false);
        verify(auditLogRepository, never()).deleteByEmailIgnoreCase(any());
    }

    @Test
    @DisplayName("Rate limiting / Lockout: 5 failed attempts from IP triggers 429")
    void shouldTriggerIpLockoutAfter5Failures() {
        when(auditLogRepository.countByIpAddressAndIsSuccessFalseAndAttemptedAtAfter(eq(IP_ADDRESS), any(Instant.class))).thenReturn(5L);

        assertThatThrownBy(() -> teacherKeyService.verifyTeacherKey(TEACHER_EMAIL, "TC-AnyKey", IP_ADDRESS))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

        verify(registryRepository, never()).findByTeacherEmailIgnoreCase(any());
    }

    @Test
    @DisplayName("Rate limiting / Lockout: 5 failed attempts for Email triggers 429")
    void shouldTriggerEmailLockoutAfter5Failures() {
        when(auditLogRepository.countByIpAddressAndIsSuccessFalseAndAttemptedAtAfter(eq(IP_ADDRESS), any(Instant.class))).thenReturn(0L);
        when(auditLogRepository.countByEmailIgnoreCaseAndIsSuccessFalseAndAttemptedAtAfter(eq(TEACHER_EMAIL), any(Instant.class))).thenReturn(5L);

        assertThatThrownBy(() -> teacherKeyService.verifyTeacherKey(TEACHER_EMAIL, "TC-AnyKey", IP_ADDRESS))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

        verify(registryRepository, never()).findByTeacherEmailIgnoreCase(any());
    }
}
