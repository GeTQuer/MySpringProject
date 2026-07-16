package com.getquer.tasktracker.request;

import com.getquer.tasktracker.Grades.Seniority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "Имя пользователя не должно быть пустым")
        @Size(min = 3, max = 50, message = "Имя пользователя должно содержать от 3 до 50 символов")
        String username,
        @NotBlank(message = "Пароль не должен быть пустым")
        @Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
        String password,
        @NotBlank(message = "Название отдела не должно быть пустым")
        String department,
        @NotNull(message = "Грейд пользователя должен быть указан")
        Seniority seniority)
{

}
