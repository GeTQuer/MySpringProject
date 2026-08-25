package com.getquer.tasktracker.service;

import com.getquer.tasktracker.Entities.NotificationEntity;
import com.getquer.tasktracker.Entities.UserEntity;
import com.getquer.tasktracker.Enums.NotificationType;
import com.getquer.tasktracker.Repositories.NotificationRepository;
import com.getquer.tasktracker.Repositories.UserRepository;
import com.getquer.tasktracker.responseDTO.NotificationDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class NotificationService {
    public record TaskAssignedEvent(
            UUID eventId,
            Long taskId,
            Long actorId,
            String actorUsername,
            Long recipientId,
            String taskContent,
            Instant occurredAt
    ) {}
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public NotificationDTO createNotification(
            TaskAssignedEvent event
    ){
        NotificationEntity notification = new NotificationEntity();
        notification.setEventId(event.eventId());
        notification.setTaskId(event.taskId());
        notification.setActorId(event.actorId());
        notification.setRecipientId(event.recipientId());
        notification.setType(NotificationType.TASK_ASSIGNED);
        notification.setTitle("Вам назначена новая задача");
        notification.setMessage(
                event.actorUsername()
                        + " назначил вам задачу: "
                        + event.taskContent()
        );
        notification.setCreatedAt(event.occurredAt());

        notificationRepository.save(notification);
        return mapToDTO(notification);
    }


    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotifications(
            String username,
            int page,
            int size
    ){
        UserEntity recipient = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "User not found: " + username
                        )
                );

        Pageable pageable = PageRequest.of(page,size, Sort.by("id").descending());

        return notificationRepository.findByRecipientId(recipient.getId(),pageable).map(this::mapToDTO);

    }

    @Transactional(readOnly = true)
    public Long getUnreadCount(String username){
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        return notificationRepository.countByRecipientIdAndReadAtIsNull(user.getId());
    }

    @Transactional
    public NotificationDTO markAsRead(
            Long notificationId,
            String username
    ){
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        NotificationEntity notification = notificationRepository.findByIdAndRecipientId(notificationId, user.getId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Notification not found")
                );
        if (notification.getReadAt() == null){
            notification.setReadAt(Instant.now());
        }
        return mapToDTO(notification);
    }

    @Transactional
    public void markAllAsRead(String username){
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        notificationRepository.markAllAsRead(user.getId(),Instant.now());
    }

    private NotificationDTO mapToDTO(NotificationEntity entity){
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
