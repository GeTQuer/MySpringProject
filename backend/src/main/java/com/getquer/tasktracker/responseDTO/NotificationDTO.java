package com.getquer.tasktracker.responseDTO;

import com.getquer.tasktracker.Enums.NotificationType;

import java.time.Instant;

public record NotificationDTO(
        Long id,
        NotificationType notificationType,
        String title,
        String message,
        Long taskId,
        boolean read,
        Instant createdAt,
        Instant readAt
) {
}
