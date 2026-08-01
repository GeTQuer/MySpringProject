package com.getquer.tasktracker.requestDTO;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest (
        @NotBlank(message = "Комментарий не может быть пустым")
        String text
){}
