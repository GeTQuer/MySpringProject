package com.getquer.tasktracker;


import com.getquer.tasktracker.responseDTO.TaskDTO;
import com.getquer.tasktracker.Entities.DepartmentEntity;
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
            mockCreator.setDepartment(null);
            TaskEntity mockSaveTask = TaskTestDataMother.createTestTask(1L, inputDTO.content(), TaskStatus.OPEN, mockCreator);
            mockSaveTask.setFullNameEmployee(inputDTO.fullNameEmployee());
            mockSaveTask.setDepartment(null);

            Mockito.when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.of(mockCreator));
            Mockito.when(taskRepository.save(Mockito.any(TaskEntity.class))).thenReturn(mockSaveTask);

            TaskDTO result = taskService.createTask(inputDTO, currentUsername);

            assertNotNull(result);
            assertEquals(inputDTO.content(), result.content());
            Mockito.verify(taskRepository, Mockito.times(1)).save(Mockito.any(TaskEntity.class));

        }

        @Test
        void createTask_ShouldSaveTask_forManager(){
            DepartmentEntity department = TaskTestDataMother.createTestDepartment(1L, "IT");
            TaskDTO inputDTO = new TaskDTO(1L, "Починить баг", "Иванов Иван Иваныч", "OPEN", "Тестер");

            UserEntity mockAssignedUser = TaskTestDataMother.createTestUserWithDepartment(2L, "Тестер", "USER", department);
            UserEntity mockCreator = TaskTestDataMother.createTestUserWithDepartment(1L, "Иванов Иван Иваныч", "MANAGER", department);

            TaskEntity mockSaveTask = TaskTestDataMother.createTestTask(1L, "Починить баг", TaskStatus.OPEN, mockAssignedUser);
            mockSaveTask.setFullNameEmployee(mockAssignedUser.getUsername());
            mockSaveTask.setDepartment(department);

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

        @Test
        void createTask_ShouldThrowException_WhenManagerAssignsTaskToUserFromDifferentDepartment(){
            DepartmentEntity itDepartment = TaskTestDataMother.createTestDepartment(1L, "IT");
            DepartmentEntity qaDepartment = TaskTestDataMother.createTestDepartment(2L, "QA");
            TaskDTO inputDTO = new TaskDTO(1L, "Починить баг", "Иванов Иван Иваныч", "OPEN", "Тестер");

            UserEntity mockAssignedUser = TaskTestDataMother.createTestUserWithDepartment(2L, "Тестер", "USER", qaDepartment);
            UserEntity mockCreator = TaskTestDataMother.createTestUserWithDepartment(1L, "Иванов Иван Иваныч", "MANAGER", itDepartment);

            Mockito.when(userRepository.findByUsername("Тестер")).thenReturn(Optional.of(mockAssignedUser));
            Mockito.when(userRepository.findByUsername("Иванов Иван Иваныч")).thenReturn(Optional.of(mockCreator));

            assertThrows(RuntimeException.class,
                    () -> taskService.createTask(inputDTO, mockCreator.getUsername()));

            Mockito.verify(taskRepository, Mockito.never()).save(Mockito.any(TaskEntity.class));
        }

        @Test
        void createTask_ShouldSaveTask_forAdmin(){
            DepartmentEntity itDepartment = TaskTestDataMother.createTestDepartment(1L, "IT");
            DepartmentEntity qaDepartment = TaskTestDataMother.createTestDepartment(2L, "QA");
            TaskDTO inputDTO = new TaskDTO(1L, "Починить баг", "Admin", "OPEN", "Тестер");

            UserEntity mockAssignedUser = TaskTestDataMother.createTestUserWithDepartment(2L, "Тестер", "USER", qaDepartment);
            UserEntity mockCreator = TaskTestDataMother.createTestUserWithDepartment(1L, "Admin", "ADMIN", itDepartment);

            TaskEntity mockSaveTask = TaskTestDataMother.createTestTask(1L, "Починить баг", TaskStatus.OPEN, mockAssignedUser);
            mockSaveTask.setFullNameEmployee(mockAssignedUser.getUsername());
            mockSaveTask.setDepartment(qaDepartment);

            Mockito.when(userRepository.findByUsername("Тестер")).thenReturn(Optional.of(mockAssignedUser));
            Mockito.when(userRepository.findByUsername("Admin")).thenReturn(Optional.of(mockCreator));
            Mockito.when(taskRepository.save(Mockito.any(TaskEntity.class))).thenReturn(mockSaveTask);

            TaskDTO result = taskService.createTask(inputDTO, mockCreator.getUsername());

            assertNotNull(result);
            assertEquals(inputDTO.assignedUsername(), result.assignedUsername());
            Mockito.verify(taskRepository, Mockito.times(1)).save(Mockito.any(TaskEntity.class));
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
            DepartmentEntity department = TaskTestDataMother.createTestDepartment(1L, "IT");
            UserEntity user1 = TaskTestDataMother.createTestUserWithDepartment(1L, "PASHA", "USER", department);
            TaskEntity task1 = TaskTestDataMother.createTestTask(1L, "Задача 1", TaskStatus.OPEN, user1);
            TaskEntity task2 = TaskTestDataMother.createTestTask(2L, "Задача 2", TaskStatus.OPEN, user1);

            List<Long> mockIds = List.of(1L,2L);
            Page<Long> mockIdsPages = new PageImpl<>(mockIds, pageable, mockIds.size());

            Mockito.when(userRepository.findByUsername(user1.getUsername())).thenReturn(Optional.of(user1));
            Mockito.when(taskRepository.findAllByUserUsername(user1.getUsername(), department.getId(), pageable))
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
                    .findAllByUserUsername(user1.getUsername(), department.getId(), pageable);

            // Проверяем, что второй метод (JOIN FETCH) вызвался 1 раз с полученным списком ID
            Mockito.verify(taskRepository, Mockito.times(1))
                    .findAllByIdsWithUser(mockIds);
        }

        @Test
        void getAllTasks_ShouldReturnEmptyPage_WhenUserHasNoTasks(){
            DepartmentEntity department = TaskTestDataMother.createTestDepartment(1L, "IT");
            UserEntity user = TaskTestDataMother.createTestUserWithDepartment(1L, "nonexistent_user", "USER", department);
            Page<Long> emptyPage = Page.empty(pageable);

            Mockito.when(userRepository.findByUsername("nonexistent_user")).thenReturn(Optional.of(user));
            Mockito.when(taskRepository.findAllByUserUsername("nonexistent_user", department.getId(), pageable))
                    .thenReturn(emptyPage);

            Page<TaskDTO> result = taskService.getAllTasks("nonexistent_user", 0, 10);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            assertEquals(0, result.getTotalElements());
            Mockito.verify(taskRepository, Mockito.times(1))
                    .findAllByUserUsername("nonexistent_user", department.getId(), pageable);
            Mockito.verify(taskRepository, Mockito.never()).findAllByIdsWithUser(Mockito.any());
        }

        @Test
        void getAllTasksByStatus_ShouldReturnAllTasksByStatusAndUsername(){
            DepartmentEntity department = TaskTestDataMother.createTestDepartment(1L, "IT");
            UserEntity user1 = TaskTestDataMother.createTestUserWithDepartment(1L, "John", "USER", department);
            TaskEntity task1 = TaskTestDataMother.createTestTask(1L, "Задача 1", TaskStatus.OPEN, user1);

            List<Long> mockIds = List.of(1L);
            Page<Long> mockIdsPages = new PageImpl<>(mockIds, pageable, mockIds.size());

            Mockito.when(userRepository.findByUsername(user1.getUsername())).thenReturn(Optional.of(user1));
            Mockito.when(taskRepository.findAllByUserUsernameAndStatus(user1.getUsername(), TaskStatus.OPEN, department.getId(), pageable)).
                    thenReturn(mockIdsPages);
            Mockito.when(taskRepository.findAllByIdsWithUser(mockIds)).
                    thenReturn(List.of(task1));

            Page<TaskDTO> result = taskService.getAllTasksByStatus(user1.getUsername(), TaskStatus.OPEN, 0, 10);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());

            Mockito.verify(taskRepository, Mockito.times(1))
                    .findAllByUserUsernameAndStatus(user1.getUsername(),TaskStatus.OPEN, department.getId(), pageable);

            // Проверяем, что второй метод (JOIN FETCH) вызвался 1 раз с полученным списком ID
            Mockito.verify(taskRepository, Mockito.times(1))
                    .findAllByIdsWithUser(mockIds);
        }

        @Test
        void getAllTasksByStatus_ShouldReturnEmptyPage_WhenUserHasNoTasksWithStatus(){
            DepartmentEntity department = TaskTestDataMother.createTestDepartment(1L, "IT");
            UserEntity user = TaskTestDataMother.createTestUserWithDepartment(1L, "John", "USER", department);
            Page<Long> emptyPage = Page.empty(pageable);

            Mockito.when(userRepository.findByUsername("John")).thenReturn(Optional.of(user));
            Mockito.when(taskRepository.findAllByUserUsernameAndStatus("John", TaskStatus.OPEN, department.getId(), pageable))
                    .thenReturn(emptyPage);

            Page<TaskDTO> result = taskService.getAllTasksByStatus("John", TaskStatus.OPEN, 0, 10);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            assertEquals(0, result.getTotalElements());
            Mockito.verify(taskRepository, Mockito.times(1))
                    .findAllByUserUsernameAndStatus("John", TaskStatus.OPEN, department.getId(), pageable);
            Mockito.verify(taskRepository, Mockito.never()).findAllByIdsWithUser(Mockito.any());
        }

        @Test
        void getAllTasks_ShouldThrowException_WhenUserNotFound(){
            Mockito.when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> taskService.getAllTasks("nonexistent", 0, 10));
        }

        @Test
        void getAllTasksByStatus_ShouldThrowException_WhenUserNotFound(){
            Mockito.when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> taskService.getAllTasksByStatus("nonexistent", TaskStatus.OPEN, 0, 10));
        }

        @Test
        void getAllTasks_ShouldReturnTasks_WhenUserHasNoDepartment(){
            UserEntity user = TaskTestDataMother.createTestUser(1L, "John", "USER");
            user.setDepartment(null);
            TaskEntity task1 = TaskTestDataMother.createTestTask(1L, "Задача 1", TaskStatus.OPEN, user);

            List<Long> mockIds = List.of(1L);
            Page<Long> mockIdsPages = new PageImpl<>(mockIds, pageable, mockIds.size());

            Mockito.when(userRepository.findByUsername("John")).thenReturn(Optional.of(user));
            Mockito.when(taskRepository.findAllByUserUsername("John", null, pageable))
                    .thenReturn(mockIdsPages);
            Mockito.when(taskRepository.findAllByIdsWithUser(mockIds))
                    .thenReturn(List.of(task1));

            Page<TaskDTO> result = taskService.getAllTasks("John", 0, 10);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            Mockito.verify(taskRepository, Mockito.times(1))
                    .findAllByUserUsername("John", null, pageable);
        }

        @Test
        void getAllTasksByStatus_ShouldReturnTasks_WhenUserHasNoDepartment(){
            UserEntity user = TaskTestDataMother.createTestUser(1L, "John", "USER");
            user.setDepartment(null);
            TaskEntity task1 = TaskTestDataMother.createTestTask(1L, "Задача 1", TaskStatus.OPEN, user);

            List<Long> mockIds = List.of(1L);
            Page<Long> mockIdsPages = new PageImpl<>(mockIds, pageable, mockIds.size());

            Mockito.when(userRepository.findByUsername("John")).thenReturn(Optional.of(user));
            Mockito.when(taskRepository.findAllByUserUsernameAndStatus("John", TaskStatus.OPEN, null, pageable))
                    .thenReturn(mockIdsPages);
            Mockito.when(taskRepository.findAllByIdsWithUser(mockIds))
                    .thenReturn(List.of(task1));

            Page<TaskDTO> result = taskService.getAllTasksByStatus("John", TaskStatus.OPEN, 0, 10);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            Mockito.verify(taskRepository, Mockito.times(1))
                    .findAllByUserUsernameAndStatus("John", TaskStatus.OPEN, null, pageable);
        }
    }

    @Nested
    @DisplayName("Тесты задач по отделам")
    class DepartmentTasksTests{
        @Test
        void getAllDepartmentTasks_ShouldReturnTasks(){
            DepartmentEntity department = TaskTestDataMother.createTestDepartment(1L, "IT");
            UserEntity manager = TaskTestDataMother.createTestUserWithDepartment(1L, "Manager", "MANAGER", department);
            UserEntity user1 = TaskTestDataMother.createTestUserWithDepartment(2L, "PASHA", "USER", department);
            TaskEntity task1 = TaskTestDataMother.createTestTask(1L, "Задача 1", TaskStatus.OPEN, user1);
            TaskEntity task2 = TaskTestDataMother.createTestTask(2L, "Задача 2", TaskStatus.OPEN, user1);

            List<Long> mockIds = List.of(1L,2L);
            Page<Long> mockIdsPages = new PageImpl<>(mockIds, pageable, mockIds.size());

            Mockito.when(userRepository.findByUsername("Manager")).thenReturn(Optional.of(manager));
            Mockito.when(taskRepository.findAllTasksByDepartmentId(department.getId(), pageable))
                    .thenReturn(mockIdsPages);
            Mockito.when(taskRepository.findAllByIdsWithUser(mockIds))
                    .thenReturn(List.of(task1,task2));

            Page<TaskDTO> result = taskService.getAllDepartmentTasks("Manager", 0, 10);

            assertNotNull(result);
            assertEquals(2, result.getTotalElements());
            Mockito.verify(taskRepository, Mockito.times(1))
                    .findAllTasksByDepartmentId(department.getId(), pageable);
            Mockito.verify(taskRepository, Mockito.times(1))
                    .findAllByIdsWithUser(mockIds);
        }

        @Test
        void getAllDepartmentTasks_ShouldReturnEmptyPage_WhenNoTasks(){
            DepartmentEntity department = TaskTestDataMother.createTestDepartment(1L, "IT");
            UserEntity manager = TaskTestDataMother.createTestUserWithDepartment(1L, "Manager", "MANAGER", department);
            Page<Long> emptyPage = Page.empty(pageable);

            Mockito.when(userRepository.findByUsername("Manager")).thenReturn(Optional.of(manager));
            Mockito.when(taskRepository.findAllTasksByDepartmentId(department.getId(), pageable))
                    .thenReturn(emptyPage);

            Page<TaskDTO> result = taskService.getAllDepartmentTasks("Manager", 0, 10);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            assertEquals(0, result.getTotalElements());
            Mockito.verify(taskRepository, Mockito.times(1))
                    .findAllTasksByDepartmentId(department.getId(), pageable);
            Mockito.verify(taskRepository, Mockito.never()).findAllByIdsWithUser(Mockito.any());
        }

        @Test
        void getAllDepartmentTasksByStatus_ShouldReturnTasksByStatus(){
            DepartmentEntity department = TaskTestDataMother.createTestDepartment(1L, "IT");
            UserEntity manager = TaskTestDataMother.createTestUserWithDepartment(1L, "Manager", "MANAGER", department);
            UserEntity user1 = TaskTestDataMother.createTestUserWithDepartment(2L, "John", "USER", department);
            TaskEntity task1 = TaskTestDataMother.createTestTask(1L, "Задача 1", TaskStatus.OPEN, user1);

            List<Long> mockIds = List.of(1L);
            Page<Long> mockIdsPages = new PageImpl<>(mockIds, pageable, mockIds.size());

            Mockito.when(userRepository.findByUsername("Manager")).thenReturn(Optional.of(manager));
            Mockito.when(taskRepository.findAllTasksByDepartmentIdAndStatus(department.getId(), TaskStatus.OPEN, pageable))
                    .thenReturn(mockIdsPages);
            Mockito.when(taskRepository.findAllByIdsWithUser(mockIds))
                    .thenReturn(List.of(task1));

            Page<TaskDTO> result = taskService.getAllDepartmentTasksByStatus("Manager", TaskStatus.OPEN, 0, 10);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            Mockito.verify(taskRepository, Mockito.times(1))
                    .findAllTasksByDepartmentIdAndStatus(department.getId(), TaskStatus.OPEN, pageable);
            Mockito.verify(taskRepository, Mockito.times(1))
                    .findAllByIdsWithUser(mockIds);
        }

        @Test
        void getAllDepartmentTasksByStatus_ShouldReturnEmptyPage_WhenNoTasksWithStatus(){
            DepartmentEntity department = TaskTestDataMother.createTestDepartment(1L, "IT");
            UserEntity manager = TaskTestDataMother.createTestUserWithDepartment(1L, "Manager", "MANAGER", department);
            Page<Long> emptyPage = Page.empty(pageable);

            Mockito.when(userRepository.findByUsername("Manager")).thenReturn(Optional.of(manager));
            Mockito.when(taskRepository.findAllTasksByDepartmentIdAndStatus(department.getId(), TaskStatus.OPEN, pageable))
                    .thenReturn(emptyPage);

            Page<TaskDTO> result = taskService.getAllDepartmentTasksByStatus("Manager", TaskStatus.OPEN, 0, 10);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            assertEquals(0, result.getTotalElements());
            Mockito.verify(taskRepository, Mockito.times(1))
                    .findAllTasksByDepartmentIdAndStatus(department.getId(), TaskStatus.OPEN, pageable);
            Mockito.verify(taskRepository, Mockito.never()).findAllByIdsWithUser(Mockito.any());
        }

        @Test
        void getAllDepartmentTasks_ShouldThrowException_WhenManagerNotFound(){
            Mockito.when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> taskService.getAllDepartmentTasks("nonexistent", 0, 10));
        }

        @Test
        void getAllDepartmentTasksByStatus_ShouldThrowException_WhenManagerNotFound(){
            Mockito.when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> taskService.getAllDepartmentTasksByStatus("nonexistent", TaskStatus.OPEN, 0, 10));
        }

        @Test
        void getAllDepartmentTasks_ShouldThrowException_WhenManagerHasNoDepartment(){
            UserEntity manager = TaskTestDataMother.createTestUser(1L, "Manager", "MANAGER");
            manager.setDepartment(null);

            Mockito.when(userRepository.findByUsername("Manager")).thenReturn(Optional.of(manager));

            assertThrows(RuntimeException.class,
                    () -> taskService.getAllDepartmentTasks("Manager", 0, 10));
        }

        @Test
        void getAllDepartmentTasksByStatus_ShouldThrowException_WhenManagerHasNoDepartment(){
            UserEntity manager = TaskTestDataMother.createTestUser(1L, "Manager", "MANAGER");
            manager.setDepartment(null);

            Mockito.when(userRepository.findByUsername("Manager")).thenReturn(Optional.of(manager));

            assertThrows(RuntimeException.class,
                    () -> taskService.getAllDepartmentTasksByStatus("Manager", TaskStatus.OPEN, 0, 10));
        }
    }

    @Nested
    @DisplayName("Тесты удаления для MANAGER")
    class ManagerDeleteTests{
        @Test
        void deleteByIdForManager_ShouldDeleteTask_WhenTaskInSameDepartment(){
            DepartmentEntity department = TaskTestDataMother.createTestDepartment(1L, "IT");
            UserEntity manager = TaskTestDataMother.createTestUserWithDepartment(1L, "Manager", "MANAGER", department);
            TaskEntity task = TaskTestDataMother.createTestTask(1L, "Задача", TaskStatus.OPEN, manager);
            task.setDepartment(department);

            Mockito.when(userRepository.findByUsername("Manager")).thenReturn(Optional.of(manager));
            Mockito.when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

            taskService.deleteByIdForManager(1L, "Manager");

            Mockito.verify(taskRepository, Mockito.times(1)).delete(task);
        }

        @Test
        void deleteByIdForManager_ShouldThrowException_WhenTaskInDifferentDepartment(){
            DepartmentEntity itDepartment = TaskTestDataMother.createTestDepartment(1L, "IT");
            DepartmentEntity qaDepartment = TaskTestDataMother.createTestDepartment(2L, "QA");
            UserEntity manager = TaskTestDataMother.createTestUserWithDepartment(1L, "Manager", "MANAGER", itDepartment);
            TaskEntity task = TaskTestDataMother.createTestTask(1L, "Задача", TaskStatus.OPEN, manager);
            task.setDepartment(qaDepartment); // Задача в другом отделе

            Mockito.when(userRepository.findByUsername("Manager")).thenReturn(Optional.of(manager));
            Mockito.when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

            assertThrows(RuntimeException.class, () -> taskService.deleteByIdForManager(1L, "Manager"));

            Mockito.verify(taskRepository, Mockito.never()).delete(Mockito.any());
        }

        @Test
        void deleteByIdForManager_ShouldThrowException_WhenUserNotFound(){
            Mockito.when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> taskService.deleteByIdForManager(1L, "nonexistent"));

            Mockito.verify(taskRepository, Mockito.never()).findById(Mockito.any());
            Mockito.verify(taskRepository, Mockito.never()).delete(Mockito.any());
        }

        @Test
        void deleteByIdForManager_ShouldThrowException_WhenTaskNotFound(){
            DepartmentEntity department = TaskTestDataMother.createTestDepartment(1L, "IT");
            UserEntity manager = TaskTestDataMother.createTestUserWithDepartment(1L, "Manager", "MANAGER", department);

            Mockito.when(userRepository.findByUsername("Manager")).thenReturn(Optional.of(manager));
            Mockito.when(taskRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> taskService.deleteByIdForManager(1L, "Manager"));

            Mockito.verify(taskRepository, Mockito.never()).delete(Mockito.any());
        }

        @Test
        void deleteByIdForManager_ShouldThrowException_WhenManagerHasNoDepartment(){
            UserEntity manager = TaskTestDataMother.createTestUser(1L, "Manager", "MANAGER");
            manager.setDepartment(null);

            Mockito.when(userRepository.findByUsername("Manager")).thenReturn(Optional.of(manager));

            assertThrows(RuntimeException.class, () -> taskService.deleteByIdForManager(1L, "Manager"));

            Mockito.verify(taskRepository, Mockito.never()).findById(Mockito.any());
            Mockito.verify(taskRepository, Mockito.never()).delete(Mockito.any());
        }
    }

    @Nested
    @DisplayName("Тесты удаления для ADMIN")
    class AdminDeleteTests{
        @Test
        void deleteById_ShouldDeleteTask(){
            UserEntity user = TaskTestDataMother.createTestUser(1L, "User", "USER");
            TaskEntity task = TaskTestDataMother.createTestTask(1L, "Задача", TaskStatus.OPEN, user);

            Mockito.when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

            taskService.deleteById(1L);

            Mockito.verify(taskRepository, Mockito.times(1)).delete(task);
        }

        @Test
        void deleteById_ShouldThrowException_WhenTaskNotFound(){
            Mockito.when(taskRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> taskService.deleteById(1L));

            Mockito.verify(taskRepository, Mockito.never()).delete(Mockito.any());
        }
    }

    @Nested
    @DisplayName("Тесты получения/обновления задач для MANAGER с проверкой отдела")
    class ManagerWithDepartmentCheckTests{
        @Test
        void getTaskByIdForManagerWithDepartmentCheck_ShouldReturnTask_WhenTaskInSameDepartment(){
            DepartmentEntity department = TaskTestDataMother.createTestDepartment(1L, "IT");
            UserEntity manager = TaskTestDataMother.createTestUserWithDepartment(1L, "Manager", "MANAGER", department);
            TaskEntity task = TaskTestDataMother.createTestTask(1L, "Задача", TaskStatus.OPEN, manager);
            task.setDepartment(department);

            Mockito.when(userRepository.findByUsername("Manager")).thenReturn(Optional.of(manager));
            Mockito.when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

            TaskDTO result = taskService.getTaskByIdForManagerWithDepartmentCheck(1L, "Manager");

            assertNotNull(result);
            assertEquals("Задача", result.content());
        }

        @Test
        void getTaskByIdForManagerWithDepartmentCheck_ShouldThrowException_WhenTaskInDifferentDepartment(){
            DepartmentEntity itDepartment = TaskTestDataMother.createTestDepartment(1L, "IT");
            DepartmentEntity qaDepartment = TaskTestDataMother.createTestDepartment(2L, "QA");
            UserEntity manager = TaskTestDataMother.createTestUserWithDepartment(1L, "Manager", "MANAGER", itDepartment);
            TaskEntity task = TaskTestDataMother.createTestTask(1L, "Задача", TaskStatus.OPEN, manager);
            task.setDepartment(qaDepartment);

            Mockito.when(userRepository.findByUsername("Manager")).thenReturn(Optional.of(manager));
            Mockito.when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

            assertThrows(RuntimeException.class, () -> taskService.getTaskByIdForManagerWithDepartmentCheck(1L, "Manager"));
        }

        @Test
        void updateTaskForManagerWithDepartmentCheck_ShouldUpdateTask_WhenTaskInSameDepartment(){
            DepartmentEntity department = TaskTestDataMother.createTestDepartment(1L, "IT");
            UserEntity manager = TaskTestDataMother.createTestUserWithDepartment(1L, "Manager", "MANAGER", department);
            TaskEntity task = TaskTestDataMother.createTestTask(1L, "Задача", TaskStatus.OPEN, manager);
            task.setDepartment(department);

            TaskDTO updateDTO = new TaskDTO(1L, "Обновленная задача", "Manager", "DONE", null);

            Mockito.when(userRepository.findByUsername("Manager")).thenReturn(Optional.of(manager));
            Mockito.when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
            Mockito.when(taskRepository.save(Mockito.any(TaskEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TaskDTO result = taskService.updateTaskForManagerWithDepartmentCheck(1L, updateDTO, "Manager");

            assertNotNull(result);
            assertEquals("Обновленная задача", result.content());
            assertEquals("DONE", result.status());
        }

        @Test
        void updateTaskForManagerWithDepartmentCheck_ShouldThrowException_WhenTaskInDifferentDepartment(){
            DepartmentEntity itDepartment = TaskTestDataMother.createTestDepartment(1L, "IT");
            DepartmentEntity qaDepartment = TaskTestDataMother.createTestDepartment(2L, "QA");
            UserEntity manager = TaskTestDataMother.createTestUserWithDepartment(1L, "Manager", "MANAGER", itDepartment);
            TaskEntity task = TaskTestDataMother.createTestTask(1L, "Задача", TaskStatus.OPEN, manager);
            task.setDepartment(qaDepartment);

            TaskDTO updateDTO = new TaskDTO(1L, "Обновленная задача", "Manager", "DONE", null);

            Mockito.when(userRepository.findByUsername("Manager")).thenReturn(Optional.of(manager));
            Mockito.when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

            assertThrows(RuntimeException.class, () -> taskService.updateTaskForManagerWithDepartmentCheck(1L, updateDTO, "Manager"));

            Mockito.verify(taskRepository, Mockito.never()).save(Mockito.any());
        }

        @Test
        void getTaskByIdForManagerWithDepartmentCheck_ShouldThrowException_WhenManagerNotFound(){
            Mockito.when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> taskService.getTaskByIdForManagerWithDepartmentCheck(1L, "nonexistent"));
        }

        @Test
        void getTaskByIdForManagerWithDepartmentCheck_ShouldThrowException_WhenManagerHasNoDepartment(){
            UserEntity manager = TaskTestDataMother.createTestUser(1L, "Manager", "MANAGER");
            manager.setDepartment(null);

            Mockito.when(userRepository.findByUsername("Manager")).thenReturn(Optional.of(manager));

            assertThrows(RuntimeException.class,
                    () -> taskService.getTaskByIdForManagerWithDepartmentCheck(1L, "Manager"));
        }

        @Test
        void getTaskByIdForManagerWithDepartmentCheck_ShouldThrowException_WhenTaskNotFound(){
            DepartmentEntity department = TaskTestDataMother.createTestDepartment(1L, "IT");
            UserEntity manager = TaskTestDataMother.createTestUserWithDepartment(1L, "Manager", "MANAGER", department);

            Mockito.when(userRepository.findByUsername("Manager")).thenReturn(Optional.of(manager));
            Mockito.when(taskRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> taskService.getTaskByIdForManagerWithDepartmentCheck(1L, "Manager"));
        }

        @Test
        void updateTaskForManagerWithDepartmentCheck_ShouldThrowException_WhenManagerNotFound(){
            TaskDTO updateDTO = new TaskDTO(1L, "Обновленная задача", "Manager", "DONE", null);

            Mockito.when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> taskService.updateTaskForManagerWithDepartmentCheck(1L, updateDTO, "nonexistent"));
        }

        @Test
        void updateTaskForManagerWithDepartmentCheck_ShouldThrowException_WhenManagerHasNoDepartment(){
            UserEntity manager = TaskTestDataMother.createTestUser(1L, "Manager", "MANAGER");
            manager.setDepartment(null);
            TaskDTO updateDTO = new TaskDTO(1L, "Обновленная задача", "Manager", "DONE", null);

            Mockito.when(userRepository.findByUsername("Manager")).thenReturn(Optional.of(manager));

            assertThrows(RuntimeException.class,
                    () -> taskService.updateTaskForManagerWithDepartmentCheck(1L, updateDTO, "Manager"));
        }

        @Test
        void updateTaskForManagerWithDepartmentCheck_ShouldThrowException_WhenTaskNotFound(){
            DepartmentEntity department = TaskTestDataMother.createTestDepartment(1L, "IT");
            UserEntity manager = TaskTestDataMother.createTestUserWithDepartment(1L, "Manager", "MANAGER", department);
            TaskDTO updateDTO = new TaskDTO(1L, "Обновленная задача", "Manager", "DONE", null);

            Mockito.when(userRepository.findByUsername("Manager")).thenReturn(Optional.of(manager));
            Mockito.when(taskRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> taskService.updateTaskForManagerWithDepartmentCheck(1L, updateDTO, "Manager"));
        }
    }

    @Nested
    @DisplayName("Примитивные методы")
    class simpleMethods{
        // -------------------------------------------------------------------------------------------------------------------------------
        @Test
        void deleteByIdAndUsername_ShouldVerify(){
            DepartmentEntity department = TaskTestDataMother.createTestDepartment(1L, "IT");
            UserEntity user = TaskTestDataMother.createTestUserWithDepartment(1L, "John", "USER", department);

            TaskEntity taskToDelete = TaskTestDataMother.createTestTask(
                    1L,
                    "task 1",
                    TaskStatus.OPEN,
                    user
            );
            taskToDelete.setDepartment(department);

            Mockito.when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
            Mockito.when(taskRepository.findByIdAndUsernameAndDepartmentId(
                    taskToDelete.getId(),
                    user.getUsername(),
                    department.getId()
            )).thenReturn(Optional.of(taskToDelete));

            taskService.deleteByIdAndUsername(
                    taskToDelete.getId(),
                    user.getUsername()
            );
            Mockito.verify(taskRepository,Mockito.times(1)).delete(taskToDelete);
        }
        @Test
        void deleteByIdAndUsername_ShouldThrowException_WhenTaskNotFound(){
            DepartmentEntity department = TaskTestDataMother.createTestDepartment(1L, "IT");
            UserEntity user = TaskTestDataMother.createTestUserWithDepartment(1L, "John", "USER", department);

            Mockito.when(userRepository.findByUsername("John")).thenReturn(Optional.of(user));
            Mockito.when(taskRepository.findByIdAndUsernameAndDepartmentId(999L, "John", department.getId()))
                    .thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class,
                    ()->{taskService.deleteByIdAndUsername(999L,"John");
            });
            Mockito.verify(taskRepository, Mockito.never()).delete(Mockito.any(TaskEntity.class));
        }

        // -------------------------------------------------------------------------------------------------------------------------------

        @Test
        void updateDate_ShouldReturnUpdatedTask(){
            DepartmentEntity department = TaskTestDataMother.createTestDepartment(1L, "IT");
            UserEntity user = TaskTestDataMother.createTestUserWithDepartment(1L,"John","USER", department);
            TaskEntity taskToUpdate = TaskTestDataMother.createTestTask(
                    1L,
                    "Задача 1",
                    TaskStatus.OPEN,
                    user
                    );
            taskToUpdate.setDepartment(department);

            Mockito.when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
            Mockito.when(taskRepository.findByIdAndUsernameAndDepartmentId(
                    taskToUpdate.getId(),
                    user.getUsername(),
                    department.getId()
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
                    .findByIdAndUsernameAndDepartmentId(1L,"John", department.getId());
        }

        @Test
        void updateData_ShouldReturnException_WhenTaskNotFound(){
            DepartmentEntity department = TaskTestDataMother.createTestDepartment(1L, "IT");
            UserEntity user = TaskTestDataMother.createTestUserWithDepartment(1L, "John", "USER", department);

            TaskDTO requestDTO = new TaskDTO(
                    999L,
                    "Задача 2",
                    "John",
                    "DONE",
                    null
            );

            Mockito.when(userRepository.findByUsername("John")).thenReturn(Optional.of(user));
            Mockito.when(taskRepository.findByIdAndUsernameAndDepartmentId(999L, "John", department.getId()))
                    .thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> taskService.updatedData(999L, requestDTO, "John"));

            Mockito.verify(taskRepository, Mockito.never())
                    .save(Mockito.any(TaskEntity.class));
        }

        // -------------------------------------------------------------------------------------------------------------------------------

        @Test
        void getTaskByID_ShouldReturnTask(){
            DepartmentEntity department = TaskTestDataMother.createTestDepartment(1L, "IT");
            UserEntity user = TaskTestDataMother.createTestUserWithDepartment(1L, "John", "USER", department);
            TaskEntity task = TaskTestDataMother.createTestTask(
                    1L,
                    "Задача 1",
                    TaskStatus.OPEN,
                    user
            );
            task.setDepartment(department);

            Mockito.when(userRepository.findByUsername("John")).thenReturn(Optional.of(user));
            Mockito.when(taskRepository.findByIdAndUsernameAndDepartmentId(1L, "John", department.getId()))
                    .thenReturn(Optional.of(task));

            TaskDTO result = taskService.getTaskByID(1L, "John");

            assertNotNull(result);
            assertEquals("Задача 1", result.content());
            assertEquals("OPEN", result.status());
            assertEquals("John", result.assignedUsername());

            Mockito.verify(taskRepository, Mockito.times(1))
                    .findByIdAndUsernameAndDepartmentId(1L, "John", department.getId());
        }

        @Test
        void getTaskByID_ShouldThrowException_WhenTaskNotFound(){
            DepartmentEntity department = TaskTestDataMother.createTestDepartment(1L, "IT");
            UserEntity user = TaskTestDataMother.createTestUserWithDepartment(1L, "John", "USER", department);

            Mockito.when(userRepository.findByUsername("John")).thenReturn(Optional.of(user));
            Mockito.when(taskRepository.findByIdAndUsernameAndDepartmentId(999L, "John", department.getId()))
                    .thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> taskService.getTaskByID(999L, "John"));

            Mockito.verify(taskRepository, Mockito.times(1))
                    .findByIdAndUsernameAndDepartmentId(999L, "John", department.getId());
        }

        @Test
        void getTaskByID_ShouldThrowException_WhenUserHasNoDepartment(){
            UserEntity user = TaskTestDataMother.createTestUser(1L, "John", "USER");
            user.setDepartment(null);

            Mockito.when(userRepository.findByUsername("John")).thenReturn(Optional.of(user));

            assertThrows(NullPointerException.class,
                    () -> taskService.getTaskByID(1L, "John"));
        }

        @Test
        void updatedData_ShouldThrowException_WhenUserHasNoDepartment(){
            UserEntity user = TaskTestDataMother.createTestUser(1L, "John", "USER");
            user.setDepartment(null);

            TaskDTO requestDTO = new TaskDTO(
                    1L,
                    "Задача 2",
                    "John",
                    "DONE",
                    null
            );

            Mockito.when(userRepository.findByUsername("John")).thenReturn(Optional.of(user));

            assertThrows(NullPointerException.class,
                    () -> taskService.updatedData(1L, requestDTO, "John"));
        }

        @Test
        void deleteByIdAndUsername_ShouldThrowException_WhenUserHasNoDepartment(){
            UserEntity user = TaskTestDataMother.createTestUser(1L, "John", "USER");
            user.setDepartment(null);

            Mockito.when(userRepository.findByUsername("John")).thenReturn(Optional.of(user));

            assertThrows(NullPointerException.class,
                    () -> taskService.deleteByIdAndUsername(1L, "John"));
        }

        @Test
        void getTaskByID_ShouldThrowException_WhenTaskInDifferentDepartment(){
            DepartmentEntity itDepartment = TaskTestDataMother.createTestDepartment(1L, "IT");
            DepartmentEntity qaDepartment = TaskTestDataMother.createTestDepartment(2L, "QA");
            UserEntity user = TaskTestDataMother.createTestUserWithDepartment(1L, "John", "USER", itDepartment);
            TaskEntity task = TaskTestDataMother.createTestTask(1L, "Задача", TaskStatus.OPEN, user);
            task.setDepartment(qaDepartment);

            Mockito.when(userRepository.findByUsername("John")).thenReturn(Optional.of(user));
            Mockito.when(taskRepository.findByIdAndUsernameAndDepartmentId(1L, "John", itDepartment.getId()))
                    .thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> taskService.getTaskByID(1L, "John"));
        }

        @Test
        void updatedData_ShouldThrowException_WhenTaskInDifferentDepartment(){
            DepartmentEntity itDepartment = TaskTestDataMother.createTestDepartment(1L, "IT");
            DepartmentEntity qaDepartment = TaskTestDataMother.createTestDepartment(2L, "QA");
            UserEntity user = TaskTestDataMother.createTestUserWithDepartment(1L, "John", "USER", itDepartment);
            TaskEntity task = TaskTestDataMother.createTestTask(1L, "Задача", TaskStatus.OPEN, user);
            task.setDepartment(qaDepartment);

            TaskDTO requestDTO = new TaskDTO(
                    1L,
                    "Задача 2",
                    "John",
                    "DONE",
                    null
            );

            Mockito.when(userRepository.findByUsername("John")).thenReturn(Optional.of(user));
            Mockito.when(taskRepository.findByIdAndUsernameAndDepartmentId(1L, "John", itDepartment.getId()))
                    .thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> taskService.updatedData(1L, requestDTO, "John"));
        }

        @Test
        void deleteByIdAndUsername_ShouldThrowException_WhenTaskInDifferentDepartment(){
            DepartmentEntity itDepartment = TaskTestDataMother.createTestDepartment(1L, "IT");
            DepartmentEntity qaDepartment = TaskTestDataMother.createTestDepartment(2L, "QA");
            UserEntity user = TaskTestDataMother.createTestUserWithDepartment(1L, "John", "USER", itDepartment);
            TaskEntity task = TaskTestDataMother.createTestTask(1L, "Задача", TaskStatus.OPEN, user);
            task.setDepartment(qaDepartment);

            Mockito.when(userRepository.findByUsername("John")).thenReturn(Optional.of(user));
            Mockito.when(taskRepository.findByIdAndUsernameAndDepartmentId(1L, "John", itDepartment.getId()))
                    .thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> taskService.deleteByIdAndUsername(1L, "John"));
        }

        // -------------------------------------------------------------------------------------------------------------------------------

        @Test
        void getTaskByIdForAdmin_ShouldReturnTask(){
            UserEntity user = TaskTestDataMother.createTestUser(1L, "John", "USER");
            TaskEntity task = TaskTestDataMother.createTestTask(
                    1L,
                    "Задача 1",
                    TaskStatus.OPEN,
                    user
            );

            Mockito.when(taskRepository.findById(1L))
                    .thenReturn(Optional.of(task));

            TaskDTO result = taskService.getTaskByIdForAdmin(1L);

            assertNotNull(result);
            assertEquals("Задача 1", result.content());
            assertEquals("OPEN", result.status());
            assertEquals("John", result.assignedUsername());

            Mockito.verify(taskRepository, Mockito.times(1))
                    .findById(1L);
        }

        @Test
        void getTaskByIdForAdmin_ShouldThrowException_WhenTaskNotFound(){
            Mockito.when(taskRepository.findById(999L))
                    .thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> taskService.getTaskByIdForAdmin(999L));

            Mockito.verify(taskRepository, Mockito.times(1))
                    .findById(999L);
        }

        // -------------------------------------------------------------------------------------------------------------------------------

        @Test
        void updateTaskForAdmin_ShouldReturnUpdatedTask(){
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

            TaskDTO result = taskService.updateTaskForAdmin(1L, requestDTO);

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
        void updateTaskForAdmin_ShouldThrowException_WhenTaskNotFound(){
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
                    () -> taskService.updateTaskForAdmin(999L, requestDTO));

            Mockito.verify(taskRepository, Mockito.never())
                    .save(Mockito.any(TaskEntity.class));
        }
    }


}
