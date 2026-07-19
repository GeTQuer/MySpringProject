package com.getquer.tasktracker.controllers;

import com.getquer.tasktracker.Entities.UserEntity;
import com.getquer.tasktracker.responseDTO.TaskDTO;
import com.getquer.tasktracker.responseDTO.UserDTO;
import com.getquer.tasktracker.Repositories.UserRepository;
import com.getquer.tasktracker.TaskStatus;
import com.getquer.tasktracker.service.TaskService;
import com.getquer.tasktracker.service.UserService;
import org.springframework.data.domain.Page;
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
    public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody TaskDTO taskDTO,
                                              Principal principal) {
        String currentUsername = principal.getName();
        TaskDTO createdTask = taskService.createTask(taskDTO, currentUsername);

        return ResponseEntity.ok(createdTask);
    }


    @GetMapping("/my")
    public ResponseEntity<Page<TaskDTO>> getMyTasks(
            @RequestParam(value = "status", required = false) TaskStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        String username = authentication.getName();
        if (status != null) {
            return ResponseEntity.ok(taskService.getAllTasksByStatus(username, status,page,size));
        }
        return ResponseEntity.ok(taskService.getAllTasks(username,page,size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<Page<TaskDTO>> getAllTasksGlobally(
            @RequestParam(value = "status", required = false) TaskStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (status != null) {
            Page<TaskDTO> taskPage = taskService.getAllTaskGloballyByStatus(status,page,size);
            return ResponseEntity.ok(taskPage);
        }
        Page<TaskDTO> taskPage = taskService.getAllTaskGlobally(page,size);
        return ResponseEntity.ok(taskPage);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/department")
    public ResponseEntity<Page<TaskDTO>> getDepartmentTasks(
            @RequestParam(value = "status",required = false) TaskStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ){
        UserEntity manager = userRepository.findByUsername(authentication.getName()).orElseThrow();
        Long departmentId = manager.getDepartment().getId();
        if (status != null){
            return ResponseEntity.ok(taskService.getAllDepartmentTasksByStatus(departmentId,status,page,size));
        }
        return ResponseEntity.ok(taskService.getAllDepartmentTasks(departmentId,page,size));
    }



    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable("id") Long id, Authentication authentication) {
        String username = authentication.getName();
        taskService.deleteByIdAndUsername(id, username);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> deleteByIdForAdmin(@PathVariable("id") Long id) {
        taskService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/manager/{id}")
    public ResponseEntity<Void> deleteByIdForManager(@PathVariable("id") Long id, Authentication authentication) {
        taskService.deleteByIdForManager(id, authentication.getName());
        return ResponseEntity.ok().build();
    }


    // USER - свои задачи
    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> getMyTask(@PathVariable("id") Long id, Authentication authentication) {
        return ResponseEntity.ok(taskService.getTaskByID(id, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDTO> updateMyTask(@PathVariable("id") Long id,
                                                 @Valid @RequestBody TaskDTO taskDTO,
                                                 Authentication authentication) {
        return ResponseEntity.ok(taskService.updatedData(id, taskDTO, authentication.getName()));
    }

    // MANAGER - задачи отдела
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/department/{id}")
    public ResponseEntity<TaskDTO> getDepartmentTask(@PathVariable("id") Long id, Authentication authentication) {
        return ResponseEntity.ok(taskService.getTaskByIdForManagerWithDepartmentCheck(id, authentication.getName()));
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/department/{id}")
    public ResponseEntity<TaskDTO> updateDepartmentTask(@PathVariable("id") Long id,
                                                        @Valid @RequestBody TaskDTO taskDTO,
                                                        Authentication authentication) {
        return ResponseEntity.ok(taskService.updateTaskForManagerWithDepartmentCheck(id, taskDTO, authentication.getName()));
    }

    // ADMIN - любые задачи
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/{id}")
    public ResponseEntity<TaskDTO> getAdminTask(@PathVariable("id") Long id) {
        return ResponseEntity.ok(taskService.getTaskByIdForManager(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/{id}")
    public ResponseEntity<TaskDTO> updateAdminTask(@PathVariable("id") Long id,
                                                   @Valid @RequestBody TaskDTO taskDTO) {
        return ResponseEntity.ok(taskService.updateTaskForManager(id, taskDTO));
    }

}