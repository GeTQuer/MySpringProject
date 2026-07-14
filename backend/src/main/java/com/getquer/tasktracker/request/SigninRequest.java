package com.getquer.tasktracker.request;

import jakarta.validation.constraints.NotBlank;

public record SigninRequest(
        @NotBlank(message = "Имя пользователя не должен быть пустым")
        String username,
        @NotBlank(message = "Пароль не должен быть пустым")
        String password) {
}
