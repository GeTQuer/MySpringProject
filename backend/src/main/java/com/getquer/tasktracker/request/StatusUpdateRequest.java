package com.getquer.tasktracker.request;

import com.getquer.tasktracker.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull(message = "Статус задачи должен быть указан")
        TaskStatus status
)
{}
