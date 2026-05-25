package com.getquer.tasktracker;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.scheduling.config.Task;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
public class TaskService {
    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository)
    {
        this.taskRepository = taskRepository;

    }
    public TaskDTO createTask(String content,String name)
    {
        TaskEntity newTask = new TaskEntity();
        newTask.setContent(content);
        newTask.setEmployee(name);
        newTask.setStatus(TaskStatus.OPEN);
        TaskEntity savedTask = taskRepository.save(newTask);
        return mapToDTO(savedTask);
    }

    public List<TaskDTO> getAllTasks(){

        return taskRepository.findAll().stream().map(this::mapToDTO).toList();
    }

    public List<TaskDTO> getAllTasksByEmployee(String name){
        return taskRepository.findByFullNameEmployee(name).stream().map(this::mapToDTO).toList();
    }

    public String deleteById(Long id)
    {
        if (!taskRepository.existsById(id))
            return "Not found ID";
        taskRepository.deleteById(id);
        return "deleted";
    }

    public TaskDTO updatedStatus(Long id,TaskStatus newStatus){
        TaskEntity task = taskRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Not found ID"));
        task.setStatus(newStatus);

        return mapToDTO(taskRepository.save(task));
    }

    private TaskDTO mapToDTO(TaskEntity taskEntity){
        return new TaskDTO(
                taskEntity.getId(),
                taskEntity.getContent(),
                taskEntity.getEmployee(),
                taskEntity.getStatus()
        );
    }
}
