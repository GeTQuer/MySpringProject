package com.getquer.tasktracker;


import com.getquer.tasktracker.DTOs.TaskDTO;
import com.getquer.tasktracker.Entities.TaskEntity;
import com.getquer.tasktracker.Entities.UserEntity;
import com.getquer.tasktracker.Repositories.TaskRepository;
import com.getquer.tasktracker.Repositories.UserRepository;
import com.getquer.tasktracker.service.TaskService;
import com.getquer.tasktracker.util.TaskTestDataMother;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {
    @Mock
            private TaskRepository taskRepository;
    @InjectMocks
            private TaskService taskService;
    @Mock
            private UserRepository userRepository;

    private final Pageable pageable = PageRequest.of(0, 10,
            Sort.by("id").descending()
    );


    @Nested
    @DisplayName("Тесты создания задач")
    class createTask{
        @Test
        void createTask_ShouldSaveTask_forSimpleUser(){
            String currentUsername = "test_user";
            TaskDTO inputDTO = new TaskDTO(1L, "Починить баг", "Иванов И.И", "OPEN", "null");

            UserEntity mockCreator = TaskTestDataMother.createTestUser(1L, currentUsername, "USER");
            TaskEntity mockSaveTask = TaskTestDataMother.createTestTask(1L, inputDTO.content(), TaskStatus.OPEN, mockCreator);
            mockSaveTask.setFullNameEmployee(inputDTO.fullNameEmployee());

            Mockito.when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.of(mockCreator));
            Mockito.when(taskRepository.save(Mockito.any(TaskEntity.class))).thenReturn(mockSaveTask);

            TaskDTO result = taskService.createTask(inputDTO, currentUsername);

            assertNotNull(result);
            assertEquals(inputDTO.content(), result.content());
            Mockito.verify(taskRepository, Mockito.times(1)).save(Mockito.any(TaskEntity.class));

        }

        @Test
        void createTask_ShouldSaveTask_forManager(){
            TaskDTO inputDTO = new TaskDTO(1L, "Починить баг", "Иванов Иван Иваныч", "OPEN", "Тестер");

            UserEntity mockAssignedUser = TaskTestDataMother.createTestUser(2L, "Тестер", "USER");
            UserEntity mockCreator = TaskTestDataMother.createTestUser(1L, "Иванов Иван Иваныч", "MANAGER");

            TaskEntity mockSaveTask = TaskTestDataMother.createTestTask(1L, "Починить баг", TaskStatus.OPEN, mockAssignedUser);
            mockSaveTask.setFullNameEmployee(mockAssignedUser.getUsername());

            Mockito.when(userRepository.findByUsername("Тестер")).thenReturn(Optional.of(mockAssignedUser));
            Mockito.when(userRepository.findByUsername("Иванов Иван Иваныч")).thenReturn(Optional.of(mockCreator));
            Mockito.when(taskRepository.save(Mockito.any(TaskEntity.class))).thenReturn(mockSaveTask);

            TaskDTO result = taskService.createTask(inputDTO, mockCreator.getUsername());

            assertNotNull(result);
            assertEquals(inputDTO.assignedUsername(), result.assignedUsername());
            Mockito.verify(taskRepository, Mockito.times(1)).save(Mockito.any(TaskEntity.class));
        }

        @Test
        void createTask_ShouldThrowException_WhenCreatorNotFound(){
            String currentUsername = "nonexistent_user";
            TaskDTO inputDTO = new TaskDTO(1L, "Починить баг", "Иванов И.И", "OPEN", "null");

            Mockito.when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> taskService.createTask(inputDTO, currentUsername));

            Mockito.verify(taskRepository, Mockito.never()).save(Mockito.any(TaskEntity.class));
        }

        @Test
        void createTask_ShouldThrowException_WhenAssignedUserNotFound_forManager(){
            TaskDTO inputDTO = new TaskDTO(1L, "Починить баг", "Иванов Иван Иваныч", "OPEN", "nonexistent_user");

            UserEntity mockCreator = TaskTestDataMother.createTestUser(1L, "Иванов Иван Иваныч", "MANAGER");

            Mockito.when(userRepository.findByUsername("Иванов Иван Иваныч")).thenReturn(Optional.of(mockCreator));
            Mockito.when(userRepository.findByUsername("nonexistent_user")).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> taskService.createTask(inputDTO, mockCreator.getUsername()));

            Mockito.verify(taskRepository, Mockito.never()).save(Mockito.any(TaskEntity.class));
        }
    }


    @Nested
    @DisplayName("Тесты просмотра всех задач для админа")
    class getAllTaskGlobally{
        @Test
        void getAllTaskGlobally_ShouldReturnPageOfTasks_fordmin(){
            List<Long> mockIds = List.of(2L, 1L);
            Page<Long> mockIdsPages = new PageImpl<>(mockIds, pageable, mockIds.size());

            Mockito.when(taskRepository.findAllIds(pageable)).thenReturn(mockIdsPages);

            Page<TaskDTO> result = taskService.getAllTaskGlobally(0, 10);

            assertNotNull(result);
            assertEquals(2, result.getTotalElements());
            Mockito.verify(taskRepository, Mockito.times(1)).findAllIds(pageable);

        }

        @Test
        void getAllTaskGlobally_ShouldReturnEmptyPage_WhenNoTasks(){
            Page<Long> emptyPage = Page.empty(pageable);

            Mockito.when(taskRepository.findAllIds(pageable)).thenReturn(emptyPage);

            Page<TaskDTO> result = taskService.getAllTaskGlobally(0, 10);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            assertEquals(0, result.getTotalElements());
            Mockito.verify(taskRepository, Mockito.times(1)).findAllIds(pageable);
            Mockito.verify(taskRepository, Mockito.never()).findAllByIdsWithUser(Mockito.any());
        }

        @Test
        void getAllTaskGloballyByStatus_ShouldReturnPageOfTasks_forAdmin(){
            UserEntity user1 = TaskTestDataMother.createTestUser(1L, "PASHA", "USER");
            TaskEntity task1 = TaskTestDataMother.createTestTask(1L, "Задача 1", TaskStatus.OPEN, user1);

            List<Long> mockIds = List.of(1L);
            Page<Long> mockIdsPages = new PageImpl<>(mockIds, pageable, mockIds.size());

            Mockito.when(taskRepository.findAllByStatus(TaskStatus.OPEN, pageable)).thenReturn(mockIdsPages);
            Mockito.when(taskRepository.findAllByIdsWithUser(mockIds)).thenReturn(List.of(task1));

            Page<TaskDTO> result = taskService.getAllTaskGloballyByStatus(TaskStatus.OPEN, 0, 10);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals("Задача 1", result.getContent().get(0).content());
            Mockito.verify(taskRepository, Mockito.times(1))
                    .findAllByStatus(TaskStatus.OPEN, pageable);

            // Проверяем, что второй метод (JOIN FETCH) вызвался 1 раз с полученным списком ID
            Mockito.verify(taskRepository, Mockito.times(1))
                    .findAllByIdsWithUser(mockIds);
        }

        @Test
        void getAllTaskGloballyByStatus_ShouldReturnEmptyPage_WhenNoTasksWithStatus(){
            Page<Long> emptyPage = Page.empty(pageable);

            Mockito.when(taskRepository.findAllByStatus(TaskStatus.OPEN, pageable)).thenReturn(emptyPage);

            Page<TaskDTO> result = taskService.getAllTaskGloballyByStatus(TaskStatus.OPEN, 0, 10);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            assertEquals(0, result.getTotalElements());
            Mockito.verify(taskRepository, Mockito.times(1))
                    .findAllByStatus(TaskStatus.OPEN, pageable);
            Mockito.verify(taskRepository, Mockito.never()).findAllByIdsWithUser(Mockito.any());
        }
    }

    @Nested
    @DisplayName("Тесты получения задач пользователями")
    class UserTasksTests{
        @Test
        void getAllTasks_ShouldReturnAllTasksbyUsername(){
            UserEntity user1 = TaskTestDataMother.createTestUser(1L, "PASHA", "USER");
            TaskEntity task1 = TaskTestDataMother.createTestTask(1L, "Задача 1", TaskStatus.OPEN, user1);
            TaskEntity task2 = TaskTestDataMother.createTestTask(2L, "Задача 2", TaskStatus.OPEN, user1);

            List<Long> mockIds = List.of(1L,2L);
            Page<Long> mockIdsPages = new PageImpl<>(mockIds, pageable, mockIds.size());

            Mockito.when(taskRepository.findAllByUserUsername(user1.getUsername(), pageable))
                    .thenReturn(mockIdsPages);

            //convertToTaskPage
            Mockito.when(taskRepository.findAllByIdsWithUser(mockIds))
                    .thenReturn(List.of(task1,task2));

            Page<TaskDTO> result = taskService.getAllTasks(user1.getUsername(),
                    pageable.getPageNumber(),
                    pageable.getPageSize()
            );

            assertNotNull(result);
            assertEquals(2, result.getTotalElements());

            Mockito.verify(taskRepository, Mockito.times(1))
                    .findAllByUserUsername(user1.getUsername(), pageable);

            // Проверяем, что второй метод (JOIN FETCH) вызвался 1 раз с полученным списком ID
            Mockito.verify(taskRepository, Mockito.times(1))
                    .findAllByIdsWithUser(mockIds);
        }

        @Test
        void getAllTasks_ShouldReturnEmptyPage_WhenUserHasNoTasks(){
            Page<Long> emptyPage = Page.empty(pageable);

            Mockito.when(taskRepository.findAllByUserUsername("nonexistent_user", pageable))
                    .thenReturn(emptyPage);

            Page<TaskDTO> result = taskService.getAllTasks("nonexistent_user", 0, 10);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            assertEquals(0, result.getTotalElements());
            Mockito.verify(taskRepository, Mockito.times(1))
                    .findAllByUserUsername("nonexistent_user", pageable);
            Mockito.verify(taskRepository, Mockito.never()).findAllByIdsWithUser(Mockito.any());
        }

        @Test
        void getAllTasksByStatus_ShouldReturnAllTasksByStatusAndUsername(){
            UserEntity user1 = TaskTestDataMother.createTestUser(1L, "John", "USER");
            TaskEntity task1 = TaskTestDataMother.createTestTask(1L, "Задача 1", TaskStatus.OPEN, user1);

            List<Long> mockIds = List.of(1L);
            Page<Long> mockIdsPages = new PageImpl<>(mockIds, pageable, mockIds.size());

            Mockito.when(taskRepository.findAllByUserUsernameAndStatus(user1.getUsername(), TaskStatus.OPEN, pageable)).
                    thenReturn(mockIdsPages);
            Mockito.when(taskRepository.findAllByIdsWithUser(mockIds)).
                    thenReturn(List.of(task1));

            Page<TaskDTO> result = taskService.getAllTasksByStatus(user1.getUsername(), TaskStatus.OPEN, 0, 10);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());

            Mockito.verify(taskRepository, Mockito.times(1))
                    .findAllByUserUsernameAndStatus(user1.getUsername(),TaskStatus.OPEN, pageable);

            // Проверяем, что второй метод (JOIN FETCH) вызвался 1 раз с полученным списком ID
            Mockito.verify(taskRepository, Mockito.times(1))
                    .findAllByIdsWithUser(mockIds);
        }

        @Test
        void getAllTasksByStatus_ShouldReturnEmptyPage_WhenUserHasNoTasksWithStatus(){
            Page<Long> emptyPage = Page.empty(pageable);

            Mockito.when(taskRepository.findAllByUserUsernameAndStatus("John", TaskStatus.OPEN, pageable))
                    .thenReturn(emptyPage);

            Page<TaskDTO> result = taskService.getAllTasksByStatus("John", TaskStatus.OPEN, 0, 10);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            assertEquals(0, result.getTotalElements());
            Mockito.verify(taskRepository, Mockito.times(1))
                    .findAllByUserUsernameAndStatus("John", TaskStatus.OPEN, pageable);
            Mockito.verify(taskRepository, Mockito.never()).findAllByIdsWithUser(Mockito.any());
        }
    }
    @Nested
    @DisplayName("Примитивные методы")
    class simpleMethods{
        // -------------------------------------------------------------------------------------------------------------------------------
        @Test
        void deleteByIdAndUsername_ShouldVerify(){
            UserEntity user = TaskTestDataMother.createTestUser(1L, "John", "USER");

            TaskEntity taskToDelete = TaskTestDataMother.createTestTask(
                    1L,
                    "task 1",
                    TaskStatus.OPEN,
                    user
            );

            Mockito.when(taskRepository.findByIdAndUserUsername(
                    taskToDelete.getId(),
                    user.getUsername()
            )).thenReturn(Optional.of(taskToDelete));

            taskService.deleteByIdAndUsername(
                    taskToDelete.getId(),
                    user.getUsername()
            );
            Mockito.verify(taskRepository,Mockito.times(1)).delete(taskToDelete);
        }
        @Test
        void deleteByIdAndUsername_ShouldThrowException_WhenTaskNotFound(){
            Mockito.when(taskRepository.findByIdAndUserUsername(999L, "John"))
                    .thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class,
                    ()->{taskService.deleteByIdAndUsername(999L,"John");
            });
            Mockito.verify(taskRepository, Mockito.never()).delete(Mockito.any(TaskEntity.class));
        }

        // -------------------------------------------------------------------------------------------------------------------------------

        @Test
        void updateDate_ShouldReturnUpdatedTask(){
            UserEntity user = TaskTestDataMother.createTestUser(1L,"John","USER");
            TaskEntity taskToUpdate = TaskTestDataMother.createTestTask(
                    1L,
                    "Задача 1",
                    TaskStatus.OPEN,
                    user
                    );
            Mockito.when(taskRepository.findByIdAndUserUsername(
                    taskToUpdate.getId(),
                    user.getUsername()
            )).thenReturn(Optional.of(taskToUpdate));
            TaskDTO requestDTO = new TaskDTO(
                    1L,
                    "Задача 2",      // content
                    "John",          // fullNameEmployee
                    "DONE",          // status
                    null             // assignedUsername
            );


            Mockito.when(taskRepository.save(Mockito.any(TaskEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));


            TaskDTO result = taskService.updatedData(1L,requestDTO,"John");
            Assertions.assertNotNull(result);
            Assertions.assertEquals("Задача 2", result.content());
            Assertions.assertEquals("DONE", result.status());
            Assertions.assertEquals("John", result.fullNameEmployee());

            Mockito.verify(taskRepository,Mockito.times(1))
                    .findByIdAndUserUsername(1L,"John");
        }

        @Test
        void updateData_ShouldReturnException_WhenTaskNotFound(){
            TaskDTO requestDTO = new TaskDTO(
                    999L,
                    "Задача 2",
                    "John",
                    "DONE",
                    null
            );

            Mockito.when(taskRepository.findByIdAndUserUsername(999L, "John"))
                    .thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> taskService.updatedData(999L, requestDTO, "John"));

            Mockito.verify(taskRepository, Mockito.never())
                    .save(Mockito.any(TaskEntity.class));
        }

        // -------------------------------------------------------------------------------------------------------------------------------

        @Test
        void getTaskByID_ShouldReturnTask(){
            UserEntity user = TaskTestDataMother.createTestUser(1L, "John", "USER");
            TaskEntity task = TaskTestDataMother.createTestTask(
                    1L,
                    "Задача 1",
                    TaskStatus.OPEN,
                    user
            );

            Mockito.when(taskRepository.findByIdAndUserUsername(1L, "John"))
                    .thenReturn(Optional.of(task));

            TaskDTO result = taskService.getTaskByID(1L, "John");

            assertNotNull(result);
            assertEquals("Задача 1", result.content());
            assertEquals("OPEN", result.status());
            assertEquals("John", result.assignedUsername());

            Mockito.verify(taskRepository, Mockito.times(1))
                    .findByIdAndUserUsername(1L, "John");
        }

        @Test
        void getTaskByID_ShouldThrowException_WhenTaskNotFound(){
            Mockito.when(taskRepository.findByIdAndUserUsername(999L, "John"))
                    .thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> taskService.getTaskByID(999L, "John"));

            Mockito.verify(taskRepository, Mockito.times(1))
                    .findByIdAndUserUsername(999L, "John");
        }

        // -------------------------------------------------------------------------------------------------------------------------------

        @Test
        void getTaskByIdForManager_ShouldReturnTask(){
            UserEntity user = TaskTestDataMother.createTestUser(1L, "John", "USER");
            TaskEntity task = TaskTestDataMother.createTestTask(
                    1L,
                    "Задача 1",
                    TaskStatus.OPEN,
                    user
            );

            Mockito.when(taskRepository.findById(1L))
                    .thenReturn(Optional.of(task));

            TaskDTO result = taskService.getTaskByIdForManager(1L);

            assertNotNull(result);
            assertEquals("Задача 1", result.content());
            assertEquals("OPEN", result.status());
            assertEquals("John", result.assignedUsername());

            Mockito.verify(taskRepository, Mockito.times(1))
                    .findById(1L);
        }

        @Test
        void getTaskByIdForManager_ShouldThrowException_WhenTaskNotFound(){
            Mockito.when(taskRepository.findById(999L))
                    .thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> taskService.getTaskByIdForManager(999L));

            Mockito.verify(taskRepository, Mockito.times(1))
                    .findById(999L);
        }

        // -------------------------------------------------------------------------------------------------------------------------------

        @Test
        void updateTaskForManager_ShouldReturnUpdatedTask(){
            UserEntity user = TaskTestDataMother.createTestUser(1L, "John", "USER");
            TaskEntity taskToUpdate = TaskTestDataMother.createTestTask(
                    1L,
                    "Задача 1",
                    TaskStatus.OPEN,
                    user
            );

            Mockito.when(taskRepository.findById(1L))
                    .thenReturn(Optional.of(taskToUpdate));

            TaskDTO requestDTO = new TaskDTO(
                    1L,
                    "Задача 2",
                    "John",
                    "DONE",
                    null
            );

            Mockito.when(taskRepository.save(Mockito.any(TaskEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            TaskDTO result = taskService.updateTaskForManager(1L, requestDTO);

            assertNotNull(result);
            assertEquals("Задача 2", result.content());
            assertEquals("DONE", result.status());
            assertEquals("John", result.fullNameEmployee());

            Mockito.verify(taskRepository, Mockito.times(1))
                    .findById(1L);
            Mockito.verify(taskRepository, Mockito.times(1))
                    .save(Mockito.any(TaskEntity.class));
        }

        @Test
        void updateTaskForManager_ShouldThrowException_WhenTaskNotFound(){
            TaskDTO requestDTO = new TaskDTO(
                    999L,
                    "Задача 2",
                    "John",
                    "DONE",
                    null
            );

            Mockito.when(taskRepository.findById(999L))
                    .thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> taskService.updateTaskForManager(999L, requestDTO));

            Mockito.verify(taskRepository, Mockito.never())
                    .save(Mockito.any(TaskEntity.class));
        }
    }


}
