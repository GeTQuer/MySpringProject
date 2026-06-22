package com.getquer.tasktracker.request;

import com.getquer.tasktracker.TaskStatus;

public record StatusUpdateRequest(
        TaskStatus status
)
{}
