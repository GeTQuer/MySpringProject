package com.getquer.tasktracker.service;

import com.getquer.tasktracker.Entities.UserEntity;
import com.getquer.tasktracker.Repositories.UserRepository;
import com.getquer.tasktracker.TaskDTO;
import com.getquer.tasktracker.Entities.TaskEntity;
import com.getquer.tasktracker.Repositories.TaskRepository;
import com.getquer.tasktracker.TaskStatus;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.scheduling.config.Task;
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
        newTask.setStatus(taskDTO.status());
        newTask.setUser(user);
        TaskEntity savedTask = taskRepository.save(newTask);
        return mapToDTO(savedTask);
    }

    public List<TaskDTO> getAllTaskGlobally(){
        List<TaskEntity> tasks = taskRepository.findAll();
        List<TaskDTO> dtos = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) {
            dtos.add(mapToDTO(tasks.get(i)));
        }
        return dtos;
    }
    public List<TaskDTO> getAllTaskGloballyByStatus(TaskStatus status){
        List<TaskEntity> tasks = taskRepository.findAllByStatus(status);
        List<TaskDTO> dtos = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) {
            dtos.add(mapToDTO(tasks.get(i)));
        }
        return dtos;
    }
    public List<TaskDTO> getAllTasks(String username) {
        List<TaskEntity> tasks = taskRepository.findAllByUserUsername(username);
        List<TaskDTO> dtos = new ArrayList<>();
        for (int i = 0; i < tasks.size();i++){
            TaskEntity task = tasks.get(i);
            dtos.add(mapToDTO(task));
        }
        return dtos;
    }

    public List<TaskDTO> getAllTasksByStatus(String username, TaskStatus status) {
        List<TaskEntity> tasks = taskRepository.findAllByUserUsernameAndStatus(
                username,status
        );
        List<TaskDTO> dtos = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++){
            TaskEntity task = tasks.get(i);
            dtos.add(mapToDTO(task));
        }
        return dtos;
//        return taskRepository.findAllByUserUsernameAndStatus(username, status)
//                .stream()
//                .map(this::mapToDTO)
//                .toList(); // Для Java 16+
    }

    public String deleteById(Long id,String username)
    {
        TaskEntity task = taskRepository.findByIdAndUsername(id,username).orElseThrow(
                ()-> new EntityNotFoundException("Not task with id = " + id)
        );
        taskRepository.delete(task);
        return "deleted";
    }

    public TaskDTO updatedStatus(Long id,TaskStatus newStatus,String username){
        TaskEntity task = taskRepository.findByIdAndUsername(id,username)
                .orElseThrow(()-> new EntityNotFoundException("Not task with id = " + id));
        task.setStatus(newStatus);
        return mapToDTO(taskRepository.save(task));
    }
    public TaskDTO getTaskByID(Long id,String username){
        TaskEntity task = taskRepository.findByIdAndUsername(id,username)
                .orElseThrow(() -> new EntityNotFoundException("Not task with id = " + id));
        return mapToDTO(task);
    }

    private TaskDTO mapToDTO(TaskEntity taskEntity){
        return new TaskDTO(
                taskEntity.getId(),
                taskEntity.getContent(),
                taskEntity.getFullNameEmployee(),
                taskEntity.getStatus()
        );
    }

}
