package com.getquer.tasktracker;

import com.getquer.tasktracker.request.EmployeeSearchRequest;
import com.getquer.tasktracker.request.StatusUpdateRequest;
import com.getquer.tasktracker.request.TaskCreateRequest;
import org.springframework.scheduling.config.Task;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;


    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping // Метод для создания задачи
    public TaskDTO createTask(@Valid @RequestBody TaskCreateRequest request) {
        return taskService.createTask(request.content(), request.name());
    }
    @GetMapping // Метод для получения списка задач
    public List<TaskDTO> getAllTasks(@RequestParam(value = "status",required = false) TaskStatus status) {
        if (status != null){
            return taskService.getAllTasksByStatus(status);
        }
        return taskService.getAllTasks();
    }

    @PostMapping("/by-employee")
    public List<TaskDTO> getAllTasksByEmployee(@Valid @RequestBody EmployeeSearchRequest employeeSearchRequest)
    {
        return taskService.getAllTasksByEmployee(employeeSearchRequest.fullNameEmployee());
    }

    @PutMapping("/{id}/status")
    public TaskDTO taskDone(@PathVariable("id")Long id,@RequestBody StatusUpdateRequest request){
        return taskService.updatedStatus(id,request.status());
    }

    @DeleteMapping("/{id}")
    public String deleteById(@PathVariable("id") Long id)
    {
        return taskService.deleteById(id);
    }

    @GetMapping("/{id}")
    public TaskDTO getTaskByID(@PathVariable("id") Long id){
        return taskService.getTaskByID(id);
    }

}
