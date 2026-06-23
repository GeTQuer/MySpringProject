package com.getquer.tasktracker;

import com.getquer.tasktracker.Repositories.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TaskSheduler {
    private final TaskRepository taskRepository;

    public TaskSheduler(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void processOverdue(){
        LocalDateTime now = LocalDateTime.now();
        taskRepository.markOverdueTasks(now);

        LocalDateTime sevenDaysAgo = now.minusDays(7);
        taskRepository.deleteOldOverdueTasks(sevenDaysAgo);
    }
}
