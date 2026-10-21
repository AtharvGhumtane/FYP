package fyp.example.assessment;

import com.fasterxml.jackson.databind.ObjectMapper;
import fyp.example.assessment.client.ClassroomClient;
import fyp.example.assessment.client.NotificationClient;
import fyp.example.assessment.entity.Submission;
import fyp.example.assessment.repository.SubmissionRepository;
import fyp.example.assessment.service.AutoSubmitterService;
import fyp.example.dto.assessment.AnswerSubmissionDto;
import fyp.example.dto.assessment.AutosaveAnswersDto;
import fyp.example.dto.assessment.CreateQuestionDto;
import fyp.example.dto.assessment.CreateTestDto;
import fyp.example.dto.assessment.SubmitTestRequestDto;
import fyp.example.dto.classroom.ClassVerificationResponseDto;
import fyp.example.dto.enums.TestStatus;
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
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AssessmentServiceIntegrationTest {

    private static final String JWT_SECRET = "01234567890123456789012345678901_very_secure_secret_key_for_test!";
    private static final String INTERNAL_KEY = "test_internal_service_key";

    @org.junit.jupiter.api.BeforeAll
    static void initUtc() {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:assessment_db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;TIME ZONE=UTC");
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

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private AutoSubmitterService autoSubmitterService;

    @MockBean
    private ClassroomClient classroomClient;

    @MockBean
    private NotificationClient notificationClient;

    private UUID teacherId;
    private String teacherToken;

    private UUID studentId;
    private String studentToken;

    private UUID classId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        teacherToken = jwtTokenProvider.generateToken(teacherId, "prof@university.edu", "Prof. Alan", UserRole.ROLE_TEACHER);

        studentId = UUID.randomUUID();
        studentToken = jwtTokenProvider.generateToken(studentId, "student@university.edu", "Alice Student", UserRole.ROLE_STUDENT);

        classId = UUID.randomUUID();

        // Default mock responses
        when(classroomClient.verifyTeacher(eq(classId), eq(teacherId), any()))
                .thenReturn(ClassVerificationResponseDto.builder().isAuthorized(true).classId(classId).teacherId(teacherId).build());

        when(classroomClient.verifyStudent(eq(classId), eq(studentId), any()))
                .thenReturn(ClassVerificationResponseDto.builder().isAuthorized(true).classId(classId).build());

        when(classroomClient.getClassRoster(eq(classId), any()))
                .thenReturn(List.of(studentId));
    }

    @Test
    @DisplayName("Complete Assessment Lifecycle: Create -> Start -> Autosave -> Submit -> Scorecard -> Analytics")
    void shouldExecuteFullAssessmentLifecycle() throws Exception {
        // 1. Teacher creates test with 2 questions
        CreateQuestionDto q1 = CreateQuestionDto.builder()
                .topic("Algorithms")
                .questionText("What is the time complexity of binary search?")
                .optionA("O(log n)")
                .optionB("O(n)")
                .optionC("O(n log n)")
                .optionD("O(1)")
                .correctOption("A")
                .marks(2)
                .explanation("Binary search divides the search interval in half each time.")
                .build();

        CreateQuestionDto q2 = CreateQuestionDto.builder()
                .topic("Data Structures")
                .questionText("Which data structure uses FIFO?")
                .optionA("Stack")
                .optionB("Queue")
                .optionC("Tree")
                .optionD("Graph")
                .correctOption("B")
                .marks(3)
                .explanation("Queue uses First-In First-Out.")
                .build();

        CreateTestDto createTestDto = CreateTestDto.builder()
                .classId(classId)
                .title("Midterm Exam: Data Structures & Algorithms")
                .description("Comprehensive midterm exam")
                .startTime(Instant.now().minus(10, ChronoUnit.MINUTES))
                .endTime(Instant.now().plus(2, ChronoUnit.HOURS))
                .durationMinutes(60)
                .questions(List.of(q1, q2))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/tests")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Midterm Exam: Data Structures & Algorithms"))
                .andExpect(jsonPath("$.totalMarks").value(5))
                .andExpect(jsonPath("$.isPublished").value(true))
                .andExpect(jsonPath("$.questions[0].correctOption").value("A"))
                .andReturn();

        String testIdStr = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();
        UUID testId = UUID.fromString(testIdStr);
        String q1IdStr = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("questions").get(0).get("id").asText();
        UUID q1Id = UUID.fromString(q1IdStr);
        String q2IdStr = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("questions").get(1).get("id").asText();
        UUID q2Id = UUID.fromString(q2IdStr);

        // 2. Student lists tests for class
        mockMvc.perform(get("/api/tests/class/" + classId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testId.toString()))
                .andExpect(jsonPath("$[0].isPublished").value(true));

        // 3. Student starts test
        MvcResult startResult = mockMvc.perform(post("/api/tests/" + testId + "/start")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testId").value(testId.toString()))
                .andExpect(jsonPath("$.title").value("Midterm Exam: Data Structures & Algorithms"))
                .andExpect(jsonPath("$.questions[0].correctOption").doesNotExist()) // correct answer hidden
                .andReturn();

        assertNotNull(objectMapper.readTree(startResult.getResponse().getContentAsString()).get("serverDeadline"));

        // 4. Student autosaves answer for Q1 (selected A - correct)
        AutosaveAnswersDto autosaveDto = AutosaveAnswersDto.builder()
                .answers(List.of(AnswerSubmissionDto.builder().questionId(q1Id).selectedOption("A").build()))
                .build();

        mockMvc.perform(put("/api/tests/" + testId + "/answers")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(autosaveDto)))
                .andExpect(status().isNoContent());

        // 5. Student submits test with answer for Q2 (selected B - correct)
        SubmitTestRequestDto submitDto = SubmitTestRequestDto.builder()
                .answers(List.of(AnswerSubmissionDto.builder().questionId(q2Id).selectedOption("B").build()))
                .tabSwitchCount(0)
                .build();

        mockMvc.perform(post("/api/tests/" + testId + "/submit")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(5))
                .andExpect(jsonPath("$.totalMarks").value(5))
                .andExpect(jsonPath("$.percentage").value(100.0))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        // 6. Student views own scorecard
        mockMvc.perform(get("/api/tests/" + testId + "/my-submission")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(5))
                .andExpect(jsonPath("$.questions[0].isCorrect").value(true))
                .andExpect(jsonPath("$.questions[0].marksObtained").value(2))
                .andExpect(jsonPath("$.questions[1].isCorrect").value(true))
                .andExpect(jsonPath("$.questions[1].marksObtained").value(3));

        // 7. Teacher views all submissions for test
        mockMvc.perform(get("/api/tests/" + testId + "/submissions")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(studentId.toString()))
                .andExpect(jsonPath("$[0].score").value(5));

        // 8. Internal S2S Aggregated Analytics
        mockMvc.perform(get("/internal/tests/analytics/aggregated")
                        .param("targetId", classId.toString())
                        .param("type", "class")
                        .header("X-Internal-Token", INTERNAL_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSubmissions").value(1))
                .andExpect(jsonPath("$.averagePercentage").value(100.0))
                .andExpect(jsonPath("$.highestScore").value(5.0))
                .andExpect(jsonPath("$.topicAccuracyMap['Algorithms']").value(100.0))
                .andExpect(jsonPath("$.topicAccuracyMap['Data Structures']").value(100.0));
    }

    @Test
    @DisplayName("Security: Non-teacher cannot create test and unauthorized teacher gets 403")
    void shouldEnforceTestCreationSecurity() throws Exception {
        CreateTestDto createTestDto = CreateTestDto.builder()
                .classId(classId)
                .title("Unauthorized Test")
                .startTime(Instant.now().plus(1, ChronoUnit.HOURS))
                .endTime(Instant.now().plus(3, ChronoUnit.HOURS))
                .durationMinutes(60)
                .questions(List.of(CreateQuestionDto.builder()
                        .topic("Math")
                        .questionText("1+1=?")
                        .optionA("1").optionB("2").optionC("3").optionD("4")
                        .correctOption("B").marks(1).build()))
                .build();

        // Student cannot call POST /api/tests
        mockMvc.perform(post("/api/tests")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTestDto)))
                .andExpect(status().isForbidden());

        // Teacher not authorized for this class
        when(classroomClient.verifyTeacher(eq(classId), eq(teacherId), any()))
                .thenReturn(ClassVerificationResponseDto.builder().isAuthorized(false).build());

        mockMvc.perform(post("/api/tests")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTestDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AutoSubmitter: Expired IN_PROGRESS submission is auto-graded and saved as AUTO_SUBMITTED")
    void shouldAutoSubmitExpiredTest() throws Exception {
        // Teacher creates test
        CreateTestDto createTestDto = CreateTestDto.builder()
                .classId(classId)
                .title("Timed Quiz")
                .startTime(Instant.now().minus(2, ChronoUnit.HOURS))
                .endTime(Instant.now().plus(1, ChronoUnit.HOURS))
                .durationMinutes(1) // 1 min duration
                .questions(List.of(
                        CreateQuestionDto.builder()
                                .topic("Java")
                                .questionText("Java is OOP?")
                                .optionA("Yes").optionB("No").optionC("Maybe").optionD("None")
                                .correctOption("A").marks(5).build()
                ))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/tests")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTestDto)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID testId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());
        UUID qId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("questions").get(0).get("id").asText());

        // Student starts test
        mockMvc.perform(post("/api/tests/" + testId + "/start")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        // Student autosaves correct answer
        mockMvc.perform(put("/api/tests/" + testId + "/answers")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AutosaveAnswersDto.builder()
                                .answers(List.of(AnswerSubmissionDto.builder().questionId(qId).selectedOption("A").build()))
                                .build())))
                .andExpect(status().isNoContent());

        // Force submission start time to 10 minutes ago so deadline is expired
        Submission sub = submissionRepository.findByTestIdAndStudentId(testId, studentId).orElseThrow();
        sub.setStartedAt(Instant.now().minus(10, ChronoUnit.MINUTES));
        submissionRepository.save(sub);

        // Run auto-submitter scheduled job logic
        autoSubmitterService.processExpiredSubmissions();

        // Verify status is now AUTO_SUBMITTED with score 5
        Submission updated = submissionRepository.findByTestIdAndStudentId(testId, studentId).orElseThrow();
        assertEquals(TestStatus.AUTO_SUBMITTED, updated.getStatus());
        assertEquals(5, updated.getScore());
        assertEquals(100.0, updated.getPercentage());
    }
}
