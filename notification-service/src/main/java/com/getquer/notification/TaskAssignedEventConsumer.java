package com.getquer.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskAssignedEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;

    @KafkaListener(
            topics = "task-events",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void consume(String payload) {
        TaskAssignedEventV1 event = deserialize(payload);

        if (notificationRepository.existsByEventId(event.eventId())) {
                log.info(
                    "Event {} has already been processed",
                    event.eventId()
            );
            return;
        }

        NotificationEntity notification = new NotificationEntity();
        notification.setEventId(event.eventId());
        notification.setRecipientId(event.recipientId());
        notification.setActorId(event.actorId());
        notification.setTaskId(event.taskId());
        notification.setType(NotificationType.TASK_ASSIGNED);
        notification.setTitle("Вам назначена новая задача");
        notification.setMessage(
                event.actorUsername()
                        + " назначил вам задачу: "
                        + event.taskTitle()
        );
        notification.setCreatedAt(event.occurredAt());

        notificationRepository.save(notification);

        log.info(
                "Created notification for event {}, recipient {}",
                event.eventId(),
                event.recipientId()
        );
    }

    private TaskAssignedEventV1 deserialize(String payload) {
        try {
            return objectMapper.readValue(
                    payload,
                    TaskAssignedEventV1.class
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Cannot deserialize TaskAssignedEventV1",
                    exception
            );
        }
    }
}