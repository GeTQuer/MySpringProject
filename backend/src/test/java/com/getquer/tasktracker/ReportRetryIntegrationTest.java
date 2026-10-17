package com.getquer.tasktracker;

import com.getquer.tasktracker.Entities.ReportJobEntity;
import com.getquer.tasktracker.Enums.ReportStatus;
import com.getquer.tasktracker.Repositories.ReportJobRepositorty;
import com.getquer.tasktracker.events.ReportRequestEvent;
import com.getquer.tasktracker.responseDTO.DepartmentReportDTO;
import com.getquer.tasktracker.responseDTO.TaskStatisticsDTO;
import com.getquer.tasktracker.service.ReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@SpringBootTest(properties = {
        "reports.retry.initial-delay-ms=3600000",
        "reports.retry.fixed-delay-ms=3600000",
        "reports.recovery.initial-delay-ms=3600000",
        "reports.recovery.fixed-delay-ms=3600000"
})
class ReportRetryIntegrationTest {
    @Autowired ReportJobRepositorty repository;
    @Autowired ReportRetryScheduler scheduler;
    @Autowired ReportRecoveryScheduler recoveryScheduler;
    @Autowired ApplicationEventPublisher publisher;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoSpyBean ReportService service;

    private UUID jobId;

    @AfterEach
    void cleanUp() {
        if (jobId != null) {
            repository.deleteById(jobId);
        }
    }

    @Test
    void simultaneousClaimsStartOnlyOneAttempt() throws Exception {
        createJob();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return service.tryStartAttempt(jobId);
            });
            var second = executor.submit(() -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return service.tryStartAttempt(jobId);
            });
            start.countDown();
            Optional<UUID> a = first.get(5, TimeUnit.SECONDS);
            Optional<UUID> b = second.get(5, TimeUnit.SECONDS);
            assertNotEquals(a.isPresent(), b.isPresent());
            ReportJobEntity saved = repository.findById(jobId).orElseThrow();
            assertEquals(1, saved.getAttemptCount());
            assertEquals(ReportStatus.PROCESSING, saved.getStatus());
        }
    }

    @Test
    void schedulerPublishesRetryAfterCommitAndNewAttemptSavesResult() throws Exception {
        createJob();
        var report = new DepartmentReportDTO(3L, "IT", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 1), Instant.now(),
                new TaskStatisticsDTO(0, 0, 0, 0, 0), List.of());
        doThrow(new DataAccessResourceFailureException("simulated temporary failure"))
                .doReturn(report).when(service).buildReport(jobId);

        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                publisher.publishEvent(new ReportRequestEvent(this, jobId)));

        ReportJobEntity failedAttempt = awaitStatus(ReportStatus.RETRY_PENDING);
        assertEquals(1, failedAttempt.getAttemptCount());
        UUID oldAttempt = failedAttempt.getAttemptId();
        assertNotNull(failedAttempt.getNextAttemptAt());

        // Не ждём 30 секунд в тесте: делаем сохранённый повтор готовым к запуску.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var job = repository.findById(jobId).orElseThrow();
            job.setNextAttemptAt(Instant.now().minusSeconds(1));
            repository.save(job);
        });
        scheduler.retryReports();

        ReportJobEntity completed = awaitStatus(ReportStatus.COMPLETED);
        assertEquals(2, completed.getAttemptCount());
        assertNotEquals(oldAttempt, completed.getAttemptId());
        assertNotNull(completed.getResultJson());
        assertNull(completed.getNextAttemptAt());

        service.markFailed(jobId, oldAttempt, new IllegalStateException("stale worker"));
        assertEquals(ReportStatus.COMPLETED, repository.findById(jobId).orElseThrow().getStatus());
    }

    @Test
    void recoveryRestartsPendingJobWhoseEventWasLost() throws Exception {
        createJob(Instant.now().minusSeconds(660));
        stubReport();

        recoveryScheduler.recoverStalledReports();

        var result = awaitStatus(ReportStatus.COMPLETED);
        assertEquals(1, result.getAttemptCount());
        assertNotNull(result.getResultJson());
    }

    @Test
    void recoveryReplacesStalledAttemptAndRejectsItsLateResult() throws Exception {
        createJob();
        var oldAttempt = service.tryStartAttempt(jobId).orElseThrow();
        var job = repository.findById(jobId).orElseThrow();
        job.setStartedAt(Instant.now().minusSeconds(660));
        repository.saveAndFlush(job);
        stubReport();

        recoveryScheduler.recoverStalledReports();

        var result = awaitStatus(ReportStatus.COMPLETED);
        assertEquals(2, result.getAttemptCount());
        assertNotEquals(oldAttempt, result.getAttemptId());
        service.markFailed(jobId, oldAttempt, new IllegalStateException("late old attempt"));
        assertEquals(ReportStatus.COMPLETED, repository.findById(jobId).orElseThrow().getStatus());
    }

    @Test
    void recoveryDoesNotRestartFreshOrCompletedJobs() {
        createJob();
        recoveryScheduler.recoverStalledReports();
        assertEquals(ReportStatus.PENDING, repository.findById(jobId).orElseThrow().getStatus());

        var job = repository.findById(jobId).orElseThrow();
        job.setStatus(ReportStatus.COMPLETED);
        job.setCreatedAt(Instant.now().minusSeconds(660));
        job.setStartedAt(Instant.now().minusSeconds(660));
        job.setFinishedAt(Instant.now());
        repository.saveAndFlush(job);
        recoveryScheduler.recoverStalledReports();
        assertEquals(ReportStatus.COMPLETED, repository.findById(jobId).orElseThrow().getStatus());
        verify(service, never()).tryStartAttempt(jobId);
    }

    @Test
    void recoveryRespectsExhaustedAttemptLimit() throws Exception {
        createJob();
        var job = repository.findById(jobId).orElseThrow();
        job.setStatus(ReportStatus.PROCESSING);
        job.setAttemptCount(5);
        job.setAttemptId(UUID.randomUUID());
        job.setStartedAt(Instant.now().minusSeconds(660));
        repository.saveAndFlush(job);

        recoveryScheduler.recoverStalledReports();

        var result = awaitStatus(ReportStatus.FAILED);
        assertEquals(5, result.getAttemptCount());
        assertNull(result.getNextAttemptAt());
        verify(service, never()).buildReport(jobId);
    }

    private void stubReport() {
        doReturn(new DepartmentReportDTO(3L, "IT", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 1), Instant.now(),
                new TaskStatisticsDTO(0, 0, 0, 0, 0), List.of()))
                .when(service).buildReport(jobId);
    }

    private void createJob() {
        createJob(Instant.now());
    }

    private void createJob(Instant createdAt) {
        var job = new ReportJobEntity();
        jobId = UUID.randomUUID();
        job.setUuid(jobId);
        job.setRequestedBy(1L);
        job.setDepartmentId(3L);
        job.setPeriodFrom(LocalDate.of(2026, 1, 1));
        job.setPeriodToExclusive(LocalDate.of(2026, 2, 1));
        job.setCreatedAt(createdAt);
        job.setStatus(ReportStatus.PENDING);
        repository.saveAndFlush(job);
    }

    private ReportJobEntity awaitStatus(ReportStatus expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            var job = repository.findById(jobId).orElseThrow();
            if (job.getStatus() == expected) {
                return job;
            }
            Thread.sleep(25);
        }
        throw new AssertionError("Report did not reach " + expected);
    }
}
