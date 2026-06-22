package com.getquer.tasktracker.Repositories;

import com.getquer.tasktracker.Entities.TaskEntity;
import com.getquer.tasktracker.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.config.Task;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long>
{
    List<TaskEntity> findAllByUserUsernameAndStatus(String username, TaskStatus status);

    List<TaskEntity> findAllByUserUsername(String username);

    List<TaskEntity> findAllByStatus(TaskStatus status);

    Optional<TaskEntity> findByIdAndUserUsername(Long id, String username);

}
