package com.getquer.tasktracker.util;

import com.getquer.tasktracker.Entities.NotificationEntity;
import com.getquer.tasktracker.Enums.NotificationType;
import com.getquer.tasktracker.service.NotificationService;
import com.getquer.tasktracker.events.TaskAssignedEventV1;

import java.time.Instant;
import java.util.UUID;

public final class NotificationTestDataMother {

    public static final Instant CREATED_AT =
            Instant.parse("2026-08-25T10:00:00Z");

    public static final Instant READ_AT =
            Instant.parse("2026-08-25T11:00:00Z");

    private NotificationTestDataMother() {
    }

    public static NotificationEntity createUnreadNotification(
            Long id,
            Long recipientId
    ) {
        NotificationEntity notification = new NotificationEntity();

        notification.setId(id);
        notification.setEventId(new UUID(0L, id));
        notification.setRecipientId(recipientId);
        notification.setActorId(10L);
        notification.setTaskId(100L + id);
        notification.setType(NotificationType.TASK_ASSIGNED);
        notification.setTitle("Вам назначена новая задача");
        notification.setMessage("manager назначил вам задачу");
        notification.setCreatedAt(CREATED_AT);
        notification.setReadAt(null);

        return notification;
    }

    public static NotificationEntity createReadNotification(
            Long id,
            Long recipientId
    ) {
        NotificationEntity notification =
                createUnreadNotification(id, recipientId);

        notification.setReadAt(READ_AT);

        return notification;
    }

    public static TaskAssignedEventV1 createTaskAssignedEvent(
            Long taskId,
            Long actorId,
            Long recipientId
    ) {
        return new TaskAssignedEventV1(
                new UUID(1L, taskId),
                taskId,
                actorId,
                "manager",
                recipientId,
                "Подготовить отчёт",
                CREATED_AT
        );
    }
}