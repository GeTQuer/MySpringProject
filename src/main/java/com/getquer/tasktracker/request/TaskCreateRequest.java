package com.getquer.tasktracker.request;

public record TaskCreateRequest (
    String content,
    String name
){}
