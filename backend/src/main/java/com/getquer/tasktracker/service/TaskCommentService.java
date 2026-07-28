package com.getquer.tasktracker.service;

import com.getquer.tasktracker.Entities.TaskCommentEntity;
import com.getquer.tasktracker.Entities.TaskEntity;
import com.getquer.tasktracker.Entities.UserEntity;
import com.getquer.tasktracker.Repositories.TaskCommentsRepository;
import com.getquer.tasktracker.Repositories.TaskRepository;
import com.getquer.tasktracker.Repositories.UserRepository;
import com.getquer.tasktracker.responseDTO.CommentDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class TaskCommentService {
    private final TaskCommentsRepository taskCommentsRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskCommentService(TaskCommentsRepository taskCommentsRepository, TaskRepository taskRepository, UserRepository userRepository) {
        this.taskCommentsRepository = taskCommentsRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CommentDTO addComment(Long taskId, Long authorId, String text) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));
        UserEntity user = userRepository.findById(authorId)
                .orElseThrow(() -> new EntityNotFoundException("Author not found"));

        TaskCommentEntity comment = new TaskCommentEntity();
        comment.setTaskComment(text);
        comment.setTask(task);
        comment.setAuthor(user);

        return mapToDTO(taskCommentsRepository.save(comment));
    }

    public String findComment(Long commentId, Long authorId) {
        TaskCommentEntity comment = taskCommentsRepository.findByIdAndAuthor_Id(commentId, authorId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found or you are not the author"));

        return comment.getTaskComment();
    }

    public Page<CommentDTO> findAllCommentsById(Long taskId, int page, int size) {
        if (!taskRepository.existsById(taskId)) {
            throw new EntityNotFoundException("Task not found");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<TaskCommentEntity> allComments = taskCommentsRepository.findAllByTask_Id(taskId, pageable);

        return allComments.map(this::mapToDTO);
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