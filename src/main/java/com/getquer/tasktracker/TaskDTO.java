package com.getquer.tasktracker;

public record TaskDTO (
    Long id,
    String content,
    String fullNameEmployee,
    TaskStatus status
){}
