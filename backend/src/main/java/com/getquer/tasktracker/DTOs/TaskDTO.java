package com.getquer.tasktracker.DTOs;

public record TaskDTO (
        Long id,
        String content,
        String fullNameEmployee,
        String status
){}
