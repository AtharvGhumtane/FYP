package fyp.example.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import fyp.example.analytics.client.AssessmentClient;
import fyp.example.analytics.client.ClassroomClient;
import fyp.example.dto.assessment.AggregatedAnalyticsDto;
import fyp.example.dto.classroom.ClassVerificationResponseDto;
import fyp.example.dto.enums.UserRole;
import fyp.example.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsServiceIntegrationTest {

    private static final String JWT_SECRET = "01234567890123456789012345678901_very_secure_secret_key_for_test!";
    private static final String INTERNAL_KEY = "test_internal_service_key";

    @BeforeAll
    static void initUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:analytics_db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;TIME ZONE=UTC");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.properties.hibernate.jdbc.time_zone", () -> "UTC");
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
    private AssessmentClient assessmentClient;

    @MockBean
    private ClassroomClient classroomClient;

    private UUID studentA;
    private String tokenStudentA;

    private UUID studentB;
    private String tokenStudentB;

    private UUID teacherId;
    private String tokenTeacher;

    private UUID classId;

    @BeforeEach
    void setUp() {
        studentA = UUID.randomUUID();
        tokenStudentA = jwtTokenProvider.generateToken(studentA, "studenta@university.edu", "Alice Student", UserRole.ROLE_STUDENT);

        studentB = UUID.randomUUID();
        tokenStudentB = jwtTokenProvider.generateToken(studentB, "studentb@university.edu", "Bob Student", UserRole.ROLE_STUDENT);

        teacherId = UUID.randomUUID();
        tokenTeacher = jwtTokenProvider.generateToken(teacherId, "prof@university.edu", "Prof. Alan", UserRole.ROLE_TEACHER);

        classId = UUID.randomUUID();

        // Default mock responses
        AggregatedAnalyticsDto mockStudentAnalytics = AggregatedAnalyticsDto.builder()
                .targetId(studentA)
                .totalSubmissions(2)
                .averageScore(85.0)
                .averagePercentage(85.0)
                .highestScore(90.0)
                .lowestScore(80.0)
                .topicAccuracyMap(Map.of("Algorithms", 90.0, "Data Structures", 80.0))
                .scoreTrend(List.of(
                        new AggregatedAnalyticsDto.ScoreTrendPointDto("Quiz 1", 80.0, "2026-10-01"),
                        new AggregatedAnalyticsDto.ScoreTrendPointDto("Quiz 2", 90.0, "2026-10-05")
                ))
                .build();

        when(assessmentClient.getAggregatedAnalytics(eq(studentA), eq("student"), any()))
                .thenReturn(mockStudentAnalytics);

        AggregatedAnalyticsDto mockClassAnalytics = AggregatedAnalyticsDto.builder()
                .targetId(classId)
                .totalSubmissions(10)
                .averageScore(75.0)
                .averagePercentage(75.0)
                .highestScore(95.0)
                .lowestScore(50.0)
                .topicAccuracyMap(Map.of("Algorithms", 70.0, "Data Structures", 80.0))
                .scoreDistribution(Map.of("61-80%", 6, "81-100%", 4))
                .scoreTrend(List.of(new AggregatedAnalyticsDto.ScoreTrendPointDto("Midterm", 75.0, "2026-10-05")))
                .build();

        when(assessmentClient.getAggregatedAnalytics(eq(classId), eq("class"), any()))
                .thenReturn(mockClassAnalytics);

        when(classroomClient.verifyTeacher(eq(classId), eq(teacherId), any()))
                .thenReturn(ClassVerificationResponseDto.builder().isAuthorized(true).classId(classId).teacherId(teacherId).className("CS101").build());

        when(classroomClient.getClassRoster(eq(classId), any()))
                .thenReturn(List.of(studentA, studentB));
    }

    @Test
    @DisplayName("Visual Analytics: Student views own DB performance metrics")
    void shouldFetchStudentDbMetrics() throws Exception {
        mockMvc.perform(get("/api/analytics/student/" + studentA + "/db-metrics")
                        .header("Authorization", "Bearer " + tokenStudentA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(studentA.toString()))
                .andExpect(jsonPath("$.totalTestsAttempted").value(2))
                .andExpect(jsonPath("$.averagePercentage").value(85.0))
                .andExpect(jsonPath("$.topicAccuracy.Algorithms").value(90.0));
    }

    @Test
    @DisplayName("Visual Analytics: Teacher views class-wide performance metrics and distribution")
    void shouldFetchClassDbMetrics() throws Exception {
        mockMvc.perform(get("/api/analytics/class/" + classId + "/db-metrics")
                        .header("Authorization", "Bearer " + tokenTeacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classId").value(classId.toString()))
                .andExpect(jsonPath("$.className").value("CS101"))
                .andExpect(jsonPath("$.totalEnrolledStudents").value(2))
                .andExpect(jsonPath("$.classAveragePercentage").value(75.0));
    }

    @Test
    @DisplayName("AI Learning Diagnosis: Student generates AI report, caches report, and retrieves cached report")
    void shouldGenerateAndCacheStudentAiReport() throws Exception {
        // 1. Initial AI report request
        mockMvc.perform(get("/api/analytics/student/" + studentA + "/ai-report")
                        .header("Authorization", "Bearer " + tokenStudentA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(studentA.toString()))
                .andExpect(jsonPath("$.aiSummary").isNotEmpty())
                .andExpect(jsonPath("$.strengths").isNotEmpty())
                .andExpect(jsonPath("$.areasForImprovement").isNotEmpty())
                .andExpect(jsonPath("$.recommendations").isNotEmpty());

        // 2. Second request returns cached report
        mockMvc.perform(get("/api/analytics/student/" + studentA + "/ai-report")
                        .header("Authorization", "Bearer " + tokenStudentA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(studentA.toString()));
    }

    @Test
    @DisplayName("AI Instructional Diagnosis: Teacher generates class overall AI report")
    void shouldGenerateClassAiReport() throws Exception {
        mockMvc.perform(get("/api/analytics/class/" + classId + "/ai-report")
                        .header("Authorization", "Bearer " + tokenTeacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classId").value(classId.toString()))
                .andExpect(jsonPath("$.aiSummary").isNotEmpty())
                .andExpect(jsonPath("$.recommendations").isNotEmpty());
    }

    @Test
    @DisplayName("Security & Access Control: Student cannot view another student's metrics and unauthorized teacher blocked")
    void shouldEnforceOwnership() throws Exception {
        // Student B tries to view Student A's analytics -> 403 Forbidden
        mockMvc.perform(get("/api/analytics/student/" + studentA + "/db-metrics")
                        .header("Authorization", "Bearer " + tokenStudentB))
                .andExpect(status().isForbidden());

        // Unauthorized teacher tries to view class analytics -> 403 Forbidden
        when(classroomClient.verifyTeacher(eq(classId), eq(teacherId), any()))
                .thenReturn(ClassVerificationResponseDto.builder().isAuthorized(false).build());

        mockMvc.perform(get("/api/analytics/class/" + classId + "/db-metrics")
                        .header("Authorization", "Bearer " + tokenTeacher))
                .andExpect(status().isForbidden());
    }
}
