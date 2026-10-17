package com.getquer.tasktracker.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import com.getquer.tasktracker.Enums.ReportStatus;

@Entity
@Table(name = "report_jobs")
@Getter
@Setter
@NoArgsConstructor
public class ReportJobEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID uuid; // Генерируем через UUID.randomUUID()

    @Column(name = "requested_by", nullable = false, updatable = false)
    private Long requestedBy; // ID менеджера, запросившего отчёт

    @Column(name = "department_id", nullable = false, updatable = false)
    private Long departmentId;

    @Column(name = "period_from", nullable = false, updatable = false)
    private LocalDate periodFrom; // Начало периода включительно

    @Column(name = "period_to_exclusive", nullable = false, updatable = false)
    private LocalDate periodToExclusive; // Конец периода не включается

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson; // Готовый DepartmentReportDTO в JSON

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt; // Когда приняли запрос

    @Column(name = "started_at")
    private Instant startedAt; // Когда worker начал обработку

    @Column(name = "finished_at")
    private Instant finishedAt; // Когда обработка завершилась успешно или с ошибкой

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "attempt_id")
    private UUID attemptId;
}
