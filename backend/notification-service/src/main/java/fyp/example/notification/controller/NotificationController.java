package fyp.example.notification.controller;

import fyp.example.dto.notification.NotificationDto;
import fyp.example.notification.service.NotificationService;
import fyp.example.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification API", description = "User notification polling, reading, and clearing")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Poll notifications for authenticated user")
    public ResponseEntity<List<NotificationDto>> getNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<NotificationDto> notifications = notificationService.getNotifications(principal.getId(), unreadOnly);
        return ResponseEntity.ok(notifications);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<NotificationDto> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        NotificationDto notification = notificationService.markAsRead(id, principal.getId());
        return ResponseEntity.ok(notification);
    }

    @PostMapping("/mark-all-read")
    @Operation(summary = "Mark all notifications as read for authenticated user")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.noContent().build();
    }
}
