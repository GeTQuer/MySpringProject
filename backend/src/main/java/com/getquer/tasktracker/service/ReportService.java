package com.getquer.tasktracker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.getquer.tasktracker.Entities.DepartmentEntity;
import com.getquer.tasktracker.Entities.ReportJobEntity;
import com.getquer.tasktracker.Entities.TaskEntity;
import com.getquer.tasktracker.Entities.UserEntity;
import com.getquer.tasktracker.Enums.TaskStatus;
import com.getquer.tasktracker.Repositories.DepartmentRepository;
import com.getquer.tasktracker.Repositories.ReportJobRepositorty;
import com.getquer.tasktracker.Repositories.TaskRepository;
import com.getquer.tasktracker.Repositories.UserRepository;
import com.getquer.tasktracker.events.ReportRequestEvent;
import com.getquer.tasktracker.requestDTO.ReportRequest;
import com.getquer.tasktracker.responseDTO.DepartmentReportDTO;
import com.getquer.tasktracker.responseDTO.EmployeeReportDTO;
import com.getquer.tasktracker.responseDTO.ReportJobCreatedDTO;
import com.getquer.tasktracker.responseDTO.TaskStatisticsDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;
import com.getquer.tasktracker.responseDTO.ReportJobStatusDTO;

import com.getquer.tasktracker.Enums.ReportStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class ReportService {
    private static final int MAX_ATTEMPTS = 5;
    private static final long RETRY_DELAY_SECONDS = 30;


    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;
    private final ReportJobRepositorty reportJobRepositorty;
    private final ObjectMapper objectMapper;
    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public DepartmentReportDTO getReport(UUID jobId, String username) {
        ReportJobEntity job = findOwnedJob(jobId, username);
        if (job.getStatus() != ReportStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Отчёт ещё не готов или завершился ошибкой"
            );
        }
        try {
            return objectMapper.readValue(job.getResultJson(), DepartmentReportDTO.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Не удалось прочитать сохранённый отчёт", exception);
        }
    }

    @Transactional(readOnly = true)
    public ReportJobStatusDTO getStatus(UUID jobId, String username) {
        ReportJobEntity job = findOwnedJob(jobId, username);
        return new ReportJobStatusDTO(job.getUuid(), job.getStatus(), job.getErrorMessage());
    }

    @Transactional
    public ReportJobCreatedDTO requestReport(
            ReportRequest reportRequest,
            String name
    ){
        UserEntity user = findActor(name);
        DepartmentEntity userDepartment = user.getDepartment();


        if (userDepartment == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Менеджеру не назначен отдел");
        }


        if (reportRequest.from() == null || reportRequest.toExclusive() == null
                || !reportRequest.from().isBefore(reportRequest.toExclusive())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Начало периода должно быть раньше конца"
            );
        }
        ReportJobEntity reportJobEntity = new ReportJobEntity();
        reportJobEntity.setUuid(UUID.randomUUID());
        reportJobEntity.setRequestedBy(user.getId());
        reportJobEntity.setDepartmentId(userDepartment.getId());
        reportJobEntity.setPeriodFrom(reportRequest.from());
        reportJobEntity.setPeriodToExclusive(reportRequest.toExclusive());
        reportJobEntity.setStatus(ReportStatus.PENDING);
        reportJobEntity.setCreatedAt(Instant.now());

        reportJobRepositorty.save(reportJobEntity);

        publisher.publishEvent(new ReportRequestEvent(this,
                reportJobEntity.getUuid()));
        return new ReportJobCreatedDTO(
                reportJobEntity.getUuid(),
                reportJobEntity.getStatus()
        );

    }


    private UserEntity findActor(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
    private ReportJobEntity findJob(UUID jobId) {
        return reportJobRepositorty.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Report not found"));
    }

    private ReportJobEntity findOwnedJob(UUID jobId, String username) {
        UserEntity actor = findActor(username);
        ReportJobEntity job = findJob(jobId);
        if (!Objects.equals(job.getRequestedBy(), actor.getId())
                || actor.getDepartment() == null
                || !Objects.equals(job.getDepartmentId(), actor.getDepartment().getId())) {
            throw new AccessDeniedException("Нет доступа к этому отчёту");
        }
        return job;
    }


    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public DepartmentReportDTO buildReport(UUID jobId) {
        ReportJobEntity job = findJob(jobId);



        DepartmentEntity department = departmentRepository
                .findById(job.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found"));

        List<UserEntity> employees =
                userRepository.findAllByDepartment_IdAndRoleOrderByUsernameAsc(
                        job.getDepartmentId(),
                        "USER"
                );
        List<TaskEntity> tasks = taskRepository.findByDepartmentIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                job.getDepartmentId(),
                job.getPeriodFrom().atStartOfDay(),
                job.getPeriodToExclusive().atStartOfDay()
        );
        Instant generatedAt = Instant.now();
        LocalDateTime asOf = LocalDateTime.ofInstant(generatedAt, ZoneId.systemDefault());
        Set<Long> employeeIds = employees.stream()
                .map(UserEntity::getId)
                .collect(Collectors.toSet());
        List<TaskEntity> reportTasks = tasks.stream()
                .filter(task -> employeeIds.contains(task.getUser().getId()))
                .toList();

        TaskStatisticsDTO statistics =
                calculateStatistics(reportTasks, asOf);

        Map<Long,List<TaskEntity>> tasksByEmployee = reportTasks.stream()
                .collect(Collectors.groupingBy(
                        task -> task.getUser().getId()
                ));

        List<EmployeeReportDTO> employeeReports = employees.stream()
                .map(e ->{
                    List<TaskEntity> employeeTasks = tasksByEmployee.getOrDefault(
                            e.getId(),
                            List.of()
                    );
                    return new EmployeeReportDTO(
                            e.getId(),
                            e.getUsername(),
                            calculateStatistics(employeeTasks, asOf)
                    );
                })
                .toList();


        return new DepartmentReportDTO(
                job.getDepartmentId(),
                department.getName(),
                job.getPeriodFrom(),
                job.getPeriodToExclusive(),
                generatedAt,
                statistics,
                employeeReports
        );

    }

    @Transactional
    public Optional<UUID> tryStartAttempt(UUID jobId) {
        ReportJobEntity job = findJobForUpdate(jobId);

        Instant now = Instant.now();

        boolean firstAttempt = job.getStatus() == ReportStatus.PENDING;

        boolean retryDue = job.getStatus() == ReportStatus.RETRY_PENDING
                && job.getNextAttemptAt() != null
                && !job.getNextAttemptAt().isAfter(now);

        if (!firstAttempt && !retryDue) {
            return Optional.empty();
        }

        if (job.getAttemptCount() >= MAX_ATTEMPTS) {
            job.setStatus(ReportStatus.FAILED);
            job.setFinishedAt(now);
            job.setNextAttemptAt(null);
            job.setErrorMessage("Исчерпан лимит попыток");
            return Optional.empty();
        }

        UUID attemptId = UUID.randomUUID();

        job.setStatus(ReportStatus.PROCESSING);
        job.setAttemptCount(job.getAttemptCount() + 1);
        job.setAttemptId(attemptId);
        job.setStartedAt(now);
        job.setNextAttemptAt(null);
        job.setFinishedAt(null);
        job.setErrorMessage(null);

        return Optional.of(attemptId);
    }

    @Transactional
    public void markCompleted(
            UUID jobId,
            UUID attemptId,
            DepartmentReportDTO report
    ) throws JsonProcessingException {
        ReportJobEntity job = findJobForUpdate(jobId);
        if (!isCurrentAttempt(job, attemptId)) {
            return;
        }
        job.setResultJson(objectMapper.writeValueAsString(report));
        job.setStatus(ReportStatus.COMPLETED);
        job.setErrorMessage(null);
        job.setNextAttemptAt(null);
        job.setFinishedAt(Instant.now());
    }

    @Transactional
    public void markFailed(UUID jobId, UUID attemptId, Exception exception) {
        log.error("Failed to generate report {}", jobId, exception);
        ReportJobEntity job = findJobForUpdate(jobId);
        if (!isCurrentAttempt(job, attemptId)) {
            return;
        }
        job.setStatus(ReportStatus.FAILED);
        job.setErrorMessage("Не удалось сформировать отчёт");
        job.setResultJson(null);
        job.setNextAttemptAt(null);
        job.setFinishedAt(Instant.now());
    }

    @Transactional
    public void scheduleRetry(UUID jobId, UUID attemptId, Exception exception) {
        ReportJobEntity job = findJobForUpdate(jobId);
        if (!isCurrentAttempt(job, attemptId)) {
            return;
        }

        Instant now = Instant.now();
        job.setResultJson(null);
        if (job.getAttemptCount() >= MAX_ATTEMPTS) {
            job.setStatus(ReportStatus.FAILED);
            job.setErrorMessage("Исчерпан лимит попыток формирования отчёта");
            job.setFinishedAt(now);
            job.setNextAttemptAt(null);
            log.error("Report {} exhausted {} attempts", jobId, MAX_ATTEMPTS, exception);
        } else {
            job.setStatus(ReportStatus.RETRY_PENDING);
            job.setErrorMessage("Временная ошибка БД. Запланирована повторная попытка");
            job.setNextAttemptAt(now.plusSeconds(RETRY_DELAY_SECONDS));
            job.setFinishedAt(null);
            log.warn("Report {} attempt {} failed; retry scheduled", jobId,
                    job.getAttemptCount(), exception);
        }
    }

    private ReportJobEntity findJobForUpdate(UUID jobId) {
        return reportJobRepositorty.findByIdForUpdate(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Report not found"));
    }

    private boolean isCurrentAttempt(ReportJobEntity job, UUID attemptId) {
        boolean current = attemptId != null
                && job.getStatus() == ReportStatus.PROCESSING
                && Objects.equals(job.getAttemptId(), attemptId);
        if (!current) {
            log.warn("Ignoring stale or already finished attempt {} for report {}",
                    attemptId, job.getUuid());
        }
        return current;
    }

    private TaskStatisticsDTO calculateStatistics(
            List<TaskEntity> tasks,
            LocalDateTime asOf
    ) {
        long done = 0;
        long inProgress = 0;
        long open = 0;
        long overdue = 0;

        for (TaskEntity task : tasks) {
            TaskStatus status = task.getStatus();

            if (status == TaskStatus.DONE) {
                done++;
            } else if (status == TaskStatus.IN_PROGRESS) {
                inProgress++;
            } else if (status == TaskStatus.OPEN) {
                open++;
            }

            boolean isOverdue =
                    status != TaskStatus.DONE
                            && status != TaskStatus.CANCELLED
                            && task.getDeadline() != null
                            && task.getDeadline().isBefore(asOf);

            if (isOverdue) {
                overdue++;
            }
        }

        return new TaskStatisticsDTO(
                tasks.size(),
                done,
                inProgress,
                open,
                overdue
        );
    }
}
