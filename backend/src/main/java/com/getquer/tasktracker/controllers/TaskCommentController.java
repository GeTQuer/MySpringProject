package com.getquer.tasktracker.controllers;

import com.getquer.tasktracker.requestDTO.CreateCommentRequest;
import com.getquer.tasktracker.responseDTO.CommentDTO;
import com.getquer.tasktracker.service.TaskCommentService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TaskCommentController {
    private final TaskCommentService taskCommentService;

    public TaskCommentController(TaskCommentService taskCommentService) {
        this.taskCommentService = taskCommentService;
    }

    @PostMapping("/tasks/{taskId}/comments")
    public ResponseEntity<CommentDTO> addComment(
            @PathVariable Long taskId,
            @RequestBody CreateCommentRequest request
    ) {
        CommentDTO createdComment = taskCommentService.addComment(
                taskId,
                request.authorId(),
                request.text()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(createdComment);
    }


    @GetMapping("/tasks/{taskId}/comments")
    public ResponseEntity<Page<CommentDTO>> getAllComments(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Page<CommentDTO> comments = taskCommentService.findAllCommentsById(taskId, page, size);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/comments/{commentId}")
    public ResponseEntity<String> getComment(
            @PathVariable Long commentId,
            @RequestParam Long authorId
    ) {
        String commentText = taskCommentService.findComment(commentId, authorId);
        return ResponseEntity.ok(commentText);
    }
}
