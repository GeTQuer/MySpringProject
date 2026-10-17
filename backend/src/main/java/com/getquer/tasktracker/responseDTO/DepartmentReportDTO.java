package com.getquer.tasktracker.responseDTO;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DepartmentReportDTO(
        Long departmentId,
        String departmentName,
        LocalDate from,
        LocalDate toExclusive,
        Instant generatedAt,
        TaskStatisticsDTO totals,
        List<EmployeeReportDTO> employees
) {}