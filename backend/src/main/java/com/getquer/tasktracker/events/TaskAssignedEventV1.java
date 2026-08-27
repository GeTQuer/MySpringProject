package com.getquer.tasktracker.events;

import java.time.Instant;
import java.util.UUID;

public record TaskAssignedEventV1(
        UUID eventId,
        Long taskId,
        Long actorId,
        String actorUsername,
        Long recipientId,
        String taskTitle,
        Instant occurredAt
) {}