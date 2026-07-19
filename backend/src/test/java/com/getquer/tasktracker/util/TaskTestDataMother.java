package com.getquer.tasktracker.util;

import com.getquer.tasktracker.Entities.DepartmentEntity;
import com.getquer.tasktracker.Entities.TaskEntity;
import com.getquer.tasktracker.Entities.UserEntity;
import com.getquer.tasktracker.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class TaskTestDataMother {

    public static DepartmentEntity createTestDepartment(Long id, String name) {
        DepartmentEntity department = new DepartmentEntity();
        department.setId(id);
        department.setName(name);
        return department;
    }

    public static UserEntity createTestUser(Long id, String username, String role) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        return user;
    }

    public static UserEntity createTestUserWithDepartment(Long id, String username, String role, DepartmentEntity department) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        user.setDepartment(department);
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