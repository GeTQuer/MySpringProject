package com.getquer.tasktracker;


import com.getquer.tasktracker.DTOs.TaskDTO;
import com.getquer.tasktracker.Entities.TaskEntity;
import com.getquer.tasktracker.Entities.UserEntity;
import com.getquer.tasktracker.Repositories.TaskRepository;
import com.getquer.tasktracker.Repositories.UserRepository;
import com.getquer.tasktracker.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
            private TaskRepository taskRepository;
    @InjectMocks
            private TaskService taskService;
    @Mock
            private UserRepository userRepository;

    @Test
    void createTask_ShouldSaveTask_forSimpleUser(){
        String currentUsername = "test_user";
        TaskDTO inputDTO = new TaskDTO(
                (long) 1,"Починить баг",
                "Иванов И.И",
                "OPEN",
                "null"
        );
        UserEntity mockCreator = new UserEntity();
        mockCreator.setUsername(currentUsername);
        mockCreator.setRole("USER");
        mockCreator.setId((long) 1);



        TaskEntity mockSaveTask = new TaskEntity();
        mockSaveTask.setId((long) 1);
        mockSaveTask.setContent(inputDTO.content());
        mockSaveTask.setFullNameEmployee(inputDTO.fullNameEmployee());
        mockSaveTask.setStatus(TaskStatus.OPEN);
        mockSaveTask.setUser(mockCreator);

        Mockito.when(userRepository.findByUsername(currentUsername)).
                thenReturn(Optional.of(mockCreator));

        Mockito.when(taskRepository.save(Mockito.any(TaskEntity.class))).
                thenReturn(mockSaveTask);

        TaskDTO result = taskService.createTask(inputDTO, currentUsername);

        assertNotNull(result);
        assertEquals(inputDTO.content(), result.content());

        Mockito.verify(taskRepository, Mockito.times(1))
                .save(Mockito.any(TaskEntity.class));

    }

    @Test
    void createTask_ShouldSaveTask_forManager(){
        TaskDTO inputDTO = new TaskDTO(
                (long) 1,
                "Починить баг",
                "Иванов Иван Иваныч",
                "OPEN",
                "Тестер"
        );

        UserEntity mockAssignedUser = new UserEntity();
        mockAssignedUser.setId((long) 2);
        mockAssignedUser.setRole("USER");
        mockAssignedUser.setUsername("Тестер");
        Mockito.when(userRepository.findByUsername("Тестер"))
                .thenReturn(Optional.of(mockAssignedUser));

        UserEntity mockCreater = new UserEntity();
        mockCreater.setId((long) 1);
        mockCreater.setUsername("Иванов Иван Иваныч");
        mockCreater.setRole("MANAGER");
        Mockito.when(userRepository.findByUsername("Иванов Иван Иваныч"))
                .thenReturn(Optional.of(mockCreater));

        TaskEntity mockSaveTask = new TaskEntity();
        mockSaveTask.setUser(mockAssignedUser);
        mockSaveTask.setId((long) 1);
        mockSaveTask.setStatus(TaskStatus.OPEN);
        mockSaveTask.setFullNameEmployee(mockAssignedUser.getUsername());
        mockSaveTask.setContent("Починить баг");
        Mockito.when(taskRepository.save(Mockito.any(TaskEntity.class)))
                .thenReturn(mockSaveTask);

        TaskDTO result = taskService.createTask(inputDTO,mockCreater.getUsername());

        assertNotNull(result);
        assertEquals(inputDTO.assignedUsername(), result.assignedUsername());

    }


}
