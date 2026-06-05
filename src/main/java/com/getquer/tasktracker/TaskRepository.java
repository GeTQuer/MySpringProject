package com.getquer.tasktracker;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.config.Task;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long>
{
    public List<TaskEntity>findByFullNameEmployee(String name);
    List<TaskEntity> findByStatus(TaskStatus status);
}
