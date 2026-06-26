package com.getquer.tasktracker.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;

public record TaskDTO (
        Long id,
        String content,
        String fullNameEmployee,
        String status,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String assignedUsername
){}
