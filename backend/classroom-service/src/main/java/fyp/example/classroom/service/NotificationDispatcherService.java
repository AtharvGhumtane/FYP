package fyp.example.classroom.service;

import fyp.example.classroom.client.NotificationClient;
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
    public void notifyTeacherJoinRequest(UUID teacherId, String studentName, String className, UUID classId) {
        try {
            DispatchNotificationRequest request = DispatchNotificationRequest.builder()
                    .userIds(List.of(teacherId))
                    .title("New Join Request")
                    .message(studentName + " has requested to join your class: " + className)
                    .type(NotificationType.ENROLLMENT_REQUEST)
                    .build();

            notificationClient.sendNotification(request, internalServiceKey);
            log.info("Sent join request notification to teacher: {}", teacherId);
        } catch (Exception e) {
            log.warn("Non-fatal: Failed to send join request notification to teacher {}: {}", teacherId, e.getMessage());
        }
    }

    @Async
    public void notifyStudentJoinDecision(UUID studentId, String className, boolean accepted, UUID classId) {
        try {
            DispatchNotificationRequest request = DispatchNotificationRequest.builder()
                    .userIds(List.of(studentId))
                    .title(accepted ? "Join Request Accepted" : "Join Request Rejected")
                    .message("Your request to join class '" + className + "' was " + (accepted ? "accepted." : "rejected."))
                    .type(accepted ? NotificationType.REQUEST_APPROVED : NotificationType.ENROLLMENT_REQUEST)
                    .build();

            notificationClient.sendNotification(request, internalServiceKey);
            log.info("Sent join decision notification to student: {}", studentId);
        } catch (Exception e) {
            log.warn("Non-fatal: Failed to send join decision notification to student {}: {}", studentId, e.getMessage());
        }
    }
}
