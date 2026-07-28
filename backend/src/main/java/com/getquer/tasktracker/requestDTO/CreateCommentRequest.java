package com.getquer.tasktracker.requestDTO;

public record CreateCommentRequest (
    Long authorId,
    String text
){}
