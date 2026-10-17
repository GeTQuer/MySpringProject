package com.getquer.tasktracker;

import com.getquer.tasktracker.events.ReportRequestEvent;
import com.getquer.tasktracker.processors.ReportRequestEventProcessor;
import com.getquer.tasktracker.service.ReportService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.transaction.CannotCreateTransactionException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReportRequestEventProcessorTest {
    private final ReportService service = mock(ReportService.class);
    private final UUID jobId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();

    private void process() {
        new ReportRequestEventProcessor(service).process(new ReportRequestEvent(this, jobId));
    }

    @Test
    void generationFailureIsTerminal() {
        when(service.tryStartAttempt(jobId)).thenReturn(Optional.of(attemptId));
        var failure = new IllegalStateException("invalid data");
        when(service.buildReport(jobId)).thenThrow(failure);
        process();
        verify(service).markFailed(jobId, attemptId, failure);
        verify(service, never()).scheduleRetry(any(), any(), any());
    }

    @Test
    void transientDatabaseFailuresScheduleRetry() {
        for (RuntimeException failure : new RuntimeException[]{
                new CannotAcquireLockException("deadlock"),
                new DataAccessResourceFailureException("connection lost"),
                new CannotCreateTransactionException("database unavailable")
        }) {
            reset(service);
            when(service.tryStartAttempt(jobId)).thenReturn(Optional.of(attemptId));
            when(service.buildReport(jobId)).thenThrow(failure);
            process();
            verify(service).scheduleRetry(jobId, attemptId, failure);
            verify(service, never()).markFailed(any(), any(), any());
        }
    }

    @Test
    void completionDatabaseFailureAlsoSchedulesRetry() throws Exception {
        when(service.tryStartAttempt(jobId)).thenReturn(Optional.of(attemptId));
        var failure = new DataAccessResourceFailureException("connection lost on save");
        doThrow(failure).when(service).markCompleted(jobId, attemptId, null);
        process();
        verify(service).scheduleRetry(jobId, attemptId, failure);
    }

    @Test
    void statusPersistenceFailureDoesNotEscapeAsyncProcessor() {
        when(service.tryStartAttempt(jobId)).thenReturn(Optional.of(attemptId));
        var failure = new DataAccessResourceFailureException("database unavailable");
        when(service.buildReport(jobId)).thenThrow(failure);
        doThrow(failure).when(service).scheduleRetry(jobId, attemptId, failure);
        assertDoesNotThrow(this::process);
    }

    @Test
    void missingOrUnclaimedJobDoesNotGenerateOrChangeStatus() {
        when(service.tryStartAttempt(jobId)).thenReturn(Optional.empty());
        process();
        verify(service, never()).buildReport(any());
        verify(service, never()).markFailed(any(), any(), any());
        when(service.tryStartAttempt(jobId)).thenThrow(new EntityNotFoundException());
        assertDoesNotThrow(this::process);
        verify(service, never()).scheduleRetry(any(), any(), any());
    }

    @Test
    void failedClaimCannotModifyAnUnconfirmedAttempt() {
        when(service.tryStartAttempt(jobId))
                .thenThrow(new DataAccessResourceFailureException("claim failed"));
        assertDoesNotThrow(this::process);
        verify(service, never()).buildReport(any());
        verify(service, never()).scheduleRetry(any(), any(), any());
        verify(service, never()).markFailed(any(), any(), any());
    }
}
