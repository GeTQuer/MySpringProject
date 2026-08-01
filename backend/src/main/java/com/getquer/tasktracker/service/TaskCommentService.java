package com.getquer.tasktracker.service;

import com.getquer.tasktracker.Entities.TaskCommentEntity;
import com.getquer.tasktracker.Entities.TaskEntity;
import com.getquer.tasktracker.Entities.UserEntity;
import com.getquer.tasktracker.Repositories.TaskCommentsRepository;
import com.getquer.tasktracker.Repositories.TaskRepository;
import com.getquer.tasktracker.Repositories.UserRepository;
import com.getquer.tasktracker.responseDTO.CommentDTO;
import com.getquer.tasktracker.security.TaskAccessPolicy;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskCommentService {
    private final TaskCommentsRepository taskCommentsRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskAccessPolicy taskAccessPolicy;

    public TaskCommentService(
            TaskCommentsRepository taskCommentsRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            TaskAccessPolicy taskAccessPolicy
    ) {
        this.taskCommentsRepository = taskCommentsRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskAccessPolicy = taskAccessPolicy;
    }

    @Transactional
    public CommentDTO addComment(Long taskId, String username, String text) {
        TaskEntity task = findTask(taskId);
        UserEntity actor = findActor(username);

        taskAccessPolicy.checkCanAccess(task, actor);

        TaskCommentEntity comment = new TaskCommentEntity();
        comment.setTaskComment(text);
        comment.setTask(task);
        comment.setAuthor(actor);

        return mapToDTO(taskCommentsRepository.save(comment));
    }

    @Transactional(readOnly = true)
    public String findComment(Long commentId, String username) {
        TaskCommentEntity comment = taskCommentsRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found"));
        UserEntity actor = findActor(username);

        taskAccessPolicy.checkCanAccess(comment.getTask(), actor);

        return comment.getTaskComment();
    }

    @Transactional(readOnly = true)
    public Page<CommentDTO> findAllCommentsById(Long taskId, String username, int page, int size) {
        TaskEntity task = findTask(taskId);
        UserEntity actor = findActor(username);

        taskAccessPolicy.checkCanAccess(task, actor);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<TaskCommentEntity> allComments = taskCommentsRepository.findAllByTask_Id(taskId, pageable);

        return allComments.map(this::mapToDTO);
    }

    private TaskEntity findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));
    }

    private UserEntity findActor(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private CommentDTO mapToDTO(TaskCommentEntity taskCommentEntity) {
        return new CommentDTO(
                taskCommentEntity.getTaskComment(),
                taskCommentEntity.getTask().getId(),
                taskCommentEntity.getAuthor().getId(),
                taskCommentEntity.getCreatedAt(),
                taskCommentEntity.getUpdatedAt()
        );
    }
}
