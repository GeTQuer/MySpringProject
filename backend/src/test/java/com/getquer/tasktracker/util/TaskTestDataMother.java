package com.getquer.tasktracker.util;

import com.getquer.tasktracker.Entities.TaskEntity;
import com.getquer.tasktracker.Entities.UserEntity;
import com.getquer.tasktracker.TaskStatus;

public class TaskTestDataMother {

    public static UserEntity createTestUser(Long id, String username, String role) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        return user;
    }

    public static TaskEntity createTestTask(Long id, String content, TaskStatus status, UserEntity user) {
        TaskEntity task = new TaskEntity();
        task.setId(id);
        task.setContent(content);
        task.setStatus(status);
        task.setUser(user);
        task.setFullNameEmployee(user.getUsername());
        return task;
    }
}