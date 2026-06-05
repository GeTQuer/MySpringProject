package com.getquer.tasktracker.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record TaskCreateRequest (
   @NotBlank(message = "Описание задачи не может быть пустым!!!")
   @Size(min = 3, message = "Описание задачи должно содержать минимум 3 символа")
    String content,
    @NotBlank(message ="Имя сотрудника обязательно")
    String name
){}
