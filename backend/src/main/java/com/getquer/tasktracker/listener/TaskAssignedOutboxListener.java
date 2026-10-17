package com.getquer.tasktracker.listener;

import com.getquer.tasktracker.events.TaskAssignedApplicationEvent;
import com.getquer.tasktracker.events.TaskAssignedEventV1;
import com.getquer.tasktracker.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskAssignedOutboxListener implements ApplicationListener<TaskAssignedApplicationEvent> {
    private final OutboxService outboxService;

    @Override
    public void onApplicationEvent(TaskAssignedApplicationEvent event){
        log.info("Sync listener: {}", Thread.currentThread().getName());

        outboxService.saveTaskAssignedEvent(event.payload());
    }
}
