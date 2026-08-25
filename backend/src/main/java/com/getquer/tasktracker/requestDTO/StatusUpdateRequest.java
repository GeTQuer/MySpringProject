package com.getquer.tasktracker.requestDTO;

import com.getquer.tasktracker.Enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull(message = "Статус задачи должен быть указан")
        TaskStatus status
)
{}
