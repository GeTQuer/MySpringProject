package com.getquer.tasktracker.service;

import com.getquer.tasktracker.Entities.UserEntity;
import com.getquer.tasktracker.Repositories.UserRepository;
import com.getquer.tasktracker.requestDTO.TaskCreateRequest;
import com.getquer.tasktracker.responseDTO.TaskDTO;
import com.getquer.tasktracker.Entities.TaskEntity;
import com.getquer.tasktracker.Repositories.TaskRepository;
import com.getquer.tasktracker.TaskStatus;
import com.getquer.tasktracker.security.TaskAccessPolicy;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskAccessPolicy taskAccessPolicy;
    public TaskService(TaskRepository taskRepository, UserRepository userRepository, TaskAccessPolicy taskAccessPolicy) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskAccessPolicy = taskAccessPolicy;
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

    @Transactional
    public TaskDTO createTask(TaskCreateRequest request, String currentUsername) {
        UserEntity creator = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new EntityNotFoundException("Создатель не найден: " + currentUsername));

        String assignedUsername = request.assignedUsername();
        UserEntity targetUser = creator;

        if (assignedUsername != null && !assignedUsername.isBlank()) {
            boolean canAssign = "MANAGER".equals(creator.getRole())
                    || "ADMIN".equals(creator.getRole());

            if (!canAssign) {
                throw new AccessDeniedException("Only managers and admins can assign tasks");
            }

            targetUser = userRepository.findByUsername(assignedUsername)
                    .orElseThrow(() -> new EntityNotFoundException("Целевой пользователь не найден: " + assignedUsername));

            if ("MANAGER".equals(creator.getRole())) {
                boolean sameDepartment = creator.getDepartment() != null
                        && targetUser.getDepartment() != null
                        && Objects.equals(
                        creator.getDepartment().getId(),
                        targetUser.getDepartment().getId()
                );
                boolean targetIsEmployee = "USER".equals(targetUser.getRole());

                if (!sameDepartment || !targetIsEmployee) {
                    throw new AccessDeniedException(
                            "Manager can assign tasks only to employees of their department"
                    );
                }
            }
        }

        TaskEntity task = new TaskEntity();
        task.setContent(request.content());
        task.setFullNameEmployee(targetUser.getUsername());
        updateStatus(task, request.status());

        task.setUser(targetUser);


        task.setDepartment(targetUser.getDepartment());

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

    @Transactional
    public void deleteByIdAndUsername(Long id, Long version, String username)
    {
        UserEntity user = findActor(username);
        Long departmentId = user.getDepartment().getId();
        TaskEntity task = taskRepository.findByIdAndUsernameAndDepartmentId(id,username,departmentId).orElseThrow(
                ()-> new EntityNotFoundException("Task with id = "+ id + " not found or you don't have permission to modify if")
        );
        taskAccessPolicy.checkCanAccess(task,user);
        checkVersion(task, version);

        taskRepository.delete(task);
    }

    @Transactional
    public TaskDTO updatedData(Long id,TaskDTO updateData, String username){
        UserEntity user = findActor(username);
        Long departmentId = user.getDepartment().getId();
        TaskEntity task = taskRepository.findByIdAndUsernameAndDepartmentId(id,username,departmentId)
                .orElseThrow(()-> new EntityNotFoundException("Task with id = "+ id + " not found or you don't have permission to modify if"));

        checkVersion(task, updateData.version());
        task.setContent(updateData.content());
        updateStatus(task, updateData.status());
        task.setFullNameEmployee(updateData.fullNameEmployee());
        taskRepository.save(task);
        taskRepository.flush();
        return mapToDTO(task);
    }

    public TaskDTO getTaskByID(Long id,String username){
        UserEntity user = findActor(username);
        Long departmentId = user.getDepartment().getId();
        TaskEntity task = taskRepository.findByIdAndUsernameAndDepartmentId(id,username,departmentId)
                .orElseThrow(() -> new EntityNotFoundException("Task with id = "+ id + " not found or you don't have permission to modify if"));
        return mapToDTO(task);
    }

    // Получить задачу по ID без проверки владельца (для ADMIN)
    public TaskDTO getTaskByIdForAdmin(Long id) {
        TaskEntity task = findTask(id);
        return mapToDTO(task);
    }

    // Получить задачу по ID с проверкой отдела (для MANAGER)
    public TaskDTO getTaskByIdForManagerWithDepartmentCheck(Long id, String username) {
        UserEntity actor = findActor(username);

        TaskEntity task = findTask(id);

        taskAccessPolicy.checkCanAccess(task, actor);

        return mapToDTO(task);
    }

    // Обновить задачу без проверки владельца (для ADMIN)
    @Transactional
    public TaskDTO updateTaskForAdmin(Long id, TaskDTO updateData) {
        TaskEntity task = findTask(id);
        checkVersion(task, updateData.version());
        task.setContent(updateData.content());
        updateStatus(task, updateData.status());
        task.setFullNameEmployee(updateData.fullNameEmployee());
        taskRepository.save(task);
        taskRepository.flush();
        return mapToDTO(task);
    }

    // Обновить задачу с проверкой отдела (для MANAGER)
    @Transactional
    public TaskDTO updateTaskForManagerWithDepartmentCheck(Long id, TaskDTO updateData, String username) {
        UserEntity actor = findActor(username);
        TaskEntity task = findTask(id);

        taskAccessPolicy.checkCanAccess(task, actor);
        checkVersion(task, updateData.version());

        task.setContent(updateData.content());
        updateStatus(task, updateData.status());
        task.setFullNameEmployee(updateData.fullNameEmployee());
        taskRepository.save(task);
        taskRepository.flush();
        return mapToDTO(task);
    }

    public Page<TaskDTO> getAllDepartmentTasks(String managerName, int page, int size){
        UserEntity manager = userRepository.findByUsername(managerName)
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        if (manager.getDepartment() == null) {
            throw new RuntimeException("Manager must be assigned to a department");
        }

        Long departmentId = manager.getDepartment().getId();

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Long> tasksIds = taskRepository.findAllTasksByDepartmentId(departmentId, pageable);

        return convertToTaskPage(tasksIds, pageable);
    }

    public Page<TaskDTO> getAllDepartmentTasksByStatus(String managerName, TaskStatus status, int page, int size){
        UserEntity manager = userRepository.findByUsername(managerName)
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        if (manager.getDepartment() == null) {
            throw new RuntimeException("Manager must be assigned to a department");
        }

        Long departmentId = manager.getDepartment().getId();

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Long> tasksIds = taskRepository.findAllTasksByDepartmentIdAndStatus(departmentId, status, pageable);

        return convertToTaskPage(tasksIds, pageable);
    }

    @Transactional
    public void deleteByIdForManager(Long id, Long version, String username) {
        UserEntity actor = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        TaskEntity task = findTask(id);

        taskAccessPolicy.checkCanAccess(task, actor);
        checkVersion(task, version);

        taskRepository.delete(task);
    }
    @Transactional
    public void deleteById(Long id, Long version) {
        TaskEntity task = findTask(id);
        checkVersion(task, version);

        taskRepository.delete(task);
    }

    private TaskDTO mapToDTO(TaskEntity taskEntity){
        return new TaskDTO(
                taskEntity.getId(),
                taskEntity.getContent(),
                taskEntity.getFullNameEmployee(),
                taskEntity.getStatus().name(),
                taskEntity.getUser().getUsername(),
                taskEntity.getVersion()
        );
    }

    private TaskEntity findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task with id = "+ taskId + " not found or you don't have permission to modify if"));
    }

    private UserEntity findActor(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }


    private void checkVersion(
            TaskEntity task,
            Long expectedVersion
    ) {
        if (expectedVersion == null || !Objects.equals(task.getVersion(), expectedVersion)) {
            throw new ObjectOptimisticLockingFailureException(
                    TaskEntity.class,
                    task.getId()
            );
        }
    }

    private void updateStatus(TaskEntity task, String statusValue){
        TaskStatus oldStatus = task.getStatus();
        TaskStatus newStatus = TaskStatus.valueOf(statusValue);

        if (oldStatus != TaskStatus.DONE && newStatus == TaskStatus.DONE){
            task.setCompletedAt(LocalDateTime.now());
        }
        else if (oldStatus == TaskStatus.DONE && newStatus != TaskStatus.DONE) {
            task.setCompletedAt(null);
        }
        task.setStatus(newStatus);
    }

}
