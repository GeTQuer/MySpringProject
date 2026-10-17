package com.getquer.tasktracker.processors;

import com.getquer.tasktracker.events.ReportRequestEvent;
import com.getquer.tasktracker.responseDTO.DepartmentReportDTO;
import com.getquer.tasktracker.service.ReportService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.CannotCreateTransactionException;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportRequestEventProcessor {

    private final ReportService reportService;

    @Async("taskEventsExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void process(ReportRequestEvent event) {
        UUID jobId = event.payload();
        Optional<UUID> attempt;
        try {
            attempt = reportService.tryStartAttempt(jobId);
        } catch (EntityNotFoundException exception) {
            log.warn("Report {} no longer exists; skipping event", jobId);
            return;
        } catch (Exception exception) {
            // Захват не подтверждён: нельзя менять статус чужой/неизвестной попытки.
            log.error("Could not confirm claim for report {}", jobId, exception);
            return;
        }

        if (attempt.isEmpty()) {
            return;
        }

        UUID attemptId = attempt.get();
        try {
            log.info("Processing report {} on {}", jobId, Thread.currentThread().getName());
            DepartmentReportDTO report =
                    reportService.buildReport(jobId);

            reportService.markCompleted(jobId, attemptId, report);
        } catch (TransientDataAccessException | DataAccessResourceFailureException
                 | CannotCreateTransactionException exception) {
            saveFailure(jobId, attemptId, exception, true);
        } catch (Exception exception) {
            saveFailure(jobId, attemptId, exception, false);
        }
    }

    private void saveFailure(UUID jobId, UUID attemptId, Exception exception, boolean retryable) {
        try {
            if (retryable) {
                reportService.scheduleRetry(jobId, attemptId, exception);
            } else {
                reportService.markFailed(jobId, attemptId, exception);
            }
        } catch (Exception statusException) {
            log.error("Report {} attempt {} failed; status could not be saved",
                    jobId, attemptId, exception);
            log.error("Failure status persistence error for report {}", jobId, statusException);
        }
    }
}
