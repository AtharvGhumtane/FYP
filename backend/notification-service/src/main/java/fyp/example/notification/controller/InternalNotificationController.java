package fyp.example.notification.controller;

import fyp.example.dto.notification.DispatchNotificationRequest;
import fyp.example.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
@Hidden
public class InternalNotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<Void> dispatchNotification(
            @Valid @RequestBody DispatchNotificationRequest request
    ) {
        notificationService.dispatchNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
