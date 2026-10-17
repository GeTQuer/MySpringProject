package com.getquer.tasktracker.responseDTO;

public record TaskStatisticsDTO(
        long total,
        long done,
        long inProgress,
        long todo,
        long overdue
) {}