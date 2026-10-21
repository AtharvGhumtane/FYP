package fyp.example.classroom.client;

import fyp.example.dto.notification.DispatchNotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "notification-service", url = "${notification-service.url:http://localhost:8084}")
public interface NotificationClient {

    @PostMapping("/internal/notifications")
    void sendNotification(
            @RequestBody DispatchNotificationRequest request,
            @RequestHeader("X-Internal-Token") String internalToken
    );
}
