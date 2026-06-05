package com.getquer.tasktracker;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.scheduling.config.Task;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
    public TaskDTO getTaskByID(Long id){
        TaskEntity task = taskRepository.findById(id).orElseThrow(
                ()->new EntityNotFoundException("Not task with id = " + id));
        return mapToDTO(task);
    }
    public List<TaskDTO> getAllTasksByStatus(TaskStatus status){
        List<TaskEntity>tasks = taskRepository.findByStatus(status);
        List<TaskDTO> dtos = new ArrayList<>();

        for (int i = 0; i < tasks.size();i++){
            TaskEntity task = tasks.get(i);
            dtos.add(mapToDTO(task));
        }
        return dtos;
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
