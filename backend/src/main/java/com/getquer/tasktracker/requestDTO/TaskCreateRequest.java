package com.getquer.tasktracker.requestDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskCreateRequest (
        @NotBlank(message = "Содержание задачи не должно быть пустым")
        @Size(min = 3, message = "Содержание задачи должно содержать минимум 3 символа")
        String content,
        @NotBlank(message = "Статус задачи должен быть указан")
        String status,
        String assignedUsername
){}
