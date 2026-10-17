package com.getquer.tasktracker.requestDTO;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;


public record ReportRequest(
        @NotNull LocalDate from,
        @NotNull LocalDate toExclusive
) {}
