package com.getquer.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskAssignedEventConsumerTest {

    @Mock
    private NotificationRepository notificationRepository;

    private ObjectMapper objectMapper;
    private TaskAssignedEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        consumer = new TaskAssignedEventConsumer(objectMapper, notificationRepository);
    }

    @Test
    void consumeCreatesNotificationFromEvent() throws Exception {
        TaskAssignedEventV1 event = event();
        when(notificationRepository.existsByEventId(event.eventId())).thenReturn(false);

        consumer.consume(objectMapper.writeValueAsString(event));

        ArgumentCaptor<NotificationEntity> captor =
                ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(captor.capture());

        NotificationEntity saved = captor.getValue();
        assertEquals(event.eventId(), saved.getEventId());
        assertEquals(event.recipientId(), saved.getRecipientId());
        assertEquals(event.taskId(), saved.getTaskId());
        assertEquals(NotificationType.TASK_ASSIGNED, saved.getType());
        assertEquals(event.occurredAt(), saved.getCreatedAt());
    }

    @Test
    void consumeIgnoresAlreadyProcessedEvent() throws Exception {
        TaskAssignedEventV1 event = event();
        when(notificationRepository.existsByEventId(event.eventId())).thenReturn(true);

        consumer.consume(objectMapper.writeValueAsString(event));

        verify(notificationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private TaskAssignedEventV1 event() {
        return new TaskAssignedEventV1(
                UUID.randomUUID(),
                27L,
                6L,
                "InfoSecMANAGER",
                10L,
                "Проверка перехода",
                Instant.parse("2026-08-27T09:40:42Z")
        );
    }
}
