package com.getquer.tasktracker.responseDTO;

public record EmployeeReportDTO(
        Long userId,
        String username,
        TaskStatisticsDTO statistics
) {}