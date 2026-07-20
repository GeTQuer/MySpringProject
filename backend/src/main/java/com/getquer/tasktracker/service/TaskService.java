package com.getquer.tasktracker.service;

import com.getquer.tasktracker.Entities.UserEntity;
import com.getquer.tasktracker.Repositories.UserRepository;
import com.getquer.tasktracker.responseDTO.TaskDTO;
import com.getquer.tasktracker.Entities.TaskEntity;
import com.getquer.tasktracker.Repositories.TaskRepository;
import com.getquer.tasktracker.TaskStatus;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

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
        task.setDepartment(creator.getDepartment());

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

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Если у пользователя нет отдела, возвращаем только его задачи без фильтрации по отделу
        if (user.getDepartment() == null) {
            Page<Long> idPages = taskRepository.findAllByUserUsername(username, null, pageable);
            return convertToTaskPage(idPages, pageable);
        }

        Long currentDepartmentId = user.getDepartment().getId();
        Page<Long> idPages = taskRepository.findAllByUserUsername(username,currentDepartmentId,pageable);
        return convertToTaskPage(idPages,pageable);
    }

    public Page<TaskDTO> getAllTasksByStatus(String username, TaskStatus status,int page,int size) {
        Pageable pageable = PageRequest.of(page,size,Sort.by("id").descending());
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Если у пользователя нет отдела, возвращаем только его задачи без фильтрации по отделу
        if (user.getDepartment() == null) {
            Page<Long> idPages = taskRepository.findAllByUserUsernameAndStatus(username, status, null, pageable);
            return convertToTaskPage(idPages, pageable);
        }

        Long currentDepartmentId = user.getDepartment().getId();
        Page<Long> idPages = taskRepository.findAllByUserUsernameAndStatus(username,status,currentDepartmentId,pageable);
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

    // Получить задачу по ID с проверкой отдела (для MANAGER)
    public TaskDTO getTaskByIdForManagerWithDepartmentCheck(Long id, String username) {
        UserEntity manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (manager.getDepartment() == null) {
            throw new RuntimeException("Manager must be assigned to a department");
        }

        Long departmentId = manager.getDepartment().getId();

        TaskEntity task = taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));

        if (!departmentId.equals(task.getDepartment().getId())) {
            throw new RuntimeException("You can only view tasks from your department");
        }

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

    // Обновить задачу с проверкой отдела (для MANAGER)
    public TaskDTO updateTaskForManagerWithDepartmentCheck(Long id, TaskDTO updateData, String username) {
        UserEntity manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (manager.getDepartment() == null) {
            throw new RuntimeException("Manager must be assigned to a department");
        }

        Long departmentId = manager.getDepartment().getId();

        TaskEntity task = taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));

        if (!departmentId.equals(task.getDepartment().getId())) {
            throw new RuntimeException("You can only update tasks from your department");
        }

        task.setContent(updateData.content());
        task.setStatus(TaskStatus.valueOf(updateData.status()));
        task.setFullNameEmployee(updateData.fullNameEmployee());
        taskRepository.save(task);
        return mapToDTO(task);
    }

    public Page<TaskDTO> getAllDepartmentTasks(Long id,int page, int size){
        Pageable pageable = PageRequest.of(page,size, Sort.by("id").descending());
        Page<Long> tasksIds = taskRepository.findAllTasksByDepartmentId(id,pageable);
        return convertToTaskPage(tasksIds,pageable);
    }

    public Page<TaskDTO> getAllDepartmentTasksByStatus(Long id,TaskStatus status,int page, int size){
        Pageable pageable = PageRequest.of(page,size, Sort.by("id").descending());
        Page<Long> tasksIds = taskRepository.findAllTasksByDepartmentIdAndStatus(id,status,pageable);
        return convertToTaskPage(tasksIds,pageable);
    }

    public void deleteByIdForManager(Long id, String username) {
        UserEntity manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (manager.getDepartment() == null) {
            throw new RuntimeException("Manager must be assigned to a department");
        }

        Long departmentId = manager.getDepartment().getId();

        TaskEntity task = taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));


        if (!departmentId.equals(task.getDepartment().getId())) {
            throw new RuntimeException("You can only delete tasks from your department");
        }

        taskRepository.delete(task);
    }

    public void deleteById(Long id) {
        TaskEntity task = taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id = " + id));
        taskRepository.delete(task);
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
