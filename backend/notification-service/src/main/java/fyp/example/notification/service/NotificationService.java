package fyp.example.notification.service;

import fyp.example.dto.notification.DispatchNotificationRequest;
import fyp.example.dto.notification.NotificationDto;
import fyp.example.notification.entity.Notification;
import fyp.example.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<NotificationDto> getNotifications(UUID userId, boolean unreadOnly) {
        List<Notification> list = unreadOnly
                ? notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return list.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public NotificationDto markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this notification");
        }

        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        return mapToDto(saved);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadForUser(userId);
    }

    @Transactional
    public void dispatchNotification(DispatchNotificationRequest request) {
        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        List<Notification> entities = new ArrayList<>();
        for (UUID userId : request.getUserIds()) {
            entities.add(Notification.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .type(request.getType())
                    .isRead(false)
                    .createdAt(now)
                    .build());
        }

        notificationRepository.saveAll(entities);
        log.info("Dispatched {} notification(s) of type {}", entities.size(), request.getType());
    }

    private NotificationDto mapToDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
