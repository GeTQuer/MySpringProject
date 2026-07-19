package com.getquer.tasktracker.requestDTO;

import com.getquer.tasktracker.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull(message = "Статус задачи должен быть указан")
        TaskStatus status
)
{}
