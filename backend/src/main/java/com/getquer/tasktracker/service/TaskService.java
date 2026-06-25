package com.getquer.tasktracker.service;

import com.getquer.tasktracker.Entities.UserEntity;
import com.getquer.tasktracker.Repositories.UserRepository;
import com.getquer.tasktracker.DTOs.TaskDTO;
import com.getquer.tasktracker.Entities.TaskEntity;
import com.getquer.tasktracker.Repositories.TaskRepository;
import com.getquer.tasktracker.TaskStatus;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }
    public TaskDTO createTask(TaskDTO taskDTO, String username)
    {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        TaskEntity newTask = new TaskEntity();
        newTask.setContent(taskDTO.content());
        newTask.setFullNameEmployee(taskDTO.fullNameEmployee());
        newTask.setStatus(TaskStatus.valueOf(taskDTO.status()));
        newTask.setUser(user);
        TaskEntity savedTask = taskRepository.save(newTask);
        return mapToDTO(savedTask);
    }

    public List<TaskDTO> getAllTaskGlobally(){
        List<TaskEntity> tasks = taskRepository.findAll();
        List<TaskDTO> dtos = new ArrayList<>();
        for (TaskEntity task : tasks) {
            dtos.add(mapToDTO(task));
        }
        return dtos;
    }
    public List<TaskDTO> getAllTaskGloballyByStatus(TaskStatus status){
        List<TaskEntity> tasks = taskRepository.findAllByStatus(status);
        List<TaskDTO> dtos = new ArrayList<>();
        for (TaskEntity task : tasks) {
            dtos.add(mapToDTO(task));
        }
        return dtos;
    }
    public List<TaskDTO> getAllTasks(String username) {
        List<TaskEntity> tasks = taskRepository.findAllByUserUsername(username);
        List<TaskDTO> dtos = new ArrayList<>();
        for (TaskEntity task : tasks) {
            dtos.add(mapToDTO(task));
        }
        return dtos;
    }

    public List<TaskDTO> getAllTasksByStatus(String username, TaskStatus status) {
        List<TaskEntity> tasks = taskRepository.findAllByUserUsernameAndStatus(
                username,status
        );
        List<TaskDTO> dtos = new ArrayList<>();
        for (TaskEntity task : tasks) {
            dtos.add(mapToDTO(task));
        }
        return dtos;
//        return taskRepository.findAllByUserUsernameAndStatus(username, status)
//                .stream()
//                .map(this::mapToDTO)
//                .toList(); // Для Java 16+
    }

    public void deleteById(Long id,String username)
    {
        TaskEntity task = taskRepository.findByIdAndUserUsername(id,username).orElseThrow(
                ()-> new EntityNotFoundException("Not task with id = " + id)
        );
        taskRepository.delete(task);
    }


    public TaskDTO updatedData(Long id,TaskDTO updateData, String username){
        TaskEntity task = taskRepository.findByIdAndUserUsername(id,username)
                .orElseThrow(()-> new EntityNotFoundException("Task not foud with id = " + id));
        task.setContent(updateData.content());
        task.setStatus(TaskStatus.valueOf(updateData.status()));
        task.setFullNameEmployee(updateData.fullNameEmployee());
        taskRepository.save(task);
        return mapToDTO(task);
    }
    public TaskDTO getTaskByID(Long id,String username){
        TaskEntity task = taskRepository.findByIdAndUserUsername(id,username)
                .orElseThrow(() -> new EntityNotFoundException("Not task with id = " + id));
        return mapToDTO(task);
    }


    private TaskDTO mapToDTO(TaskEntity taskEntity){
        return new TaskDTO(
                taskEntity.getId(),
                taskEntity.getContent(),
                taskEntity.getFullNameEmployee(),
                taskEntity.getStatus().name()
        );
    }

}
