package com.getquer.tasktracker.security;

import com.getquer.tasktracker.Entities.TaskEntity;
import com.getquer.tasktracker.Entities.UserEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class TaskAccessPolicy {

    public void checkCanAccess(TaskEntity task, UserEntity actor) {
        switch (actor.getRole()) {
            case "ADMIN" -> {
                return;
            }
            case "MANAGER" -> checkSameDepartment(task, actor);
            case "USER" -> checkAssignedUser(task, actor);
            default -> throw new AccessDeniedException("Access denied");
        }
    }

    private void checkSameDepartment(TaskEntity task, UserEntity actor) {
        if (task.getDepartment() == null
                || actor.getDepartment() == null
                || !Objects.equals(
                task.getDepartment().getId(),
                actor.getDepartment().getId())) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private void checkAssignedUser(TaskEntity task, UserEntity actor) {
        if (task.getUser() == null
                || !Objects.equals(task.getUser().getId(), actor.getId())) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
