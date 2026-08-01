package com.getquer.tasktracker;

import com.getquer.tasktracker.Entities.DepartmentEntity;
import com.getquer.tasktracker.Entities.TaskCommentEntity;
import com.getquer.tasktracker.Entities.TaskEntity;
import com.getquer.tasktracker.Entities.UserEntity;
import com.getquer.tasktracker.Repositories.TaskCommentsRepository;
import com.getquer.tasktracker.Repositories.TaskRepository;
import com.getquer.tasktracker.Repositories.UserRepository;
import com.getquer.tasktracker.responseDTO.CommentDTO;
import com.getquer.tasktracker.security.TaskAccessPolicy;
import com.getquer.tasktracker.service.TaskCommentService;
import com.getquer.tasktracker.util.TaskTestDataMother;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskCommentServiceTest {

    @Mock
    private TaskCommentsRepository taskCommentsRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Spy
    private TaskAccessPolicy taskAccessPolicy = new TaskAccessPolicy();
    @InjectMocks
    private TaskCommentService taskCommentService;

    @Test
    void addComment_ShouldUseAuthenticatedUserAsAuthor() {
        DepartmentEntity department = TaskTestDataMother.createTestDepartment(1L, "IT");
        UserEntity actor = TaskTestDataMother.createTestUserWithDepartment(
                1L, "john", "USER", department
        );
        TaskEntity task = TaskTestDataMother.createTestTask(
                10L, "Task", TaskStatus.OPEN, actor
        );
        task.setDepartment(department);

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(actor));
        when(taskCommentsRepository.save(any(TaskCommentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CommentDTO result = taskCommentService.addComment(10L, "john", "My comment");

        assertEquals("My comment", result.content());
        assertEquals(10L, result.task_id());
        assertEquals(1L, result.author_id());
        verify(taskCommentsRepository).save(any(TaskCommentEntity.class));
    }

    @Test
    void addComment_ShouldRejectUserWhoIsNotAssignedToTask() {
        DepartmentEntity department = TaskTestDataMother.createTestDepartment(1L, "IT");
        UserEntity owner = TaskTestDataMother.createTestUserWithDepartment(
                1L, "owner", "USER", department
        );
        UserEntity actor = TaskTestDataMother.createTestUserWithDepartment(
                2L, "john", "USER", department
        );
        TaskEntity task = TaskTestDataMother.createTestTask(
                10L, "Task", TaskStatus.OPEN, owner
        );
        task.setDepartment(department);

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(actor));

        assertThrows(
                AccessDeniedException.class,
                () -> taskCommentService.addComment(10L, "john", "My comment")
        );

        verify(taskCommentsRepository, never()).save(any(TaskCommentEntity.class));
    }
}
