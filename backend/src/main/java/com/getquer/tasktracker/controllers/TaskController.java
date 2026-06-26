package com.getquer.tasktracker.controllers;

import com.getquer.tasktracker.DTOs.UserDTO;
import com.getquer.tasktracker.Repositories.UserRepository;
import com.getquer.tasktracker.service.TaskService;
import com.getquer.tasktracker.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;
    private final UserRepository userRepository;
    private final UserService userService;

    public TaskController(TaskService taskService, UserRepository userRepository, UserService userService) {
        this.taskService = taskService;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody TaskDTO taskDTO, Principal principal) {
        // Достаем логин из токена
        String username = principal.getName();

        // Передаем логин в сервис вместе с данными задачи
        TaskDTO createdTask = taskService.createTask(taskDTO, username);

        return ResponseEntity.ok(createdTask);
    }


    // 1. Эндпоинт только для сотрудников (видят только свои задачи)
    @GetMapping("/my")
    public ResponseEntity<List<TaskDTO>> getMyTasks(
            @RequestParam(value = "status", required = false) TaskStatus status,
            Authentication authentication) {

        String username = authentication.getName();
        if (status != null) {
            return ResponseEntity.ok(taskService.getAllTasksByStatus(username, status));
        }
        return ResponseEntity.ok(taskService.getAllTasks(username));
    }

    // 2. Эндпоинт только для админа(видит всё)
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @GetMapping("/all")
    public ResponseEntity<List<TaskDTO>> getAllTasksGlobally(
            @RequestParam(value = "status", required = false) TaskStatus status) {

        if (status != null) {
            return ResponseEntity.ok(taskService.getAllTaskGloballyByStatus(status));
        }
        return ResponseEntity.ok(taskService.getAllTaskGlobally());
    }

    // 3. Эндпоинт для получения списка пользователей
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable("id") Long id, Authentication authentication)
    {
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER") || a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(403).build();
        }
        String username = authentication.getName();
        taskService.deleteById(id,username);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> getTaskByID(@PathVariable("id") Long id, Authentication authentication) {
        boolean isManager = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
        TaskDTO taskDTO = isManager
                ? taskService.getTaskByIdForManager(id)
                : taskService.getTaskByID(id, authentication.getName());
        return ResponseEntity.ok(taskDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDTO> updateTask(@PathVariable("id") Long id,
                                                 @Valid @RequestBody TaskDTO taskDTO,
                                                 Authentication authentication){
        boolean isManager = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
        TaskDTO updated = isManager
                ? taskService.updateTaskForManager(id, taskDTO)
                : taskService.updatedData(id, taskDTO, authentication.getName());
        return ResponseEntity.ok(updated);
    }

}
