package fyp.example.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import fyp.example.dto.enums.NotificationType;
import fyp.example.dto.enums.UserRole;
import fyp.example.dto.notification.DispatchNotificationRequest;
import fyp.example.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationServiceIntegrationTest {

    private static final String JWT_SECRET = "01234567890123456789012345678901_very_secure_secret_key_for_test!";
    private static final String INTERNAL_KEY = "test_internal_service_key";

    @BeforeAll
    static void initUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:notification_db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;TIME ZONE=UTC");
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

    private UUID userA;
    private String tokenA;

    private UUID userB;
    private String tokenB;

    @BeforeEach
    void setUp() {
        userA = UUID.randomUUID();
        tokenA = jwtTokenProvider.generateToken(userA, "usera@university.edu", "Alice Student", UserRole.ROLE_STUDENT);

        userB = UUID.randomUUID();
        tokenB = jwtTokenProvider.generateToken(userB, "userb@university.edu", "Bob Student", UserRole.ROLE_STUDENT);
    }

    @Test
    @DisplayName("Complete Notification Lifecycle: S2S Dispatch -> Poll -> Mark Single Read -> Mark All Read")
    void shouldExecuteFullNotificationLifecycle() throws Exception {
        // 1. S2S dispatch to userA and userB
        DispatchNotificationRequest request = DispatchNotificationRequest.builder()
                .userIds(List.of(userA, userB))
                .title("New Test Scheduled")
                .message("Midterm Exam is now available.")
                .type(NotificationType.TEST_SCHEDULED)
                .build();

        mockMvc.perform(post("/internal/notifications")
                        .header("X-Internal-Token", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 2. Dispatch a second notification specifically for userA
        DispatchNotificationRequest request2 = DispatchNotificationRequest.builder()
                .userIds(List.of(userA))
                .title("Test Evaluated")
                .message("Your score is 100/100.")
                .type(NotificationType.TEST_EVALUATED)
                .build();

        mockMvc.perform(post("/internal/notifications")
                        .header("X-Internal-Token", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        // 3. User A polls notifications (should see 2 notifications)
        MvcResult resultA = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].isRead").value(false))
                .andExpect(jsonPath("$[1].isRead").value(false))
                .andReturn();

        String firstNotifIdStr = objectMapper.readTree(resultA.getResponse().getContentAsString()).get(0).get("id").asText();
        UUID firstNotifId = UUID.fromString(firstNotifIdStr);

        // 4. User A marks the first notification as read
        mockMvc.perform(patch("/api/notifications/" + firstNotifId + "/read")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstNotifId.toString()))
                .andExpect(jsonPath("$.isRead").value(true));

        // 5. User A polls unread only (should see only 1)
        mockMvc.perform(get("/api/notifications")
                        .param("unreadOnly", "true")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // 6. User B attempts to mark User A's notification as read -> 403 Forbidden
        mockMvc.perform(patch("/api/notifications/" + firstNotifId + "/read")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // 7. User A marks all as read
        mockMvc.perform(post("/api/notifications/mark-all-read")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        // 8. User A polls unread only -> should be empty
        mockMvc.perform(get("/api/notifications")
                        .param("unreadOnly", "true")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Security: Internal endpoints block requests with invalid internal key")
    void shouldBlockInvalidInternalKey() throws Exception {
        DispatchNotificationRequest request = DispatchNotificationRequest.builder()
                .userIds(List.of(userA))
                .title("Unauthorized Dispatch")
                .message("Should not be saved")
                .type(NotificationType.TEST_SCHEDULED)
                .build();

        mockMvc.perform(post("/internal/notifications")
                        .header("X-Internal-Token", "wrong_key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
