package com.getquer.tasktracker.service;

import com.getquer.tasktracker.Entities.UserEntity;
import com.getquer.tasktracker.Repositories.UserRepository;
import com.getquer.tasktracker.DTOs.TaskDTO;
import com.getquer.tasktracker.Entities.TaskEntity;
import com.getquer.tasktracker.Repositories.TaskRepository;
import com.getquer.tasktracker.TaskStatus;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.*;
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

    private Page<TaskDTO> convertToTaskPage(Page<Long> idPage, Pageable pageable) {
        // Если база ничего не нашла по фильтрам, сразу отдаем пустую страницу
        if (idPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> ids = idPage.getContent();

        List<TaskEntity> tasks = taskRepository.findAllByIdsWithUser(ids);

        List<TaskDTO> dtos = tasks.stream()
                .sorted((t1, t2) -> t2.getId().compareTo(t1.getId()))
                .map(this::mapToDTO)
                .toList();
        return new PageImpl<>(dtos, pageable, idPage.getTotalElements());
    }


    public TaskDTO createTask(TaskDTO taskDTO,String currentUsername)
    {
        UserEntity creator = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Создатель не найден: " + currentUsername));

        String assignedUsername = taskDTO.assignedUsername();
        UserEntity targetUser = creator;

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
        Page<Long> idPages = taskRepository.findAllIds(pageable);
        return convertToTaskPage(idPages,pageable);
    }
    public Page<TaskDTO> getAllTaskGloballyByStatus(TaskStatus status,int page,int size){
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Long> idPages =taskRepository.findAllByStatus(status,pageable);

        return convertToTaskPage(idPages,pageable);
    }
    public Page<TaskDTO> getAllTasks(String username,int page,int size) {
        Pageable pageable = PageRequest.of(page,size,Sort.by("id").descending());

        Page<Long> idPages = taskRepository.findAllByUserUsername(username,pageable);
        return convertToTaskPage(idPages,pageable);
    }

    public Page<TaskDTO> getAllTasksByStatus(String username, TaskStatus status,int page,int size) {
        Pageable pageable = PageRequest.of(page,size,Sort.by("id").descending());
        Page<Long> idPages = taskRepository.findAllByUserUsernameAndStatus(username,status,pageable);
        return convertToTaskPage(idPages,pageable);
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
