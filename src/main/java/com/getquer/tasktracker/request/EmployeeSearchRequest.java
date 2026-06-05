package com.getquer.tasktracker.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmployeeSearchRequest(
        @NotBlank(message = "Имя сотрудника обязательно!")
        @Size(min = 3, message = "Имя сотрудника должно содержать минимум 3 символа")
        String fullNameEmployee
)
{}
