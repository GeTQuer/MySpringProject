package com.getquer.tasktracker;

import com.getquer.tasktracker.Repositories.TaskRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class TaskSheduler {
    private final TaskRepository taskRepository;

    public TaskSheduler(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void processOverdue(){
        taskRepository.markOverdueTasks(LocalDateTime.now());
    }
}
