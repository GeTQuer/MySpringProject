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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }
    public TaskDTO createTask(TaskDTO taskDTO, String creatorUsername)
    {
        UserEntity creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new RuntimeException("Создатель не найден: " + creatorUsername));

        // Определяем, кому назначаем задачу
        String assignedUsername = taskDTO.assignedUsername();
        UserEntity targetUser = creator; // по умолчанию самому себе

        if (assignedUsername != null && !assignedUsername.isEmpty()
                && (creator.getRole().equals("MANAGER") || creator.getRole().equals("ADMIN"))) {
            targetUser = userRepository.findByUsername(assignedUsername)
                    .orElseThrow(() -> new RuntimeException("Целевой пользователь не найден: " + assignedUsername));
        }

        TaskEntity task = new TaskEntity();

        task.setContent(taskDTO.content());
        task.setFullNameEmployee(taskDTO.fullNameEmployee());
        task.setStatus(TaskStatus.valueOf(taskDTO.status()));
        task.setUser(targetUser);
        TaskEntity savedTask = taskRepository.save(task);
        return mapToDTO(savedTask);
    }

    public Page<TaskDTO> getAllTaskGlobally(int page,int size){
        Pageable pageable = PageRequest.of(page,size,Sort.by("id").descending());
        Page<TaskEntity> taskPage = taskRepository.findAll(pageable);
        return taskPage.map(task->mapToDTO(task));
    }
    public Page<TaskDTO> getAllTaskGloballyByStatus(TaskStatus status,int page,int size){
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<TaskEntity> taskPage = taskRepository.findAllByStatus(status,pageable);
        return taskPage.map(task-> mapToDTO(task));
    }
    public Page<TaskDTO> getAllTasks(String username,int page,int size) {
        Pageable pageable = PageRequest.of(page,size,Sort.by("id").descending());
        Page<TaskEntity> taskPage = taskRepository.findAllByUserUsername(username,pageable);
        return taskPage.map(task->mapToDTO(task));
    }

    public Page<TaskDTO> getAllTasksByStatus(String username, TaskStatus status,int page,int size) {
        Pageable pageable = PageRequest.of(page,size,Sort.by("id").descending());
        Page<TaskEntity> taskPage = taskRepository.findAllByUserUsernameAndStatus(username,status,pageable);
        return taskPage.map(task->mapToDTO(task));
//        return taskRepository.findAllByUserUsernameAndStatus(username, status)
//                .stream()
//                .map(this::mapToDTO)
//                .toList(); // Для Java 16+
    }

    public void deleteByIdAndUsername(Long id,String username)
    {
        TaskEntity task = taskRepository.findByIdAndUserUsername(id,username).orElseThrow(
                ()-> new EntityNotFoundException("Task with id = "+ id + " not found or you don't have permission to modify if")
        );
        taskRepository.delete(task);
    }


    public TaskDTO updatedData(Long id,TaskDTO updateData, String username){
        TaskEntity task = taskRepository.findByIdAndUserUsername(id,username)
                .orElseThrow(()-> new EntityNotFoundException("Task with id = "+ id + " not found or you don't have permission to modify if"));
        task.setContent(updateData.content());
        task.setStatus(TaskStatus.valueOf(updateData.status()));
        task.setFullNameEmployee(updateData.fullNameEmployee());
        taskRepository.save(task);
        return mapToDTO(task);
    }
    public TaskDTO getTaskByID(Long id,String username){
        TaskEntity task = taskRepository.findByIdAndUserUsername(id,username)
                .orElseThrow(() -> new EntityNotFoundException("Task with id = "+ id + " not found or you don't have permission to modify if"));
        return mapToDTO(task);
    }

    // Получить задачу по ID без проверки владельца (для MANAGER)
    public TaskDTO getTaskByIdForManager(Long id) {
        TaskEntity task = taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Not task with id = " + id));
        return mapToDTO(task);
    }

    // Обновить задачу без проверки владельца (для MANAGER)
    public TaskDTO updateTaskForManager(Long id, TaskDTO updateData) {
        TaskEntity task = taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id = " + id));
        task.setContent(updateData.content());
        task.setStatus(TaskStatus.valueOf(updateData.status()));
        task.setFullNameEmployee(updateData.fullNameEmployee());
        taskRepository.save(task);
        return mapToDTO(task);
    }

    private TaskDTO mapToDTO(TaskEntity taskEntity){
        return new TaskDTO(
                taskEntity.getId(),
                taskEntity.getContent(),
                taskEntity.getFullNameEmployee(),
                taskEntity.getStatus().name(),
                taskEntity.getUser().getUsername()
        );
    }

}
