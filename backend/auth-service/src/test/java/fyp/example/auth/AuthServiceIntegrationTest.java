package fyp.example.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import fyp.example.auth.entity.TeacherKeyRegistry;
import fyp.example.auth.entity.User;
import fyp.example.auth.repository.TeacherKeyAuditLogRepository;
import fyp.example.auth.repository.TeacherKeyRegistryRepository;
import fyp.example.auth.repository.UserRepository;
import fyp.example.auth.service.AdminSeedingService;
import fyp.example.auth.service.TeacherKeyService;
import fyp.example.dto.auth.CreateTeacherKeyDto;
import fyp.example.dto.auth.LoginRequestDto;
import fyp.example.dto.auth.StudentRegisterDto;
import fyp.example.dto.auth.TeacherKeyResponseDto;
import fyp.example.dto.auth.TeacherRegisterDto;
import fyp.example.dto.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthServiceIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:auth_db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("jwt.secret", () -> "01234567890123456789012345678901_very_secure_secret_key_for_test!");
        registry.add("app.security.internal-service-key", () -> "test_internal_key");
        registry.add("app.admin.email", () -> "admin@system.local");
        registry.add("app.admin.password", () -> "AdminPassword123!");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherKeyRegistryRepository teacherKeyRegistryRepository;

    @Autowired
    private TeacherKeyAuditLogRepository auditLogRepository;

    @Autowired
    private TeacherKeyService teacherKeyService;

    @Autowired
    private AdminSeedingService adminSeedingService;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
    }

    @Test
    @DisplayName("Admin seeding is idempotent and seeds initial admin once")
    void shouldSeedAdminIdempotently() {
        // Run seed method multiple times
        adminSeedingService.seedAdminUser();
        adminSeedingService.seedAdminUser();

        long adminCount = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.ROLE_ADMIN)
                .count();

        assertThat(adminCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Student registration: server strictly assigns ROLE_STUDENT and allows login")
    void shouldRegisterStudentAndLoginSuccessfully() throws Exception {
        String email = "student_" + UUID.randomUUID() + "@university.edu";
        StudentRegisterDto dto = StudentRegisterDto.builder()
                .fullName("Test Student")
                .email(email)
                .password("Password123!")
                .build();

        // 1. Register student
        mockMvc.perform(post("/api/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ROLE_STUDENT"))
                .andExpect(jsonPath("$.token").isNotEmpty());

        // 2. Login as student
        LoginRequestDto loginDto = LoginRequestDto.builder()
                .email(email)
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_STUDENT"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("Teacher registration: requires valid secret key and assigns ROLE_TEACHER")
    void shouldRegisterTeacherWithValidKey() throws Exception {
        String email = "teacher_" + UUID.randomUUID() + "@university.edu";

        // Generate key via TeacherKeyService
        TeacherKeyResponseDto keyDto = teacherKeyService.generateTeacherKey(
                CreateTeacherKeyDto.builder().teacherEmail(email).build(),
                "admin@system.local"
        );

        TeacherRegisterDto registerDto = TeacherRegisterDto.builder()
                .fullName("Prof. Alan")
                .email(email)
                .password("Password123!")
                .secretKey(keyDto.getRawSecretKey())
                .build();

        mockMvc.perform(post("/api/auth/register/teacher")
                        .header("X-Forwarded-For", "10.0.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ROLE_TEACHER"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("Lockout: 5 failed attempts locks out client, returning 429 even with correct key")
    void shouldLockoutAfter5FailedAttempts() throws Exception {
        String email = "teacher_lockout_" + UUID.randomUUID() + "@university.edu";
        String clientIp = "198.51.100.25";

        // Generate a valid key
        TeacherKeyResponseDto keyDto = teacherKeyService.generateTeacherKey(
                CreateTeacherKeyDto.builder().teacherEmail(email).build(),
                "admin@system.local"
        );

        TeacherRegisterDto wrongKeyDto = TeacherRegisterDto.builder()
                .fullName("Prof. Test")
                .email(email)
                .password("Password123!")
                .secretKey("TC-WrongKey")
                .build();

        // 5 consecutive failed attempts
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/register/teacher")
                            .header("X-Forwarded-For", clientIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongKeyDto)))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt: using the CORRECT key -> MUST return 429 (Too Many Requests) due to lockout
        TeacherRegisterDto correctKeyDto = TeacherRegisterDto.builder()
                .fullName("Prof. Test")
                .email(email)
                .password("Password123!")
                .secretKey(keyDto.getRawSecretKey())
                .build();

        mockMvc.perform(post("/api/auth/register/teacher")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctKeyDto)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Lockout isolation: lockout on client A does not affect client B")
    void shouldNotShareLockoutBetweenDifferentClients() throws Exception {
        String emailA = "teacher_a_" + UUID.randomUUID() + "@university.edu";
        String emailB = "teacher_b_" + UUID.randomUUID() + "@university.edu";
        String clientIpA = "192.168.10.1";
        String clientIpB = "192.168.20.2";

        // Generate valid key for B
        TeacherKeyResponseDto keyDtoB = teacherKeyService.generateTeacherKey(
                CreateTeacherKeyDto.builder().teacherEmail(emailB).build(),
                "admin@system.local"
        );

        TeacherRegisterDto wrongKeyDtoA = TeacherRegisterDto.builder()
                .fullName("Teacher A")
                .email(emailA)
                .password("Password123!")
                .secretKey("TC-WrongKey")
                .build();

        // Fail 5 times on client A
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/register/teacher")
                            .header("X-Forwarded-For", clientIpA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongKeyDtoA)))
                    .andExpect(status().isUnauthorized());
        }

        // Client B from different IP should be able to register successfully
        TeacherRegisterDto correctKeyDtoB = TeacherRegisterDto.builder()
                .fullName("Teacher B")
                .email(emailB)
                .password("Password123!")
                .secretKey(keyDtoB.getRawSecretKey())
                .build();

        mockMvc.perform(post("/api/auth/register/teacher")
                        .header("X-Forwarded-For", clientIpB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctKeyDtoB)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ROLE_TEACHER"));
    }
}
