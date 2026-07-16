package com.getquer.tasktracker.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TaskDTO (
        Long id,
        @NotBlank(message = "Содержание задачи не должно быть пустым")
        String content,
        @NotBlank(message = "ФИО сотрудника не должно быть пустым")
        String fullNameEmployee,
        @NotBlank(message = "Статус задачи должен быть указан")
        String status,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String assignedUsername
){}
