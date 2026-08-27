package com.getquer.notification;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotifications(
            Long recipientId,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("id").descending()
        );

        return notificationRepository
                .findByRecipientId(recipientId, pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long recipientId) {
        return notificationRepository
                .countByRecipientIdAndReadAtIsNull(recipientId);
    }

    @Transactional
    public NotificationDTO markAsRead(
            Long notificationId,
            Long recipientId
    ) {
        NotificationEntity notification =
                notificationRepository
                        .findByIdAndRecipientId(
                                notificationId,
                                recipientId
                        )
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Notification not found"
                                )
                        );

        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
        }

        return mapToDTO(notification);
    }

    @Transactional
    public void markAllAsRead(Long recipientId) {
        notificationRepository.markAllAsRead(
                recipientId,
                Instant.now()
        );
    }

    private NotificationDTO mapToDTO(
            NotificationEntity entity
    ) {
        return new NotificationDTO(
                entity.getId(),
                entity.getType(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getTaskId(),
                entity.getReadAt() != null,
                entity.getCreatedAt(),
                entity.getReadAt()
        );
    }
}