package com.getquer.tasktracker.responseDTO;

import com.getquer.tasktracker.Enums.ReportStatus;

import java.util.UUID;

public record ReportJobCreatedDTO(
        UUID jobId,
        ReportStatus status
) {}
