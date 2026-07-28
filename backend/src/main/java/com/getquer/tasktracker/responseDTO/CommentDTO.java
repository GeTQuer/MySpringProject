package com.getquer.tasktracker.responseDTO;

import java.time.LocalDateTime;

public record CommentDTO(
        String content,
        Long task_id,
        Long author_id,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
