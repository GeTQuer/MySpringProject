package com.getquer.tasktracker.service;

import com.getquer.tasktracker.Entities.UserEntity;
import com.getquer.tasktracker.Repositories.UserRepository;
import com.getquer.tasktracker.responseDTO.TaskDTO;
import com.getquer.tasktracker.Entities.TaskEntity;
import com.getquer.tasktracker.Repositories.TaskRepository;
import com.getquer.tasktracker.TaskStatus;
import com.getquer.tasktracker.security.TaskAccessPolicy;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
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
    public TaskDTO createTask(TaskDTO taskDTO, String currentUsername) {
        UserEntity creator = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Создатель не найден: " + currentUsername));

        String assignedUsername = taskDTO.assignedUsername();
        UserEntity targetUser = creator;

        if (assignedUsername != null && !assignedUsername.isEmpty()
                && (creator.getRole().equals("MANAGER") || creator.getRole().equals("ADMIN"))) {

            targetUser = userRepository.findByUsername(assignedUsername)
                    .orElseThrow(() -> new RuntimeException("Целевой пользователь не найден: " + assignedUsername));

            if (creator.getRole().equals("MANAGER")) {
                if (targetUser.getDepartment() == null ||
                        !creator.getDepartment().getId().equals(targetUser.getDepartment().getId())) {
                    throw new RuntimeException("Ошибка доступа: вы не можете назначать задачи сотрудникам из другого отдела");
                }
            }
        }

        TaskEntity task = new TaskEntity();
        task.setContent(taskDTO.content());
        task.setFullNameEmployee(taskDTO.fullNameEmployee());
        task.setStatus(TaskStatus.valueOf(taskDTO.status()));

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
    public void deleteByIdAndUsername(Long id,String username)
    {
        UserEntity user = findActor(username);
        Long departmentId = user.getDepartment().getId();
        TaskEntity task = taskRepository.findByIdAndUsernameAndDepartmentId(id,username,departmentId).orElseThrow(
                ()-> new EntityNotFoundException("Task with id = "+ id + " not found or you don't have permission to modify if")
        );
        taskAccessPolicy.checkCanAccess(task,user);

        taskRepository.delete(task);
    }

    @Transactional
    public TaskDTO updatedData(Long id,TaskDTO updateData, String username){
        UserEntity user = findActor(username);
        Long departmentId = user.getDepartment().getId();
        TaskEntity task = taskRepository.findByIdAndUsernameAndDepartmentId(id,username,departmentId)
                .orElseThrow(()-> new EntityNotFoundException("Task with id = "+ id + " not found or you don't have permission to modify if"));
        task.setContent(updateData.content());
        task.setStatus(TaskStatus.valueOf(updateData.status()));
        task.setFullNameEmployee(updateData.fullNameEmployee());
        taskRepository.save(task);
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
    @Transactional(readOnly = true)
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
        task.setContent(updateData.content());
        task.setStatus(TaskStatus.valueOf(updateData.status()));
        task.setFullNameEmployee(updateData.fullNameEmployee());
        taskRepository.save(task);
        return mapToDTO(task);
    }

    // Обновить задачу с проверкой отдела (для MANAGER)
    @Transactional
    public TaskDTO updateTaskForManagerWithDepartmentCheck(Long id, TaskDTO updateData, String username) {
        UserEntity actor = findActor(username);
        TaskEntity task = findTask(id);
        taskAccessPolicy.checkCanAccess(task, actor);

        task.setContent(updateData.content());
        task.setStatus(TaskStatus.valueOf(updateData.status()));
        task.setFullNameEmployee(updateData.fullNameEmployee());
        taskRepository.save(task);
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
    public void deleteByIdForManager(Long id, String username) {
        UserEntity actor = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        TaskEntity task = findTask(id);

        taskAccessPolicy.checkCanAccess(task, actor);

        taskRepository.delete(task);
    }
    @Transactional
    public void deleteById(Long id) {
        TaskEntity task = findTask(id);
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

    private TaskEntity findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task with id = "+ taskId + " not found or you don't have permission to modify if"));
    }

    private UserEntity findActor(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

}
