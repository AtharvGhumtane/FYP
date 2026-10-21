package fyp.example.classroom;

import com.fasterxml.jackson.databind.ObjectMapper;
import fyp.example.classroom.client.NotificationClient;
import fyp.example.dto.classroom.CreateClassDto;
import fyp.example.dto.classroom.EnrollmentDecisionDto;
import fyp.example.dto.enums.EnrollmentStatus;
import fyp.example.dto.enums.UserRole;
import fyp.example.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClassroomServiceIntegrationTest {

    private static final String JWT_SECRET = "01234567890123456789012345678901_very_secure_secret_key_for_test!";
    private static final String INTERNAL_KEY = "test_internal_service_key";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:classroom_db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("jwt.secret", () -> JWT_SECRET);
        registry.add("app.security.internal-service-key", () -> INTERNAL_KEY);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private NotificationClient notificationClient;

    private UUID teacherId;
    private String teacherToken;

    private UUID studentId;
    private String studentToken;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        teacherToken = jwtTokenProvider.generateToken(teacherId, "teacher@university.edu", "Prof. Smith", UserRole.ROLE_TEACHER);

        studentId = UUID.randomUUID();
        studentToken = jwtTokenProvider.generateToken(studentId, "student@university.edu", "Alice Student", UserRole.ROLE_STUDENT);
    }

    @Test
    @DisplayName("Complete classroom workflow: create -> join -> approve -> roster -> internal verify")
    void shouldExecuteFullClassroomWorkflow() throws Exception {
        // 1. Teacher creates class
        CreateClassDto createDto = CreateClassDto.builder()
                .name("Operating Systems")
                .subject("Computer Science")
                .description("Processes, memory, and file systems")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/classes")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Operating Systems"))
                .andExpect(jsonPath("$.subject").value("Computer Science"))
                .andExpect(jsonPath("$.teacherId").value(teacherId.toString()))
                .andReturn();

        String classIdStr = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();
        UUID classId = UUID.fromString(classIdStr);

        // 2. Student requests to join class
        mockMvc.perform(post("/api/classes/" + classId + "/join")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.studentId").value(studentId.toString()));

        // 3. Teacher views pending requests
        mockMvc.perform(get("/api/classes/" + classId + "/requests")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(studentId.toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        // 4. Teacher approves request
        EnrollmentDecisionDto decisionDto = EnrollmentDecisionDto.builder()
                .status(EnrollmentStatus.ACCEPTED)
                .build();

        mockMvc.perform(post("/api/classes/" + classId + "/requests/" + studentId)
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decisionDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // 5. Student views class roster
        mockMvc.perform(get("/api/classes/" + classId + "/roster")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(studentId.toString()));

        // 6. Internal S2S verification: student is accepted
        mockMvc.perform(get("/internal/classes/" + classId + "/verify-student/" + studentId)
                        .header("X-Internal-Token", INTERNAL_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthorized").value(true));

        // 7. Internal S2S verification: teacher ownership
        mockMvc.perform(get("/internal/classes/" + classId + "/verify-teacher/" + teacherId)
                        .header("X-Internal-Token", INTERNAL_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthorized").value(true));
    }

    @Test
    @DisplayName("Access Control: Non-owner teacher cannot view or manage another teacher's class")
    void shouldBlockUnauthorizedTeacher() throws Exception {
        // Teacher A creates class
        CreateClassDto createDto = CreateClassDto.builder()
                .name("Compilers")
                .subject("Computer Science")
                .description("Lexing and parsing")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/classes")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn();

        String classIdStr = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // Teacher B attempts to view class details
        UUID otherTeacherId = UUID.randomUUID();
        String otherTeacherToken = jwtTokenProvider.generateToken(otherTeacherId, "other@university.edu", "Prof. Other", UserRole.ROLE_TEACHER);

        mockMvc.perform(get("/api/classes/" + classIdStr)
                        .header("Authorization", "Bearer " + otherTeacherToken))
                .andExpect(status().isForbidden());
    }
}
