package fyp.example.assessment.service;

import fyp.example.assessment.client.NotificationClient;
import fyp.example.dto.enums.NotificationType;
import fyp.example.dto.notification.DispatchNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcherService {

    private final NotificationClient notificationClient;

    @Value("${app.security.internal-service-key:}")
    private String internalServiceKey;

    @Async
    public void notifyStudentsTestScheduled(List<UUID> studentIds, String testTitle, UUID testId) {
        if (studentIds == null || studentIds.isEmpty()) {
            return;
        }
        try {
            DispatchNotificationRequest request = DispatchNotificationRequest.builder()
                    .userIds(studentIds)
                    .title("New Test Scheduled")
                    .message("A new test '" + testTitle + "' has been scheduled.")
                    .type(NotificationType.TEST_SCHEDULED)
                    .build();

            notificationClient.sendNotification(request, internalServiceKey);
            log.info("Sent test scheduled notification for test {} to {} students", testId, studentIds.size());
        } catch (Exception e) {
            log.warn("Non-fatal: Failed to send test scheduled notification for test {}: {}", testId, e.getMessage());
        }
    }

    @Async
    public void notifyStudentTestEvaluated(UUID studentId, String testTitle, int score, int totalMarks, UUID testId) {
        if (studentId == null) {
            return;
        }
        try {
            DispatchNotificationRequest request = DispatchNotificationRequest.builder()
                    .userIds(List.of(studentId))
                    .title("Test Evaluated")
                    .message("Your submission for '" + testTitle + "' has been evaluated. Score: " + score + "/" + totalMarks)
                    .type(NotificationType.TEST_EVALUATED)
                    .build();

            notificationClient.sendNotification(request, internalServiceKey);
            log.info("Sent test evaluated notification for test {} to student {}", testId, studentId);
        } catch (Exception e) {
            log.warn("Non-fatal: Failed to send test evaluated notification for test {}: {}", testId, e.getMessage());
        }
    }
}
