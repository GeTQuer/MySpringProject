package com.getquer.tasktracker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.getquer.tasktracker.Entities.OutboxEventEntity;
import com.getquer.tasktracker.Repositories.OutboxEventRepository;
import com.getquer.tasktracker.events.TaskAssignedEventV1;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;


@Service
@RequiredArgsConstructor
public class OutboxService {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private static final String TASK_ASSIGNED_EVENT = "TASK_ASSIGNED_V1";

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveTaskAssignedEvent(TaskAssignedEventV1 event){
        OutboxEventEntity outboxEvent = new OutboxEventEntity();
        outboxEvent.setEventId(event.eventId());
        outboxEvent.setEventType(TASK_ASSIGNED_EVENT);
        outboxEvent.setTaskId(event.taskId());
        outboxEvent.setPayload(serialize(event));
        outboxEvent.setCreatedAt(Instant.now());

        outboxEventRepository.save(outboxEvent);
    }

    private String serialize(TaskAssignedEventV1 event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Не удалось сериализовать событие назначения задачи",
                    exception
            );
        }
    }
}
