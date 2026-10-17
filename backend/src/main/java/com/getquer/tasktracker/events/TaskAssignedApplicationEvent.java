package com.getquer.tasktracker.events;

import org.springframework.context.ApplicationEvent;


public class TaskAssignedApplicationEvent extends ApplicationEvent {
    private final TaskAssignedEventV1 taskAssignedEventV1;

    public TaskAssignedApplicationEvent(Object source, TaskAssignedEventV1 taskAssignedEventV1) {
        super(source);
        this.taskAssignedEventV1 = taskAssignedEventV1;
    }
    public TaskAssignedEventV1 payload(){
        return taskAssignedEventV1;
    }
}
