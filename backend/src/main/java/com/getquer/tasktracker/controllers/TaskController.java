package com.getquer.tasktracker.controllers;

import com.getquer.tasktracker.Entities.TaskEntity;
import com.getquer.tasktracker.Repositories.UserRepository;
import com.getquer.tasktracker.TaskDTO;
import com.getquer.tasktracker.service.TaskService;
import com.getquer.tasktracker.TaskStatus;
import com.getquer.tasktracker.request.StatusUpdateRequest;
import com.getquer.tasktracker.request.TaskCreateRequest;
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

    public TaskController(TaskService taskService, UserRepository userRepository) {
        this.taskService = taskService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody TaskDTO taskDTO, Principal principal) {
        // Достаем логин из токена
        String username = principal.getName();

        // Передаем логин в сервис вместе с данными задачи
        TaskDTO createdTask = taskService.createTask(taskDTO, username);

        return ResponseEntity.ok(createdTask);
    }
    @GetMapping // Метод для получения списка задач
    public ResponseEntity<List<TaskDTO>> getAllTasks(@RequestParam(value = "status",required = false) TaskStatus status,
                                                     Authentication authentication) {
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            if (status != null) {
                return ResponseEntity.ok(taskService.getAllTaskGloballyByStatus(status));
            }
            return ResponseEntity.ok(taskService.getAllTaskGlobally());
        }
        if (status != null) {
            return ResponseEntity.ok(taskService.getAllTasksByStatus(username, status));
        }
        return ResponseEntity.ok(taskService.getAllTasks(username));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable("id") Long id, Authentication authentication)
    {
        String username = authentication.getName();
        taskService.deleteById(id,username);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> getTaskByID(@PathVariable("id") Long id, Authentication authentication){
        String username = authentication.getName();
        return ResponseEntity.ok(taskService.getTaskByID(id,username));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDTO> updateTask(@PathVariable("id") Long id,
                                                 @RequestBody TaskDTO updateData,
                                                 Authentication authentication){
        String username = authentication.getName();
        TaskDTO savedTask = taskService.updatedData(id,updateData,username);
        return ResponseEntity.ok(savedTask);
    }

}
