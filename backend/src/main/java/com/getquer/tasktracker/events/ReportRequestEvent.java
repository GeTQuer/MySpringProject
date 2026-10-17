package com.getquer.tasktracker.events;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class ReportRequestEvent extends ApplicationEvent {
    private final UUID uuid;

    public ReportRequestEvent(Object source, UUID uuid){
        super(source);
        this.uuid= uuid;
    }
    public UUID payload(){
        return uuid;
    }
}
