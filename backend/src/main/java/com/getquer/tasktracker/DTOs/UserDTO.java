package com.getquer.tasktracker.DTOs;

import com.getquer.tasktracker.Grades.Seniority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserDTO(

        Long id,
        @NotBlank(message = "Имя пользователя не должно быть пустым")
        String username,
        @NotBlank(message = "Роль пользователя должна быть указана")
        String role,
        @NotBlank(message = "Название отдела должно быть указана")
        String department,
        @NotNull(message = "Грейд пользователя должен быть указан")
        Seniority seniority
) {
}
