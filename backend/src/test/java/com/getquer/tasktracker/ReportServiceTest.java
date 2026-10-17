package com.getquer.tasktracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getquer.tasktracker.Entities.*;
import com.getquer.tasktracker.Enums.ReportStatus;
import com.getquer.tasktracker.Enums.TaskStatus;
import com.getquer.tasktracker.Repositories.*;
import com.getquer.tasktracker.events.ReportRequestEvent;
import com.getquer.tasktracker.requestDTO.ReportRequest;
import com.getquer.tasktracker.responseDTO.DepartmentReportDTO;
import com.getquer.tasktracker.responseDTO.TaskStatisticsDTO;
import com.getquer.tasktracker.service.ReportService;
import com.getquer.tasktracker.util.TaskTestDataMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Mock TaskRepository taskRepository;
    @Mock UserRepository userRepository;
    @Mock ApplicationEventPublisher publisher;
    @Mock ReportJobRepositorty reportRepository;
    @Mock DepartmentRepository departmentRepository;

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private ReportService service;
    private DepartmentEntity department;
    private UserEntity manager;
    private ReportJobEntity job;

    @BeforeEach
    void setUp() {
        service = new ReportService(taskRepository, userRepository, publisher,
                reportRepository, mapper, departmentRepository);
        department = TaskTestDataMother.createTestDepartment(3L, "IT");
        manager = TaskTestDataMother.createTestUserWithDepartment(1L, "manager", "MANAGER", department);
        job = new ReportJobEntity();
        job.setUuid(java.util.UUID.randomUUID());
        job.setRequestedBy(manager.getId());
        job.setDepartmentId(department.getId());
        job.setPeriodFrom(LocalDate.of(2026, 1, 1));
        job.setPeriodToExclusive(LocalDate.of(2026, 2, 1));
        job.setStatus(ReportStatus.PENDING);
    }

    @Test
    void requestPersistsPendingJobAndPublishesMatchingId() {
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        var result = service.requestReport(new ReportRequest(job.getPeriodFrom(), job.getPeriodToExclusive()), "manager");
        ArgumentCaptor<ReportJobEntity> saved = ArgumentCaptor.forClass(ReportJobEntity.class);
        ArgumentCaptor<ReportRequestEvent> event = ArgumentCaptor.forClass(ReportRequestEvent.class);
        var order = inOrder(reportRepository, publisher);
        order.verify(reportRepository).save(saved.capture());
        order.verify(publisher).publishEvent(event.capture());
        assertEquals(ReportStatus.PENDING, result.status());
        assertEquals(result.jobId(), saved.getValue().getUuid());
        assertEquals(result.jobId(), event.getValue().payload());
        assertEquals(manager.getId(), saved.getValue().getRequestedBy());
    }

    @Test
    void invalidPeriodDoesNotSaveOrPublish() {
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        assertThrows(ResponseStatusException.class, () -> service.requestReport(
                new ReportRequest(job.getPeriodToExclusive(), job.getPeriodFrom()), "manager"));
        verifyNoInteractions(reportRepository, publisher);
    }

    @Test
    void reportSeparatesEmployeesExcludesManagerAndHandlesOverdue() {
        var employee = TaskTestDataMother.createTestUserWithDepartment(10L, "alice", "USER", department);
        var second = TaskTestDataMother.createTestUserWithDepartment(11L, "bob", "USER", department);
        var empty = TaskTestDataMother.createTestUserWithDepartment(12L, "empty", "USER", department);
        var done = task(1L, TaskStatus.DONE, employee, LocalDateTime.of(2000, 1, 1, 0, 0));
        var late = task(2L, TaskStatus.IN_PROGRESS, employee, LocalDateTime.of(2000, 1, 1, 0, 0));
        var open = task(3L, TaskStatus.OPEN, second, LocalDateTime.of(2100, 1, 1, 0, 0));
        var cancelled = task(4L, TaskStatus.CANCELLED, second, LocalDateTime.of(2000, 1, 1, 0, 0));
        var managerTask = task(5L, TaskStatus.DONE, manager, null);
        when(reportRepository.findById(job.getUuid())).thenReturn(Optional.of(job));
        when(departmentRepository.findById(3L)).thenReturn(Optional.of(department));
        when(userRepository.findAllByDepartment_IdAndRoleOrderByUsernameAsc(3L, "USER"))
                .thenReturn(List.of(employee, second, empty));
        when(taskRepository.findByDepartmentIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                3L, job.getPeriodFrom().atStartOfDay(), job.getPeriodToExclusive().atStartOfDay()))
                .thenReturn(List.of(done, late, open, cancelled, managerTask));

        DepartmentReportDTO result = service.buildReport(job.getUuid());

        assertEquals(new TaskStatisticsDTO(4, 1, 1, 1, 1), result.totals());
        assertEquals(new TaskStatisticsDTO(2, 1, 1, 0, 1), result.employees().get(0).statistics());
        assertEquals(new TaskStatisticsDTO(2, 0, 0, 1, 0), result.employees().get(1).statistics());
        assertEquals(new TaskStatisticsDTO(0, 0, 0, 0, 0), result.employees().get(2).statistics());
    }

    @Test
    void completedReportRoundTripsThroughJson() throws Exception {
        when(reportRepository.findById(job.getUuid())).thenReturn(Optional.of(job));
        when(reportRepository.findByIdForUpdate(job.getUuid())).thenReturn(Optional.of(job));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        DepartmentReportDTO report = new DepartmentReportDTO(3L, "IT", job.getPeriodFrom(),
                job.getPeriodToExclusive(), Instant.now(), new TaskStatisticsDTO(0, 0, 0, 0, 0), List.of());
        var attemptId = service.tryStartAttempt(job.getUuid()).orElseThrow();
        assertEquals(ReportStatus.PROCESSING, job.getStatus());
        assertNotNull(job.getStartedAt());
        service.markCompleted(job.getUuid(), attemptId, report);
        assertEquals(ReportStatus.COMPLETED, job.getStatus());
        assertNotNull(job.getFinishedAt());
        assertEquals(report, service.getReport(job.getUuid(), "manager"));
    }

    @Test
    void foreignManagerCannotReadResultOrStatus() {
        var other = TaskTestDataMother.createTestUserWithDepartment(2L, "other", "MANAGER", department);
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));
        when(reportRepository.findById(job.getUuid())).thenReturn(Optional.of(job));
        assertThrows(AccessDeniedException.class, () -> service.getReport(job.getUuid(), "other"));
        assertThrows(AccessDeniedException.class, () -> service.getStatus(job.getUuid(), "other"));
    }

    @Test
    void unfinishedResultReturnsConflictAndFailureIsVisibleInStatus() {
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        when(reportRepository.findById(job.getUuid())).thenReturn(Optional.of(job));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.getReport(job.getUuid(), "manager"));
        assertEquals(409, error.getStatusCode().value());
        when(reportRepository.findByIdForUpdate(job.getUuid())).thenReturn(Optional.of(job));
        var attemptId = service.tryStartAttempt(job.getUuid()).orElseThrow();
        service.markFailed(job.getUuid(), attemptId, new IllegalStateException("test failure"));
        var status = service.getStatus(job.getUuid(), "manager");
        assertEquals(ReportStatus.FAILED, status.status());
        assertNotNull(status.errorMessage());
        assertNull(job.getResultJson());
    }

    @Test
    void claimStartsOnlyOneAttempt() {
        when(reportRepository.findByIdForUpdate(job.getUuid())).thenReturn(Optional.of(job));
        var attempt = service.tryStartAttempt(job.getUuid()).orElseThrow();
        assertEquals(attempt, job.getAttemptId());
        assertEquals(1, job.getAttemptCount());
        assertTrue(service.tryStartAttempt(job.getUuid()).isEmpty());
        assertEquals(1, job.getAttemptCount());
    }

    @Test
    void transientFailureWaitsThirtySecondsBeforeAnotherAttempt() {
        when(reportRepository.findByIdForUpdate(job.getUuid())).thenReturn(Optional.of(job));
        var attempt = service.tryStartAttempt(job.getUuid()).orElseThrow();
        Instant before = Instant.now();
        service.scheduleRetry(job.getUuid(), attempt, new IllegalStateException("temporary failure"));
        assertEquals(ReportStatus.RETRY_PENDING, job.getStatus());
        assertFalse(job.getNextAttemptAt().isBefore(before.plusSeconds(30)));
        assertNull(job.getFinishedAt());
        assertTrue(service.tryStartAttempt(job.getUuid()).isEmpty());
        job.setNextAttemptAt(Instant.now().minusSeconds(1));
        var secondAttempt = service.tryStartAttempt(job.getUuid()).orElseThrow();
        assertNotEquals(attempt, secondAttempt);
        assertEquals(2, job.getAttemptCount());
    }

    @Test
    void fifthFailedAttemptIsTerminal() {
        when(reportRepository.findByIdForUpdate(job.getUuid())).thenReturn(Optional.of(job));
        job.setAttemptCount(4);
        var attempt = service.tryStartAttempt(job.getUuid()).orElseThrow();
        service.scheduleRetry(job.getUuid(), attempt, new IllegalStateException("failure"));
        assertEquals(5, job.getAttemptCount());
        assertEquals(ReportStatus.FAILED, job.getStatus());
        assertNull(job.getNextAttemptAt());
        assertNotNull(job.getFinishedAt());
        assertTrue(service.tryStartAttempt(job.getUuid()).isEmpty());
    }

    @Test
    void staleAttemptCannotCompleteFailOrRescheduleCurrentAttempt() throws Exception {
        when(reportRepository.findByIdForUpdate(job.getUuid())).thenReturn(Optional.of(job));
        var current = service.tryStartAttempt(job.getUuid()).orElseThrow();
        var stale = java.util.UUID.randomUUID();
        service.markCompleted(job.getUuid(), stale, null);
        service.markFailed(job.getUuid(), stale, new IllegalStateException("old failure"));
        service.scheduleRetry(job.getUuid(), stale, new IllegalStateException("old failure"));
        assertEquals(ReportStatus.PROCESSING, job.getStatus());
        assertEquals(current, job.getAttemptId());
        assertNull(job.getFinishedAt());
        assertNull(job.getNextAttemptAt());
    }

    private TaskEntity task(Long id, TaskStatus status, UserEntity user, LocalDateTime deadline) {
        TaskEntity task = TaskTestDataMother.createTestTask(id, "task", status, user);
        task.setDeadline(deadline);
        return task;
    }
}
